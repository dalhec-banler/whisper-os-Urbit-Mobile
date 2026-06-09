package io.nativeplanet.controller;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

/**
 * Parent-assisted moon provisioning entry point.
 *
 * This class intentionally does not implement the remote protocol yet. It
 * validates the launcher/controller contract for hosting URL + access code and
 * returns a safe, explicit unavailable response without logging secrets.
 */
public final class ParentPairingManager {

    private static final String TAG = "ParentPairingManager";

    private ParentPairingManager() {
    }

    public static String pairWithPlanet(String requestJson) {
        try {
            if (requestJson == null || requestJson.trim().isEmpty()) {
                return response(false, "MISSING_REQUEST", "Pairing request missing");
            }

            JSONObject request = new JSONObject(requestJson);
            String hostUrl = request.optString("hostUrl", "").trim();
            String accessCode = request.optString("accessCode", "").trim();

            if (!isValidHttpsUrl(hostUrl)) {
                return response(false, "INVALID_HOST_URL", "Enter a valid HTTPS hosting URL");
            }

            if (accessCode.isEmpty()) {
                return response(false, "MISSING_ACCESS_CODE", "Enter your +code");
            }

            return response(false, "PARENT_PAIRING_UNAVAILABLE",
                    "Parent pairing is not available in this build");
        } catch (JSONException e) {
            Log.w(TAG, "Pairing request JSON parse failed");
            return response(false, "REQUEST_PARSE_ERROR", "Pairing request is not valid JSON");
        } catch (Exception e) {
            Log.e(TAG, "Pairing failed unexpectedly", e);
            return response(false, "PAIRING_FAILED", "Pairing failed");
        }
    }

    private static boolean isValidHttpsUrl(String hostUrl) {
        if (hostUrl == null || hostUrl.length() > 512) {
            return false;
        }
        try {
            URI uri = URI.create(hostUrl);
            return "https".equalsIgnoreCase(uri.getScheme()) &&
                    uri.getHost() != null &&
                    !uri.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private static String response(boolean accepted, String code, String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("accepted", accepted);
            json.put("code", code);
            json.put("message", message != null ? message : JSONObject.NULL);
            return json.toString();
        } catch (JSONException e) {
            return "{\"accepted\":false,\"code\":\"INTERNAL_ERROR\"}";
        }
    }
}
