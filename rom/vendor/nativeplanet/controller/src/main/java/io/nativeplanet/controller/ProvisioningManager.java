package io.nativeplanet.controller;

import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Controller-owned moon provisioning.
 *
 * Security rules:
 * - raw key material is accepted only in-memory from the caller
 * - raw key material is never logged or returned
 * - provider responses expose only keyFileExists
 * - files are written atomically where practical
 */
public final class ProvisioningManager {

    private static final String TAG = "ProvisioningManager";

    private static final String NATIVEPLANET_DIR = "/data/nativeplanet";
    private static final String KEYS_DIR = NATIVEPLANET_DIR + "/keys";
    private static final String SHIPS_DIR = NATIVEPLANET_DIR + "/ships";
    private static final String BOOT_PACKAGE_PATH = NATIVEPLANET_DIR + "/boot-package.json";
    private static final String BOOT_PACKAGE_STATUS_PATH = NATIVEPLANET_DIR + "/boot-package-status.json";
    private static final String DEFAULT_PILL_PATH = "/system_ext/etc/nativeplanet/satellite.pill";
    private static final String VERE_SERVICE_PROP = "init.svc.nativeplanet_vere";

    private static final int FILE_MODE_0640 = 0640;
    private static final int KEY_FILE_MODE_0640 = 0640;
    //  vere runs as group "shell"; it must traverse keys/ to read its key
    //  and write into ships/ to create its pier, so these dirs need group
    //  rwx. mkdirs() alone yields 0700 under the controller's 0077 umask,
    //  which locks the runtime out on a fresh /data/nativeplanet.
    private static final int DIR_MODE_0770 = 0770;
    private ProvisioningManager() {
    }

    public static String provisionMoon(String requestJson) {
        try {
            if (requestJson == null || requestJson.trim().isEmpty()) {
                return response(false, "MISSING_REQUEST", "Provisioning request missing", null);
            }

            JSONObject request = new JSONObject(requestJson);
            ProvisioningRequest parsed = parseRequest(request);
            Validation validation = validate(parsed);
            if (!validation.valid) {
                return response(false, validation.code, validation.message, buildInvalidStatus(parsed, validation));
            }

            boolean replaceExisting = request.optBoolean("replaceExisting", false);
            if (isVereServiceRunning()) {
                return response(false, "RUNTIME_RUNNING", "Stop the current ship before provisioning", getBootPackageStatusObject());
            }

            File existingPackage = new File(BOOT_PACKAGE_PATH);
            if (existingPackage.exists() && !replaceExisting) {
                return response(false, "BOOT_PACKAGE_EXISTS", "A ship is already configured", getBootPackageStatusObject());
            }

            ensureDirectory(new File(NATIVEPLANET_DIR));
            ensureDirectory(new File(KEYS_DIR));
            ensureDirectory(new File(SHIPS_DIR));

            writeKeyFile(parsed);
            JSONObject bootPackage = buildBootPackage(parsed);
            writeJsonFileAtomic(BOOT_PACKAGE_PATH, bootPackage);

            JSONObject status = buildBootPackageStatus(bootPackage);
            writeJsonFileAtomic(BOOT_PACKAGE_STATUS_PATH, status);

            String startJson = RuntimeControl.startRuntime();
            JSONObject result = new JSONObject(startJson);
            if (!result.optBoolean("accepted", false)) {
                return response(false, result.optString("code", "START_FAILED"),
                        result.optString("message", "Runtime start failed"), status);
            }

            return response(true, "PROVISIONED_START_REQUESTED", null, status);
        } catch (JSONException e) {
            Log.w(TAG, "Provisioning request JSON parse failed: " + e.getMessage());
            return response(false, "REQUEST_PARSE_ERROR", "Provisioning request is not valid JSON", null);
        } catch (ProvisioningException e) {
            Log.w(TAG, "Provisioning failed: " + e.code);
            return response(false, e.code, e.safeMessage, null);
        } catch (Exception e) {
            Log.e(TAG, "Provisioning failed unexpectedly", e);
            return response(false, "PROVISIONING_FAILED", "Provisioning failed", null);
        }
    }

    public static String getBootPackageStatus() {
        return getBootPackageStatusObject().toString();
    }

