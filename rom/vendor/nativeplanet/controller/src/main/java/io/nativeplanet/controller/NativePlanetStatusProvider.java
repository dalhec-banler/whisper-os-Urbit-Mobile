package io.nativeplanet.controller;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * ContentProvider exposing NativePlanet runtime status to the launcher.
 *
 * URI scheme:
 *   content://io.nativeplanet.controller/status          - combined status
 *   content://io.nativeplanet.controller/network         - network state
 *   content://io.nativeplanet.controller/runtime         - runtime status
 *   content://io.nativeplanet.controller/bootpackage     - boot package status
 *   content://io.nativeplanet.controller/diagnostics     - diagnostics summary
 *
 * Security:
 *   - No key material exposed
 *   - keyMaterialRef redacted to keyFileExists boolean
 *   - No /data/nativeplanet/keys access
 *   - Logs sanitized (no secrets)
 *
 * Permission:
 *   DEBUG builds: protectionLevel="normal" for launcher testing velocity
 *   TODO production: change to protectionLevel="signature|privileged"
 */
public class NativePlanetStatusProvider extends ContentProvider {

    private static final String TAG = "NativePlanetStatusProvider";
    private static final String AUTHORITY = "io.nativeplanet.controller";

    private static final String NATIVEPLANET_DIR = "/data/nativeplanet";
    private static final String NETWORK_STATE_PATH = NATIVEPLANET_DIR + "/network-state.json";
    private static final String RESOLV_CONF_PATH = NATIVEPLANET_DIR + "/resolv.conf";
    private static final String RUNTIME_STATUS_PATH = NATIVEPLANET_DIR + "/runtime-status.json";
    private static final String BOOT_PACKAGE_PATH = NATIVEPLANET_DIR + "/boot-package.json";
    private static final String BOOT_PACKAGE_STATUS_PATH = NATIVEPLANET_DIR + "/boot-package-status.json";

