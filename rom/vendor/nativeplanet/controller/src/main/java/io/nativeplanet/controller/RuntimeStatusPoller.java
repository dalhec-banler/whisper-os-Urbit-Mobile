package io.nativeplanet.controller;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;

/**
 * Polls vere runtime status via conn.sock and writes runtime-status.json.
 * Runs on a background HandlerThread to avoid blocking the main thread.
 */
public class RuntimeStatusPoller {

    private static final String TAG = "RuntimeStatusPoller";

    private static final String NATIVEPLANET_DIR = "/data/nativeplanet";
    private static final String BOOT_PACKAGE_PATH = NATIVEPLANET_DIR + "/boot-package.json";
    private static final String RUNTIME_STATUS_PATH = NATIVEPLANET_DIR + "/runtime-status.json";
    private static final String CONN_SOCK_SUFFIX = "/.urb/conn.sock";

    private static final long POLL_INTERVAL_MS = 5000; // 5 seconds
    private static final long SLOW_POLL_INTERVAL_MS = 30000; // 30 seconds when unconfigured
    private static final int FILE_MODE_0640 = 0640;

    private HandlerThread handlerThread;
    private Handler handler;
    private boolean running = false;

    // Cached state
    private String lastState = "unknown";
    private String lastShipName = null;
    private String lastVersion = null;
    private long lastSuccessfulPollMs = 0;
    private String lastError = null;
    private boolean lastBootPackagePresent = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            try {
                pollAndWriteStatus();
            } catch (Exception e) {
                Log.e(TAG, "Poll failed: " + e.getMessage());
            }