    private static JSONObject getBootPackageStatusObject() {
        File bootPackage = new File(BOOT_PACKAGE_PATH);
        if (!bootPackage.exists()) {
            return noBootPackageStatus();
        }

        try {
            String content = java.nio.file.Files.readString(bootPackage.toPath());
            JSONObject pkg = new JSONObject(content);
            return buildBootPackageStatus(pkg);
        } catch (Exception e) {
            Validation validation = new Validation(false, "BOOT_PACKAGE_PARSE_ERROR", "Could not read ship configuration");
            return buildInvalidStatus(null, validation);
        }
    }

    private static ProvisioningRequest parseRequest(JSONObject request) {
        ProvisioningRequest parsed = new ProvisioningRequest();
        parsed.ship = normalizeShip(request.optString("ship", ""));
        parsed.parent = normalizeShip(request.optString("parent", ""));
        parsed.keyMaterial = request.optString("keyMaterial", "");
        parsed.pillPath = request.optString("pillPath", DEFAULT_PILL_PATH);
        parsed.bootMode = request.optString("bootMode", "MOON").toUpperCase(Locale.US);
        return parsed;
    }

    private static Validation validate(ProvisioningRequest request) {
        if (!"MOON".equals(request.bootMode)) {
            return new Validation(false, "UNSUPPORTED_BOOT_MODE", "Only moon provisioning is supported right now");
        }
        if (!isValidShipName(request.ship)) {
            return new Validation(false, "INVALID_SHIP", "Moon name is invalid");
        }
        if (!isValidShipName(request.parent)) {
            return new Validation(false, "INVALID_PARENT", "Parent ship is invalid");
        }
        if (!isValidModernMoonKey(request.keyMaterial)) {
            return new Validation(false, "KEY_FORMAT_UNSUPPORTED", "Key must be exported by current Urbit/Vere");
        }
        if (!isSafePath(request.pillPath) || !request.pillPath.startsWith("/system_ext/etc/nativeplanet/")) {
            return new Validation(false, "INVALID_PILL_PATH", "Pill path is not allowed");
        }
        if (!new File(request.pillPath).isFile()) {
            return new Validation(false, "MISSING_PILL", "Satellite pill is missing");
        }
        return Validation.OK;
    }