    private static final int MATCH_STATUS = 1;
    private static final int MATCH_NETWORK = 2;
    private static final int MATCH_RUNTIME = 3;
    private static final int MATCH_BOOTPACKAGE = 4;
    private static final int MATCH_DIAGNOSTICS = 5;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, "status", MATCH_STATUS);
        sUriMatcher.addURI(AUTHORITY, "network", MATCH_NETWORK);
        sUriMatcher.addURI(AUTHORITY, "runtime", MATCH_RUNTIME);
        sUriMatcher.addURI(AUTHORITY, "bootpackage", MATCH_BOOTPACKAGE);
        sUriMatcher.addURI(AUTHORITY, "diagnostics", MATCH_DIAGNOSTICS);
    }

    @Override
    public boolean onCreate() {
        Log.i(TAG, "NativePlanetStatusProvider created");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        int match = sUriMatcher.match(uri);
        String json;

        switch (match) {
            case MATCH_STATUS:
                json = getCombinedStatus();
                break;
            case MATCH_NETWORK:
                json = getNetworkStatus();
                break;
            case MATCH_RUNTIME:
                json = getRuntimeStatus();
                break;
            case MATCH_BOOTPACKAGE:
                json = getBootPackageStatus();
                break;
            case MATCH_DIAGNOSTICS:
                json = getDiagnostics();
                break;
            default:
                Log.w(TAG, "Unknown URI: " + uri);
                return null;
        }

        MatrixCursor cursor = new MatrixCursor(new String[]{"json"});
        cursor.addRow(new Object[]{json});
        return cursor;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        Bundle result = new Bundle();

        switch (method) {
            case "getStatus":
                result.putString("json", getCombinedStatus());
                break;
            case "getNetwork":
                result.putString("json", getNetworkStatus());
                break;
            case "getRuntime":
                result.putString("json", getRuntimeStatus());
                break;
            case "getBootPackage":
                result.putString("json", getBootPackageStatus());
                break;
            case "getDiagnostics":
                result.putString("json", getDiagnostics());
                break;
            case "startRuntime":
                result.putString("json", RuntimeControl.startRuntime());
                break;
            case "stopRuntime":
                result.putString("json", RuntimeControl.stopRuntimeAsync());
                break;
            default:
                Log.w(TAG, "Unknown method: " + method);
        }

        return result;
    }

    private String getCombinedStatus() {
        try {
            JSONObject combined = new JSONObject();
            combined.put("version", "0.1");
            combined.put("timestampMs", System.currentTimeMillis());
            combined.put("network", new JSONObject(getNetworkStatus()));
            combined.put("runtime", new JSONObject(getRuntimeStatus()));
            combined.put("bootPackage", new JSONObject(getBootPackageStatus()));
            combined.put("controllerAvailable", true);
            return combined.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build combined status", e);
            return "{\"error\":\"internal\",\"controllerAvailable\":true}";
        }
    }

    private String getNetworkStatus() {
        String raw = readFile(NETWORK_STATE_PATH);
        if (raw == null) {
            return buildDisconnectedNetwork();
        }

        try {
            JSONObject json = new JSONObject(raw);
            json.put("resolverAvailable", new File(RESOLV_CONF_PATH).exists());
            json.put("resolverContents", readFile(RESOLV_CONF_PATH));
            return json.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse network-state.json", e);
            return buildDisconnectedNetwork();
        }
    }

    private String buildDisconnectedNetwork() {
        try {
            JSONObject json = new JSONObject();
            json.put("networkType", "NONE");
            json.put("interfaceName", JSONObject.NULL);
            json.put("validated", false);
            json.put("dnsServers", new org.json.JSONArray());
            json.put("timestampMs", System.currentTimeMillis());
            json.put("resolverAvailable", false);
            json.put("resolverContents", JSONObject.NULL);
            return json.toString();
        } catch (JSONException e) {
            return "{\"networkType\":\"NONE\",\"validated\":false}";
        }
    }

    private String getRuntimeStatus() {
        String raw = readFile(RUNTIME_STATUS_PATH);
        if (raw != null) {
            // Validate JSON and extract runtime object if present
            try {
                JSONObject parsed = new JSONObject(raw);
                // New format has nested runtime object
                if (parsed.has("runtime")) {
                    JSONObject runtime = parsed.getJSONObject("runtime");
                    // Add top-level fields for compatibility
                    runtime.put("connSockAvailable", parsed.optBoolean("connSockAvailable", false));
                    runtime.put("timestamp", parsed.optLong("timestamp", 0));
                    return runtime.toString();
                }
                // Old format or direct runtime object
                return raw;
            } catch (JSONException e) {
                Log.w(TAG, "Malformed runtime-status.json, falling back to process state");
                // Fall through to process state detection
            }
        }

        // Runtime status file doesn't exist or is malformed - derive from process state
        try {
            JSONObject json = new JSONObject();
            boolean running = isVereServiceRunning();
            json.put("state", running ? "running" : "stopped");
            json.put("shipName", JSONObject.NULL);
            json.put("version", JSONObject.NULL);
            json.put("bootMode", JSONObject.NULL);
            json.put("pid", JSONObject.NULL);
            json.put("uptimeMs", JSONObject.NULL);
            json.put("lastError", JSONObject.NULL);
            json.put("lastSuccessfulPoll", JSONObject.NULL);
            json.put("connSockAvailable", false);
            return json.toString();
        } catch (JSONException e) {
            return "{\"state\":\"unknown\"}";
        }
    }

    private String getBootPackageStatus() {
        // Try boot-package-status.json first
        String statusRaw = readFile(BOOT_PACKAGE_STATUS_PATH);
        if (statusRaw != null) {
            return sanitizeBootPackageStatus(statusRaw);
        }

        // Fall back to reading boot-package.json and building status
        String raw = readFile(BOOT_PACKAGE_PATH);
        if (raw == null) {
            return buildNoBootPackage();
        }

        try {
            JSONObject pkg = new JSONObject(raw);
            JSONObject status = new JSONObject();

            status.put("exists", true);
            status.put("valid", true);
            status.put("packageVersion", pkg.optInt("packageVersion", 1));
            status.put("bootMode", pkg.optString("bootMode", null));
            status.put("ship", pkg.optString("ship", null));
            status.put("parent", pkg.optString("parent", null));

            String pierPath = pkg.optString("pierPath", null);
            status.put("pierPath", pierPath);
            status.put("pierExists", pierPath != null && new File(pierPath).exists());

            String pillPath = pkg.optString("pillPath", null);
            status.put("pillPath", pillPath);
            status.put("pillExists", pillPath != null && new File(pillPath).exists());

            // SECURITY: Redact key material reference to boolean only
            // TODO: keyFileExists currently means "keyMaterialRef field is present",
            // not that the referenced file actually exists. Acceptable for debug,
            // but should verify file existence in production.
            String keyRef = pkg.optString("keyMaterialRef", null);
            status.put("keyFileExists", keyRef != null && !keyRef.isEmpty() && !keyRef.equals("none"));

            status.put("validationErrors", new org.json.JSONArray());

            return status.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse boot-package.json", e);
            return buildNoBootPackage();
        }
    }

    private String sanitizeBootPackageStatus(String raw) {
        try {
            JSONObject status = new JSONObject(raw);
            // SECURITY: Remove any key material references
            if (status.has("keyMaterialRef")) {
                String ref = status.optString("keyMaterialRef", "");
                status.put("keyFileExists", !ref.isEmpty() && !ref.equals("none"));
                status.remove("keyMaterialRef");
            }
            return status.toString();
        } catch (JSONException e) {
            return raw;
        }
    }

    private String buildNoBootPackage() {
        try {
            JSONObject status = new JSONObject();
            status.put("exists", false);
            status.put("valid", false);
            status.put("ship", JSONObject.NULL);
            status.put("parent", JSONObject.NULL);
            status.put("pierExists", false);
            status.put("pillExists", false);
            status.put("keyFileExists", false);
            status.put("validationErrors", new org.json.JSONArray());
            return status.toString();
        } catch (JSONException e) {
            return "{\"exists\":false,\"valid\":false}";
        }
    }

    private String getDiagnostics() {
        try {
            JSONObject diag = new JSONObject();

            // Resolver contents
            diag.put("resolverContents", readFile(RESOLV_CONF_PATH));

            // Network state raw
            diag.put("networkStateRaw", readFile(NETWORK_STATE_PATH));

            // Controller logs - sanitized excerpt
            diag.put("controllerLogs", new org.json.JSONArray());

            // Launcher logs - not available from controller
            diag.put("launcherLogs", new org.json.JSONArray());

            // Recent errors
            diag.put("recentErrors", new org.json.JSONArray());

            return diag.toString();
        } catch (JSONException e) {
            return "{\"error\":\"internal\"}";
        }
    }

    private String readFile(String path) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            Log.e(TAG, "Failed to read " + path, e);
            return null;
        }
    }

    private boolean isVereServiceRunning() {
        return "running".equals(SystemProperties.get("init.svc.nativeplanet_vere", ""));
    }

    // Required ContentProvider methods - not used for this read-only provider

    @Override
    public String getType(Uri uri) {
        return "application/json";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }
}
