package io.nativeplanet.controller;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parent-assisted moon provisioning entry point.
 *
 * This validates the launcher/controller contract and performs the first real
 * remote checkpoint: authenticate to the parent ship's Eyre endpoint with the
 * supplied +code, ask Artemis to create a mobile moon, then pass the returned
 * boot key to the local provisioning path. Raw +codes and boot keys must stay
 * out of logs and response JSON.
 */
public final class ParentPairingManager {

    private static final String TAG = "ParentPairingManager";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final String AUTH_CONFIRM_SCRY = "/~/scry/hood/kiln/pikes.json";
    private static final String ARTEMIS_APP_PATH = "/apps/artemis/";
    private static final String ARTEMIS_MOONS_SCRY = "/~/scry/artemis/mons.json";
    private static final int MOON_CREATE_TIMEOUT_MS = 30000;
    private static final int MOON_CREATE_POLL_INTERVAL_MS = 2000;
    private static final SecureRandom RANDOM = new SecureRandom();

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
            if (!probe.available) {
                return response(false, "PARENT_SERVICE_UNAVAILABLE",
                        "Planet login worked. Artemis is not installed on the planet yet.");
            }

            String parentShip = fetchParentShip(normalizedUrl, session.cookie);
            if (parentShip == null || parentShip.isEmpty()) {
                return response(false, "PARENT_PROTOCOL_UNSUPPORTED",
                        "Planet login worked, but the parent ship name could not be confirmed.");
            }

            MoonList before = fetchArtemisMoons(normalizedUrl, session.cookie);
            if (!before.available) {
                return response(false, "PARENT_PROTOCOL_UNSUPPORTED",
                        "Artemis is installed, but its mobile provisioning scry is not available yet.");
            }

            String label = buildMobileMoonLabel();
            if (!requestMobileMoon(normalizedUrl, session.cookie, parentShip, label)) {
                return response(false, "PARENT_MOON_CREATE_FAILED",
                        "Artemis did not accept the mobile moon request.");
            }

            MobileMoon moon = waitForCreatedMobileMoon(normalizedUrl, session.cookie, before.knownShips);
            if (moon == null) {
                return response(false, "PARENT_MOON_CREATE_TIMEOUT",
                        "Artemis did not return a new mobile moon in time.");
            }

            return ProvisioningManager.provisionMoon(buildProvisioningRequest(moon, parentShip));
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

    private static String fetchParentShip(String hostUrl, String cookie) throws IOException {
        URL url = URI.create(hostUrl + "/~/host").toURL();
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", cookie);

        int status = connection.getResponseCode();
        String body = status == HttpURLConnection.HTTP_OK ? readResponseBody(connection) : null;
        connection.disconnect();

        if (body == null) {
            return null;
        }

        String trimmed = body.trim();
        if (trimmed.startsWith("~")) {
            trimmed = trimmed.substring(1);
        }
        if (isBareShipName(trimmed)) {
            return trimmed;
        }
        return null;
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

    private static MoonList fetchArtemisMoons(String hostUrl, String cookie) throws IOException, JSONException {
        URL url = URI.create(hostUrl + ARTEMIS_MOONS_SCRY).toURL();
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", cookie);

        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            connection.disconnect();
            return MoonList.unavailable();
        }

        String body = readResponseBody(connection);
        connection.disconnect();

        JSONObject json = new JSONObject(body);
        MoonList result = new MoonList(true);
        org.json.JSONArray moons = json.optJSONArray("moons");
        if (moons == null) {
            return result;
        }

        for (int i = 0; i < moons.length(); i++) {
            JSONObject item = moons.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String who = normalizeShip(item.optString("who", ""));
            if (!who.isEmpty()) {
                result.knownShips.add(who);
            }
            MobileMoon moon = mobileMoonFromJson(item);
            if (moon != null) {
                result.mobileMoons.add(moon);
            }
        }
        return result;
    }

