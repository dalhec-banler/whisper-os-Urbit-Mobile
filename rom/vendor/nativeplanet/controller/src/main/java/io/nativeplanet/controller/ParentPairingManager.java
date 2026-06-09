package io.nativeplanet.controller;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Parent-assisted moon provisioning entry point.
 *
 * This validates the launcher/controller contract and performs the first real
 * remote checkpoint: authenticate to the parent ship's Eyre endpoint with the
 * supplied +code and verify Artemis is installed. It does not yet create or
 * import a moon. That requires implementing the Artemis mobile moon request.
 */
public final class ParentPairingManager {

    private static final String TAG = "ParentPairingManager";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final String AUTH_CONFIRM_SCRY = "/~/scry/hood/kiln/pikes.json";
    private static final String ARTEMIS_APP_PATH = "/apps/artemis/";

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
            String normalizedUrl = normalizeHttpsUrl(hostUrl);

            if (!isValidHttpsUrl(normalizedUrl)) {
                return response(false, "INVALID_HOST_URL", "Enter a valid HTTPS hosting URL");
            }

            if (accessCode.isEmpty()) {
                return response(false, "MISSING_ACCESS_CODE", "Enter your +code");
            }

            PairingSession session = authenticate(normalizedUrl, accessCode);
            if (session.cookie == null || session.cookie.isEmpty() ||
                    !confirmAuthenticated(normalizedUrl, session.cookie)) {
                return response(false, "PARENT_AUTH_FAILED",
                        "Planet login failed. Check the hosting URL and +code.");
            }

            ParentServiceProbe probe = probeParentService(normalizedUrl, session.cookie);
            if (probe.available) {
                return response(false, "PARENT_PROTOCOL_UNSUPPORTED",
                        "Planet login worked and Artemis is installed, but this phone build cannot request a mobile moon yet.");
            }

            return response(false, "PARENT_SERVICE_UNAVAILABLE",
                    "Planet login worked. Artemis is not installed on the planet yet.");
        } catch (JSONException e) {
            Log.w(TAG, "Pairing request JSON parse failed");
            return response(false, "REQUEST_PARSE_ERROR", "Pairing request is not valid JSON");
        } catch (IOException e) {
            Log.w(TAG, "Parent pairing network request failed: " + safeMessage(e));
            return response(false, "PARENT_NETWORK_FAILED",
                    "Could not reach the planet hosting URL.");
        } catch (Exception e) {
            Log.e(TAG, "Pairing failed unexpectedly", e);
            return response(false, "PAIRING_FAILED", "Pairing failed");
        }
    }

    private static PairingSession authenticate(String hostUrl, String accessCode) throws IOException {
        URL url = URI.create(hostUrl + "/~/login").toURL();
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String form = "password=" + URLEncoder.encode(accessCode, StandardCharsets.UTF_8.name());
        byte[] body = form.getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);

        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body);
        }

        int status = connection.getResponseCode();
        String cookie = findUrbauthCookie(connection.getHeaderFields());
        connection.disconnect();

        return new PairingSession(status, cookie);
    }

    private static boolean confirmAuthenticated(String hostUrl, String cookie) throws IOException {
        URL url = URI.create(hostUrl + AUTH_CONFIRM_SCRY).toURL();
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", cookie);

        int status = connection.getResponseCode();
        connection.disconnect();

        return status == HttpURLConnection.HTTP_OK;
    }

    private static ParentServiceProbe probeParentService(String hostUrl, String cookie) throws IOException {
        URL url = URI.create(hostUrl + ARTEMIS_APP_PATH).toURL();
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", cookie);

        int status = connection.getResponseCode();
        connection.disconnect();

        return new ParentServiceProbe(status == HttpURLConnection.HTTP_OK, status);
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
        return connection;
    }

    private static String findUrbauthCookie(Map<String, List<String>> headers) {
        if (headers == null) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || !"set-cookie".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (value == null || !value.toLowerCase().contains("urbauth")) {
                    continue;
                }
                int semicolon = value.indexOf(';');
                return semicolon >= 0 ? value.substring(0, semicolon) : value;
            }
        }
        return null;
    }

    private static String normalizeHttpsUrl(String hostUrl) {
        if (hostUrl == null) {
            return "";
        }
        String trimmed = hostUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return stripTrailingSlash(trimmed);
        }
        return "https://" + stripTrailingSlash(trimmed);
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
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

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + message;
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

    private static final class PairingSession {
        @SuppressWarnings("unused")
        final int status;
        final String cookie;

        PairingSession(int status, String cookie) {
            this.status = status;
            this.cookie = cookie;
        }
    }

    private static final class ParentServiceProbe {
        final boolean available;
        @SuppressWarnings("unused")
        final int status;

        ParentServiceProbe(boolean available, int status) {
            this.available = available;
            this.status = status;
        }
    }
}
