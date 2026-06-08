package io.nativeplanet.controller;

import android.os.SystemProperties;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller-owned runtime lifecycle actions.
 *
 * Normal stop follows GroundSeg's model: request Urbit |exit through conn.sock,
 * wait for the ship to exit, then update desired init property state.
 */
public final class RuntimeControl {

    private static final String TAG = "RuntimeControl";

    private static final String NATIVEPLANET_DIR = "/data/nativeplanet";
    private static final String BOOT_PACKAGE_PATH = NATIVEPLANET_DIR + "/boot-package.json";
    private static final String VERE_ENABLED_PROP = "nativeplanet.vere.enabled";
    private static final String VERE_SERVICE_PROP = "init.svc.nativeplanet_vere";
    private static final String CONN_SOCK_SUFFIX = "/.urb/conn.sock";

    private static final long STOP_TIMEOUT_MS = 10 * 60 * 1000L;
    private static final long STOP_POLL_MS = 500L;

    private static final AtomicBoolean STOP_IN_PROGRESS = new AtomicBoolean(false);

    private RuntimeControl() {
    }

    public static String startRuntime() {
        try {
            if (!new File(BOOT_PACKAGE_PATH).exists()) {
                return result(false, "NO_BOOT_PACKAGE", "boot-package.json missing");
            }

            SystemProperties.set(VERE_ENABLED_PROP, "1");
            return result(true, "START_REQUESTED", null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to request runtime start", e);
            return result(false, "START_FAILED", e.getMessage());
        }
    }

    public static String stopRuntimeAsync() {
        if (!STOP_IN_PROGRESS.compareAndSet(false, true)) {
            return result(true, "STOP_ALREADY_IN_PROGRESS", null);
        }

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stopRuntimeWorker();
                } finally {
                    STOP_IN_PROGRESS.set(false);
                }
            }
        }, "NativePlanetGracefulStop");
        worker.start();

        return result(true, "STOP_REQUESTED", null);
    }

    private static void stopRuntimeWorker() {
        Log.i(TAG, "Graceful runtime stop requested");

        if (!isVereServiceRunning()) {
            Log.i(TAG, "Vere is already stopped");
            SystemProperties.set(VERE_ENABLED_PROP, "0");
            return;
        }

        String pierPath = readPierPath();
        if (pierPath == null) {
            Log.w(TAG, "No pier path available; refusing normal stop without conn.sock");
            return;
        }

        String sockPath = pierPath + CONN_SOCK_SUFFIX;
        boolean exitAccepted = false;

        try (ConnSockClient client = new ConnSockClient(sockPath)) {
            client.connect();
            exitAccepted = client.requestGracefulExit();
            Log.i(TAG, "Graceful exit request accepted=" + exitAccepted);
        } catch (Exception e) {
            Log.e(TAG, "Graceful exit request failed: " + e.getMessage(), e);
        }

        if (!exitAccepted) {
            Log.w(TAG, "Graceful exit was not accepted; leaving runtime running for safety");
            return;
        }

        long deadline = System.currentTimeMillis() + STOP_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!isVereServiceRunning()) {
                Log.i(TAG, "Vere exited after graceful stop request");
                SystemProperties.set(VERE_ENABLED_PROP, "0");
                return;
            }

            try {
                Thread.sleep(STOP_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "Graceful stop wait interrupted");
                return;
            }
        }

        Log.w(TAG, "Timed out waiting for graceful runtime stop; force-stop required");
    }

    private static boolean isVereServiceRunning() {
        return "running".equals(SystemProperties.get(VERE_SERVICE_PROP, ""));
    }

    private static String readPierPath() {
        File bootPackage = new File(BOOT_PACKAGE_PATH);
        if (!bootPackage.exists()) {
            return null;
        }

        try {
            String content = new String(Files.readAllBytes(bootPackage.toPath()));
            JSONObject json = new JSONObject(content);
            String pierPath = json.optString("pierPath", null);
            return (pierPath == null || pierPath.isEmpty()) ? null : pierPath;
        } catch (Exception e) {
            Log.e(TAG, "Failed to read boot-package.json: " + e.getMessage());
            return null;
        }
    }

    private static String result(boolean accepted, String code, String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("accepted", accepted);
            json.put("code", code);
            json.put("message", message != null ? message : JSONObject.NULL);
            return json.toString();
        } catch (Exception e) {
            return "{\"accepted\":false,\"code\":\"INTERNAL_ERROR\"}";
        }
    }
}