    private static boolean requestMobileMoon(String hostUrl, String cookie, String parentShip,
                                             String label) throws IOException, JSONException {
        String channelId = buildChannelId();
        URL url = URI.create(hostUrl + "/~/channel/" + channelId).toURL();
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Cookie", cookie);
        connection.setRequestProperty("Content-Type", "application/json");

        JSONObject makeMoon = new JSONObject()
                .put("nam", label)
                .put("rol", "mobile");
        JSONObject json = new JSONObject()
                .put("make-moon", makeMoon);
        JSONObject event = new JSONObject()
                .put("id", 1)
                .put("action", "poke")
                .put("ship", parentShip)
                .put("app", "artemis")
                .put("mark", "artemis-action")
                .put("json", json);
        org.json.JSONArray events = new org.json.JSONArray().put(event);
        byte[] body = events.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(body.length);

        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(body);
        }

        int status = connection.getResponseCode();
        connection.disconnect();
        return status >= 200 && status < 300;
    }

    private static MobileMoon waitForCreatedMobileMoon(String hostUrl, String cookie,
                                                       Set<String> knownShips)
            throws IOException, JSONException, InterruptedException {
        long deadline = System.currentTimeMillis() + MOON_CREATE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            MoonList list = fetchArtemisMoons(hostUrl, cookie);
            if (list.available) {
                for (MobileMoon moon : list.mobileMoons) {
                    if (!knownShips.contains(moon.ship)) {
                        return moon;
                    }
                }
            }
            Thread.sleep(MOON_CREATE_POLL_INTERVAL_MS);
        }
        return null;
    }

    private static MobileMoon mobileMoonFromJson(JSONObject item) {
        String role = item.optString("rol", "");
        if (!"mobile".equals(role)) {
            return null;
        }

        String ship = normalizeShip(item.optString("who", ""));
        String key = item.optString("sed", "").trim();
        if (ship.isEmpty() || key.isEmpty()) {
            return null;
        }
        return new MobileMoon(ship, key);
    }

    private static String buildProvisioningRequest(MobileMoon moon, String parentShip) throws JSONException {
        return new JSONObject()
                .put("bootMode", "MOON")
                .put("ship", moon.ship)
                .put("parent", "~" + parentShip)
                .put("keyMaterial", moon.keyMaterial)
                .put("replaceExisting", true)
                .toString();
    }

    private static HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
        return connection;
    }

    private static String readResponseBody(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        stream.close();
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private static String buildChannelId() {
        byte[] bytes = new byte[3];
        RANDOM.nextBytes(bytes);
        StringBuilder suffix = new StringBuilder();
        for (byte value : bytes) {
            suffix.append(String.format("%02x", value & 0xff));
        }
        return (System.currentTimeMillis() / 1000L) + "-" + suffix;
    }

    private static String buildMobileMoonLabel() {
        return "NativePlanet Mobile " + Long.toString(System.currentTimeMillis(), 36);
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

    private static String normalizeShip(String ship) {
        if (ship == null) {
            return "";
        }
        String trimmed = ship.trim();
        if (trimmed.startsWith("~")) {
            trimmed = trimmed.substring(1);
        }
        return isBareShipName(trimmed) ? "~" + trimmed : "";
    }

    private static boolean isBareShipName(String ship) {
        if (ship == null || ship.isEmpty() || ship.length() > 63) {
            return false;
        }
        for (int i = 0; i < ship.length(); i++) {
            char c = ship.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
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

    private static final class MoonList {
        final boolean available;
        final Set<String> knownShips = new HashSet<>();
        final java.util.ArrayList<MobileMoon> mobileMoons = new java.util.ArrayList<>();

        MoonList(boolean available) {
            this.available = available;
        }

        static MoonList unavailable() {
            return new MoonList(false);
        }
    }

    private static final class MobileMoon {
        final String ship;
        final String keyMaterial;

        MobileMoon(String ship, String keyMaterial) {
            this.ship = ship;
            this.keyMaterial = keyMaterial;
        }
    }
}