            // Schedule next poll
            long interval = ("stopped".equals(lastState) && !lastBootPackagePresent)
                    ? SLOW_POLL_INTERVAL_MS : POLL_INTERVAL_MS;
            if (running) {
                handler.postDelayed(this, interval);
            }
        }
    };

    public void start() {
        if (running) return;

        Log.i(TAG, "Starting RuntimeStatusPoller");
        running = true;

        handlerThread = new HandlerThread("RuntimeStatusPoller");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // Initial poll immediately
        handler.post(pollRunnable);
    }

    public void stop() {
        if (!running) return;

        Log.i(TAG, "Stopping RuntimeStatusPoller");
        running = false;

        if (handler != null) {
            handler.removeCallbacks(pollRunnable);
        }
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }
        handler = null;
    }

    private void pollAndWriteStatus() {
        // Read boot-package.json to find pier path
        String pierPath = readPierPath();
        if (pierPath == null) {
            lastBootPackagePresent = false;
            writeStatus("stopped", null, null, null, "no boot-package");
            return;
        }
        lastBootPackagePresent = true;

        // Check if conn.sock exists
        String connSockPath = pierPath + CONN_SOCK_SUFFIX;
        File connSockFile = new File(connSockPath);
        if (!connSockFile.exists()) {
            // Socket doesn't exist - check if vere process is running
            if (isVereServiceRunning()) {
                writeStatus("starting", lastShipName, lastVersion, null, "conn.sock not ready");
            } else {
                writeStatus("stopped", lastShipName, lastVersion, null, null);
            }
            return;
        }

        // Poll conn.sock
        ConnSockClient.StatusResult result = ConnSockClient.pollStatus(connSockPath);

        if (!result.connected) {
            // Connection failed
            lastError = result.error;
            if (isVereServiceRunning()) {
                writeStatus("starting", lastShipName, lastVersion, null, result.error);
            } else {
                writeStatus("stopped", lastShipName, lastVersion, null, result.error);
            }
            return;
        }

        if (result.error != null) {
            // Protocol error
            lastError = result.error;
            writeStatus("error", lastShipName, lastVersion, null, result.error);
            return;
        }

        // Success!
        lastSuccessfulPollMs = System.currentTimeMillis();
        lastError = null;

        if (result.alive) {
            // Update cached values
            if (result.shipId != null) {
                lastShipName = formatShipName(result.shipId);
            }
            if (result.version != null) {
                lastVersion = result.version;
            }
            writeStatus("running", lastShipName, lastVersion, null, null);
        } else {
            writeStatus("starting", lastShipName, lastVersion, null, "live=no");
        }
    }

    private String readPierPath() {
        File bootPackage = new File(BOOT_PACKAGE_PATH);
        if (!bootPackage.exists()) {
            return null;
        }

        try {
            String content = new String(Files.readAllBytes(bootPackage.toPath()));
            JSONObject json = new JSONObject(content);
            return json.optString("pierPath", null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to read boot-package.json: " + e.getMessage());
            return null;
        }
    }

    private void writeStatus(String state, String shipName, String version,
                             Integer pid, String error) {
        lastState = state;

        try {
            JSONObject json = new JSONObject();
            json.put("timestamp", System.currentTimeMillis());

            JSONObject runtime = new JSONObject();
            runtime.put("state", state);
            runtime.put("shipName", shipName != null ? shipName : JSONObject.NULL);
            runtime.put("version", version != null ? version : JSONObject.NULL);
            runtime.put("pid", pid != null ? pid : JSONObject.NULL);
            runtime.put("lastError", error != null ? error : JSONObject.NULL);
            runtime.put("lastSuccessfulPoll", lastSuccessfulPollMs > 0 ? lastSuccessfulPollMs : JSONObject.NULL);

            json.put("runtime", runtime);
            json.put("connSockAvailable", !"stopped".equals(state) && error == null);

            writeFile(RUNTIME_STATUS_PATH, json.toString(2));

            if (!"running".equals(state) || error != null) {
                Log.d(TAG, "Status: " + state + (error != null ? " (" + error + ")" : ""));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build status JSON: " + e.getMessage());
        }
    }

    private void writeFile(String path, String content) {
        // Write atomically: write to temp file, chmod, then rename
        String tempPath = path + ".tmp";
        File tempFile = new File(tempPath);
        File targetFile = new File(path);

        try {
            // Write using try-with-resources
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(content);
            }

            // Set permissions for group-readable before rename
            Os.chmod(tempPath, FILE_MODE_0640);

            // Atomic rename
            if (!tempFile.renameTo(targetFile)) {
                Log.e(TAG, "Failed to rename " + tempPath + " to " + path);
                tempFile.delete();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write " + path + ": " + e.getMessage());
            tempFile.delete();
        } catch (ErrnoException e) {
            Log.w(TAG, "Failed to chmod " + path + ": " + e.getMessage());
            tempFile.delete();
        }
    }

    private boolean isVereServiceRunning() {
        return "running".equals(SystemProperties.get("init.svc.nativeplanet_vere", ""));
    }

    /**
     * Format @p as ship name with ~ prefix.
     * For now, just returns hex representation since full @p encoding is complex.
     * TODO: Implement proper @p formatting.
     */
    private String formatShipName(BigInteger shipId) {
        // Full @p encoding is complex. For bootstrap, just return a recognizable format.
        // The boot-package.json ship name is authoritative anyway.
        // This provides verification that we're talking to the expected ship.

        // Read ship name from boot-package as authoritative source
        String bootShipName = readBootPackageShipName();
        if (bootShipName != null) {
            return bootShipName;
        }

        // Fallback to hex
        return "~0x" + shipId.toString(16);
    }

    private String readBootPackageShipName() {
        File bootPackage = new File(BOOT_PACKAGE_PATH);
        if (!bootPackage.exists()) {
            return null;
        }

        try {
            String content = new String(Files.readAllBytes(bootPackage.toPath()));
            JSONObject json = new JSONObject(content);
            String ship = json.optString("ship", null);
            if (ship != null && !ship.isEmpty()) {
                return ship.startsWith("~") ? ship : "~" + ship;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