    private static boolean isValidShipName(String ship) {
        if (ship == null || ship.isEmpty() || ship.length() > 64) {
            return false;
        }
        String bare = ship.startsWith("~") ? ship.substring(1) : ship;
        if (bare.isEmpty() || bare.length() > 63) {
            return false;
        }
        for (int i = 0; i < bare.length(); i++) {
            char c = bare.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidModernMoonKey(String key) {
        if (key == null) {
            return false;
        }
        String trimmed = key.trim();
        if (!trimmed.equals(key) || trimmed.length() < 80 || trimmed.length() > 512) {
            return false;
        }
        if (!trimmed.startsWith("0w")) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') || c == '.' || c == '~' || c == '-';
            if (!ok) {
                return false;
            }
        }

        // Modern Artemis emits the moon seed as @uw. The concrete suffix varies
        // by key, but current mobile moon seeds consistently terminate with the
        // jammed atom tail "3i5".
        return trimmed.endsWith("3i5");
    }

    private static String normalizeShip(String ship) {
        if (ship == null) {
            return "";
        }
        String trimmed = ship.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.startsWith("~") ? trimmed : "~" + trimmed;
    }

    private static String bareShip(String ship) {
        return ship.startsWith("~") ? ship.substring(1) : ship;
    }

    private static boolean isSafePath(String path) {
        if (path == null || path.isEmpty() || path.contains("..")) {
            return false;
        }
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                return false;
            }
        }
        return true;
    }

    private static void ensureDirectory(File dir) throws ProvisioningException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new ProvisioningException("DIRECTORY_CREATE_FAILED", "Could not prepare ship storage");
        }
        if (!dir.isDirectory()) {
            throw new ProvisioningException("DIRECTORY_INVALID", "Ship storage path is not a directory");
        }
        //  Force group rwx so the shell-group runtime can reach the key and
        //  create its pier; mkdirs() would otherwise leave 0700 under umask.
        try {
            Os.chmod(dir.getAbsolutePath(), DIR_MODE_0770);
        } catch (ErrnoException e) {
            throw new ProvisioningException("DIRECTORY_PERMISSION_FAILED",
                    "Could not set ship storage permissions");
        }
    }

    private static void writeKeyFile(ProvisioningRequest request) throws ProvisioningException {
        String path = keyPathFor(request.ship);
        File target = new File(path);
        File temp = new File(path + ".tmp");

        try (FileWriter writer = new FileWriter(temp)) {
            writer.write(request.keyMaterial);
            writer.write("\n");
        } catch (IOException e) {
            deleteQuietly(temp);
            throw new ProvisioningException("KEY_WRITE_FAILED", "Could not write key file");
        }

        try {
            Os.chmod(temp.getAbsolutePath(), KEY_FILE_MODE_0640);
        } catch (ErrnoException e) {
            deleteQuietly(temp);
            throw new ProvisioningException("KEY_PERMISSION_FAILED", "Could not set key file permissions");
        }

        if (!temp.renameTo(target)) {
            deleteQuietly(temp);
            throw new ProvisioningException("KEY_RENAME_FAILED", "Could not finalize key file");
        }
    }

    private static JSONObject buildBootPackage(ProvisioningRequest request) throws JSONException {
        String bareShip = bareShip(request.ship);
        JSONObject json = new JSONObject();
        json.put("ship", bareShip);
        json.put("parent", request.parent);
        json.put("pillPath", request.pillPath);
        json.put("pierPath", SHIPS_DIR + "/" + bareShip);
        json.put("bootMode", "MOON");
        json.put("keyMaterialRef", "file:" + keyPathFor(request.ship));
        json.put("networkConfig", new JSONObject());
        json.put("delegationConfig", new JSONObject());
        json.put("createdAtMs", System.currentTimeMillis());
        json.put("packageVersion", 1);
        return json;
    }

    private static JSONObject buildBootPackageStatus(JSONObject bootPackage) throws JSONException {
        JSONObject status = new JSONObject();
        JSONArray errors = new JSONArray();

        String ship = bootPackage.optString("ship", "");
        String parent = bootPackage.optString("parent", "");
        String bootMode = bootPackage.optString("bootMode", "");
        String pierPath = bootPackage.optString("pierPath", "");
        String pillPath = bootPackage.optString("pillPath", "");
        String keyRef = bootPackage.optString("keyMaterialRef", "");
        String keyPath = keyRef.startsWith("file:") ? keyRef.substring(5) : "";

        addErrorIf(errors, bootPackage.optInt("packageVersion", -1) != 1,
                "packageVersion", "UNSUPPORTED_PACKAGE_VERSION", "Package version must be 1");
        addErrorIf(errors, !"MOON".equals(bootMode),
                "bootMode", "UNSUPPORTED_BOOT_MODE", "Only moon boot packages are supported right now");
        addErrorIf(errors, !isValidShipName(normalizeShip(ship)),
                "ship", "INVALID_SHIP", "Moon name is invalid");
        addErrorIf(errors, !isValidShipName(parent),
                "parent", "INVALID_PARENT", "Parent ship is invalid");
        addErrorIf(errors, !pillPath.startsWith("/system_ext/etc/nativeplanet/") || !new File(pillPath).isFile(),
                "pillPath", "MISSING_PILL", "Satellite pill is missing");
        addErrorIf(errors, !pierPath.startsWith(SHIPS_DIR + "/") || !isSafePath(pierPath),
                "pierPath", "INVALID_PIER_PATH", "Pier path is not allowed");
        addErrorIf(errors, !keyPath.startsWith(KEYS_DIR + "/") || !new File(keyPath).isFile(),
                "keyMaterialRef", "MISSING_KEY_FILE", "Key file is missing");

        status.put("exists", true);
        status.put("valid", errors.length() == 0);
        status.put("packageVersion", bootPackage.optInt("packageVersion", 1));
        status.put("bootMode", bootMode.isEmpty() ? JSONObject.NULL : bootMode);
        status.put("ship", ship.isEmpty() ? JSONObject.NULL : ship);
        status.put("parent", parent.isEmpty() ? JSONObject.NULL : parent);
        status.put("pierPath", pierPath.isEmpty() ? JSONObject.NULL : pierPath);
        status.put("pierExists", !pierPath.isEmpty() && new File(pierPath).exists());
        status.put("pillPath", pillPath.isEmpty() ? JSONObject.NULL : pillPath);
        status.put("pillExists", !pillPath.isEmpty() && new File(pillPath).isFile());
        status.put("keyFileExists", !keyPath.isEmpty() && new File(keyPath).isFile());
        status.put("validationErrors", errors);
        return status;
    }

    private static JSONObject noBootPackageStatus() {
        try {
            JSONObject status = new JSONObject();
            status.put("exists", false);
            status.put("valid", false);
            status.put("packageVersion", JSONObject.NULL);
            status.put("bootMode", JSONObject.NULL);
            status.put("ship", JSONObject.NULL);
            status.put("parent", JSONObject.NULL);
            status.put("pierPath", JSONObject.NULL);
            status.put("pierExists", false);
            status.put("pillPath", JSONObject.NULL);
            status.put("pillExists", false);
            status.put("keyFileExists", false);
            status.put("validationErrors", new JSONArray());
            return status;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static JSONObject buildInvalidStatus(ProvisioningRequest request, Validation validation) {
        try {
            JSONObject status = noBootPackageStatus();
            status.put("exists", request != null);
            status.put("valid", false);
            if (request != null) {
                status.put("packageVersion", 1);
                status.put("bootMode", request.bootMode);
                status.put("ship", request.ship.isEmpty() ? JSONObject.NULL : bareShip(request.ship));
                status.put("parent", request.parent.isEmpty() ? JSONObject.NULL : request.parent);
                status.put("pillPath", request.pillPath.isEmpty() ? JSONObject.NULL : request.pillPath);
                status.put("pillExists", !request.pillPath.isEmpty() && new File(request.pillPath).isFile());
                status.put("keyFileExists", false);
            }
            JSONArray errors = new JSONArray();
            JSONObject error = new JSONObject();
            error.put("field", validation.fieldForCode());
            error.put("code", validation.code);
            error.put("message", validation.message);
            errors.put(error);
            status.put("validationErrors", errors);
            return status;
        } catch (JSONException e) {
            return noBootPackageStatus();
        }
    }

    private static void addErrorIf(JSONArray errors, boolean condition,
                                   String field, String code, String message) throws JSONException {
        if (!condition) {
            return;
        }
        JSONObject error = new JSONObject();
        error.put("field", field);
        error.put("code", code);
        error.put("message", message);
        errors.put(error);
    }

    private static void writeJsonFileAtomic(String path, JSONObject json) throws ProvisioningException {
        File target = new File(path);
        File temp = new File(path + ".tmp");

        try (FileWriter writer = new FileWriter(temp)) {
            writer.write(json.toString(2));
            writer.write("\n");
        } catch (IOException | JSONException e) {
            deleteQuietly(temp);
            throw new ProvisioningException("FILE_WRITE_FAILED", "Could not write ship configuration");
        }

        try {
            Os.chmod(temp.getAbsolutePath(), FILE_MODE_0640);
        } catch (ErrnoException e) {
            deleteQuietly(temp);
            throw new ProvisioningException("FILE_PERMISSION_FAILED", "Could not set ship configuration permissions");
        }

        if (!temp.renameTo(target)) {
            deleteQuietly(temp);
            throw new ProvisioningException("FILE_RENAME_FAILED", "Could not finalize ship configuration");
        }
    }

    private static String keyPathFor(String ship) {
        return KEYS_DIR + "/" + bareShip(ship) + ".key";
    }

    private static boolean isVereServiceRunning() {
        return "running".equals(SystemProperties.get(VERE_SERVICE_PROP, ""));
    }

    private static void deleteQuietly(File file) {
        try {
            file.delete();
        } catch (Exception ignored) {
        }
    }

    private static String response(boolean accepted, String code, String message, JSONObject bootPackageStatus) {
        try {
            JSONObject json = new JSONObject();
            json.put("accepted", accepted);
            json.put("code", code);
            json.put("message", message != null ? message : JSONObject.NULL);
            if (bootPackageStatus != null) {
                json.put("bootPackage", bootPackageStatus);
            }
            return json.toString();
        } catch (JSONException e) {
            return "{\"accepted\":false,\"code\":\"INTERNAL_ERROR\"}";
        }
    }

    private static final class ProvisioningRequest {
        String ship;
        String parent;
        String keyMaterial;
        String pillPath;
        String bootMode;
    }

    private static final class Validation {
        static final Validation OK = new Validation(true, "OK", null);

        final boolean valid;
        final String code;
        final String message;

        Validation(boolean valid, String code, String message) {
            this.valid = valid;
            this.code = code;
            this.message = message;
        }

        String fieldForCode() {
            if (code.contains("KEY")) return "keyMaterial";
            if (code.contains("PARENT")) return "parent";
            if (code.contains("SHIP")) return "ship";
            if (code.contains("PILL")) return "pillPath";
            if (code.contains("BOOT_MODE")) return "bootMode";
            return "request";
        }
    }

    private static final class ProvisioningException extends Exception {
        final String code;
        final String safeMessage;

        ProvisioningException(String code, String safeMessage) {
            super(code);
            this.code = code;
            this.safeMessage = safeMessage;
        }
    }
}
