package io.nativeplanet.controller;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Polls the running ship's %docket app inventory through conn.sock.
 */
public class HostedAppsPoller {

    private static final String TAG = "HostedAppsPoller";

    private static final String NATIVEPLANET_DIR = "/data/nativeplanet";
    private static final String BOOT_PACKAGE_PATH = NATIVEPLANET_DIR + "/boot-package.json";
    private static final String HOSTED_APPS_PATH = NATIVEPLANET_DIR + "/hosted-apps.json";
    private static final String CONN_SOCK_SUFFIX = "/.urb/conn.sock";

    private static final int FILE_MODE_0640 = 0640;
    private static final long POLL_INTERVAL_MS = 60000;
    private static final long RETRY_INTERVAL_MS = 15000;

    private static final String TLON_ANDROID_PACKAGE = "network.tlon";

    private static final String DOCKET_CHARGES_HOON =
            "=/  m  (strand ,vase)  " +
            ";<  x=*  bind:m  (scry * /gx/docket/charges/noun)  " +
            "(pure:m !>(x))";

    private static final String NATIVEPLANET_MOBILE_APPS_HOON =
            "=/  m  (strand ,vase)  " +
            ";<  x=json  bind:m  (scry json /gx/nativeplanet-mobile/apps/json)  " +
            "(pure:m !>((en:json:html x)))";

    private HandlerThread handlerThread;
    private Handler handler;
    private boolean running = false;
    private final Context context;

    public HostedAppsPoller(Context context) {
        this.context = context.getApplicationContext();
    }

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;

            boolean success = false;
            try {
                success = pollAndWriteHostedApps();
            } catch (Exception e) {
                Log.w(TAG, "Hosted apps poll failed: " + e.getMessage());
                writeErrorState("poll-error");
            }

            if (running) {
                handler.postDelayed(this, success ? POLL_INTERVAL_MS : RETRY_INTERVAL_MS);
            }
        }
    };

    public void start() {
        if (running) return;

        Log.i(TAG, "Starting HostedAppsPoller");
        running = true;

        handlerThread = new HandlerThread("HostedAppsPoller");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handler.post(pollRunnable);
    }

    public void stop() {
        if (!running) return;

        Log.i(TAG, "Stopping HostedAppsPoller");
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

    private boolean pollAndWriteHostedApps() throws IOException, JSONException {
        String pierPath = readPierPath();
        if (pierPath == null) {
            writeErrorState("no-boot-package");
            return false;
        }

        String connSockPath = pierPath + CONN_SOCK_SUFFIX;
        if (!new File(connSockPath).exists()) {
            writeErrorState("conn-sock-missing");
            return false;
        }

        NounCodec.Noun response;
        String mobileAppsJson = null;
        try (ConnSockClient client = new ConnSockClient(connSockPath)) {
            client.connect();
            response = client.sendKhanEval(DOCKET_CHARGES_HOON);
            try {
                mobileAppsJson = NounCodec.parseFyrdCordResponse(
                        client.sendKhanEval(NATIVEPLANET_MOBILE_APPS_HOON));
            } catch (Exception e) {
                Log.i(TAG, "No nativeplanet-mobile app metadata available: " + e.getMessage());
            }
        }

        NounCodec.Noun value = parseKhanValue(response);
        JSONObject json = buildHostedAppsJson(value, mobileAppsJson);
        writeFile(HOSTED_APPS_PATH, json.toString(2));
        return true;
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
            Log.w(TAG, "Failed to read boot-package.json: " + e.getMessage());
            return null;
        }
    }

    private NounCodec.Noun parseKhanValue(NounCodec.Noun noun) throws IOException {
        if (!(noun instanceof NounCodec.Cell)) {
            throw new IOException("khan response not a cell");
        }

        NounCodec.Cell top = (NounCodec.Cell) noun;
        if (!(top.tail instanceof NounCodec.Cell)) {
            throw new IOException("khan response missing tag");
        }

        NounCodec.Cell tagged = (NounCodec.Cell) top.tail;
        if (!isCord(tagged.head, "avow") || !(tagged.tail instanceof NounCodec.Cell)) {
            throw new IOException("khan response not avow");
        }

        NounCodec.Cell result = (NounCodec.Cell) tagged.tail;
        if (!isAtomValue(result.head, BigInteger.ZERO)) {
            throw new IOException("khan thread failed");
        }
        if (!(result.tail instanceof NounCodec.Cell)) {
            throw new IOException("khan response missing page");
        }

        NounCodec.Cell page = (NounCodec.Cell) result.tail;
        if (!isCord(page.head, "noun")) {
            throw new IOException("khan response mark not noun");
        }
        return page.tail;
    }

    private JSONObject buildHostedAppsJson(NounCodec.Noun value, String mobileAppsJson)
            throws JSONException, IOException {
        if (!(value instanceof NounCodec.Cell)) {
            throw new IOException("docket response not a cell");
        }
        NounCodec.Cell update = (NounCodec.Cell) value;
        if (!isCord(update.head, "initial")) {
            throw new IOException("docket response not initial");
        }

        List<JSONObject> apps = new ArrayList<>();
        walkMap(update.tail, apps);
        boolean mobileMetadataAvailable = applyMobileMetadata(apps, mobileAppsJson);
        apps.sort(Comparator.comparing(app -> app.optString("title", "")));

        JSONArray array = new JSONArray();
        for (JSONObject app : apps) {
            array.put(app);
        }

        JSONObject json = new JSONObject();
        long now = System.currentTimeMillis();
        json.put("version", 1);
        json.put("source", mobileMetadataAvailable ? "docket+nativeplanet-mobile" : "docket");
        json.put("mobileMetadataAvailable", mobileMetadataAvailable);
        json.put("timestampMs", now);
        json.put("lastPollAttemptMs", now);
        json.put("lastError", JSONObject.NULL);
        json.put("stale", false);
        json.put("apps", array);
        return json;
    }

    private boolean applyMobileMetadata(List<JSONObject> apps, String mobileAppsJson)
            throws JSONException {
        if (mobileAppsJson == null || mobileAppsJson.isEmpty()) {
            return false;
        }

        JSONObject metadata;
        try {
            metadata = new JSONObject(mobileAppsJson);
        } catch (JSONException e) {
            Log.w(TAG, "Malformed nativeplanet-mobile app metadata: " + e.getMessage());
            return false;
        }

        JSONArray mobileApps = metadata.optJSONArray("apps");
        if (mobileApps == null) {
            return false;
        }

        Map<String, JSONObject> byDesk = new LinkedHashMap<>();
        for (JSONObject app : apps) {
            String key = app.optString("desk", app.optString("id", ""));
            if (!key.isEmpty()) {
                byDesk.put(key, app);
            }
        }

        for (int i = 0; i < mobileApps.length(); i++) {
            JSONObject mobile = mobileApps.optJSONObject(i);
            if (mobile == null) {
                continue;
            }

            String desk = mobile.optString("desk", "");
            if (desk.isEmpty()) {
                continue;
            }

            if (mobile.optBoolean("hidden", false)) {
                byDesk.remove(desk);
                continue;
            }

            JSONObject app = byDesk.get(desk);
            if (app == null) {
                app = buildMetadataOnlyApp(desk);
                byDesk.put(desk, app);
            }

            applyMobileAppMetadata(app, mobile);
        }

        apps.clear();
        apps.addAll(byDesk.values());
        return true;
    }

    private JSONObject buildMetadataOnlyApp(String desk) throws JSONException {
        JSONObject app = new JSONObject();
        app.put("id", desk);
        app.put("desk", desk);
        app.put("title", titleFromDesk(desk));
        app.put("info", "");
        app.put("tileColor", "#c79338");
        app.put("launchMode", "");
        app.put("basePath", JSONObject.NULL);
        app.put("startUrl", "");
        app.put("sourceUrl", JSONObject.NULL);
        app.put("imageUrl", JSONObject.NULL);
        app.put("version", JSONObject.NULL);
        app.put("website", "");
        app.put("license", "");
        app.put("availability", "nativeplanet-mobile");
        app.put("androidPackage", JSONObject.NULL);
        app.put("pwaManifestUrl", JSONObject.NULL);
        return app;
    }

    private void applyMobileAppMetadata(JSONObject app, JSONObject mobile) throws JSONException {
        String preferredLaunchMode = normalizeLaunchMode(
                mobile.optString("preferredLaunchMode", ""));
        String mobilePath = mobile.optString("mobilePath", "");
        String androidPackage = mobile.optString("androidPackage", "");
        String pwaManifestPath = mobile.optString("pwaManifestPath", "");

        if (!mobilePath.isEmpty()) {
            app.put("basePath", mobilePath);
            app.put("startUrl", "");
        }

        if (!pwaManifestPath.isEmpty()) {
            app.put("pwaManifestUrl", pwaManifestPath);
        }

        if (!androidPackage.isEmpty()) {
            app.put("androidPackage", androidPackage);
        }

        if ("native".equals(preferredLaunchMode)) {
            if (!androidPackage.isEmpty() && isLaunchablePackageInstalled(androidPackage)) {
                app.put("launchMode", "native");
                app.put("androidPackage", androidPackage);
            } else if (!mobilePath.isEmpty()) {
                app.put("launchMode", "local_webview");
            }
        } else if ("pwa".equals(preferredLaunchMode)) {
            if (!mobilePath.isEmpty()) {
                app.put("launchMode", "pwa");
            }
        } else if ("local_webview".equals(preferredLaunchMode)) {
            if (!mobilePath.isEmpty()) {
                app.put("launchMode", "local_webview");
            }
        } else if ("browser".equals(preferredLaunchMode)) {
            app.put("launchMode", "browser");
        }

        app.put("recommended", mobile.optBoolean("recommended", false));
        app.put("hidden", false);
        app.put("mobileMetadata", true);
    }

    private String normalizeLaunchMode(String launchMode) {
        if ("native".equals(launchMode)
                || "pwa".equals(launchMode)
                || "local_webview".equals(launchMode)
                || "browser".equals(launchMode)) {
            return launchMode;
        }
        return "";
    }

    private String titleFromDesk(String desk) {
        if ("groups".equals(desk)) return "Tlon";
        if ("webterm".equals(desk)) return "Terminal";
        if ("dojo".equals(desk)) return "Dojo";
        if ("landscape".equals(desk)) return "Landscape";
        if ("grove".equals(desk)) return "Grove";
        if ("kin".equals(desk)) return "Kin";
        return desk;
    }

    private void walkMap(NounCodec.Noun node, List<JSONObject> apps) {
        if (isNull(node) || !(node instanceof NounCodec.Cell)) {
            return;
        }

        NounCodec.Cell cell = (NounCodec.Cell) node;
        if (!(cell.tail instanceof NounCodec.Cell)) {
            return;
        }

        NounCodec.Noun entry = cell.head;
        NounCodec.Cell branches = (NounCodec.Cell) cell.tail;

        if (entry instanceof NounCodec.Cell) {
            NounCodec.Cell entryCell = (NounCodec.Cell) entry;
            String desk = atomToCord(entryCell.head);
            if (desk != null && !desk.isEmpty()) {
                try {
                    apps.add(parseCharge(desk, entryCell.tail));
                } catch (JSONException e) {
                    Log.w(TAG, "Skipping malformed docket entry for %" + desk + ": "
                            + e.getMessage());
                }
            }
        }

        walkMap(branches.head, apps);
        walkMap(branches.tail, apps);
    }

    private JSONObject parseCharge(String desk, NounCodec.Noun charge) throws JSONException {
        if (!(charge instanceof NounCodec.Cell)) {
            throw new JSONException("invalid charge");
        }

        NounCodec.Cell chargeCell = (NounCodec.Cell) charge;
        List<NounCodec.Noun> docket = tuple(chargeCell.head);
        List<NounCodec.Noun> chad = tuple(chargeCell.tail);
        Href href = parseHref(get(docket, 4));

        JSONObject app = new JSONObject();
        app.put("id", desk);
        app.put("desk", desk);
        app.put("title", fallback(atomToCord(get(docket, 1)), desk));
        app.put("info", fallback(atomToCord(get(docket, 2)), ""));
        app.put("tileColor", atomToColor(get(docket, 3)));
        app.put("launchMode", href.launchMode);
        app.put("basePath", href.basePath != null ? href.basePath : JSONObject.NULL);
        app.put("startUrl", "");
        app.put("sourceUrl", href.sourceUrl != null ? href.sourceUrl : JSONObject.NULL);
        app.put("imageUrl", unitCord(get(docket, 5)));
        app.put("version", versionString(get(docket, 6)));
        app.put("website", fallback(atomToCord(get(docket, 7)), ""));
        app.put("license", fallback(atomToCord(get(docket, 8)), ""));
        app.put("availability", fallback(atomToCord(get(chad, 0)), "unknown"));
        app.put("androidPackage", JSONObject.NULL);
        app.put("pwaManifestUrl", JSONObject.NULL);
        applyKnownCompanionMetadata(app);
        return app;
    }

    private void applyKnownCompanionMetadata(JSONObject app) throws JSONException {
        String id = app.optString("id", "");
        String desk = app.optString("desk", "");
        if (!"groups".equals(id) && !"groups".equals(desk) && !"tlon".equals(id)) {
            return;
        }

        if (isLaunchablePackageInstalled(TLON_ANDROID_PACKAGE)) {
            app.put("launchMode", "native");
            app.put("androidPackage", TLON_ANDROID_PACKAGE);
        }
    }

    private boolean isLaunchablePackageInstalled(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        return packageManager != null
                && packageManager.getLaunchIntentForPackage(packageName) != null;
    }

    private Href parseHref(NounCodec.Noun noun) {
        List<NounCodec.Noun> parts = tuple(noun);
        String type = atomToCord(get(parts, 0));
        if (!"glob".equals(type)) {
            return new Href("browser", null, null);
        }

        String base = atomToCord(get(parts, 1));
        String location = atomToCord(get(parts, 3));
        String sourceUrl = "http".equals(location) ? atomToCord(get(parts, 4)) : null;
        if (base == null || base.isEmpty()) {
            return new Href("browser", null, sourceUrl);
        }
        return new Href("local_webview", "/apps/" + base + "/", sourceUrl);
    }

    private List<NounCodec.Noun> tuple(NounCodec.Noun noun) {
        ArrayList<NounCodec.Noun> out = new ArrayList<>();
        NounCodec.Noun cursor = noun;
        while (cursor instanceof NounCodec.Cell) {
            NounCodec.Cell cell = (NounCodec.Cell) cursor;
            out.add(cell.head);
            cursor = cell.tail;
        }
        out.add(cursor);
        return out;
    }

    private NounCodec.Noun get(List<NounCodec.Noun> list, int index) {
        return index >= 0 && index < list.size() ? list.get(index) : NounCodec.Atom.ZERO;
    }

    private String unitCord(NounCodec.Noun noun) {
        if (isNull(noun)) {
            return null;
        }
        if (noun instanceof NounCodec.Cell) {
            NounCodec.Cell cell = (NounCodec.Cell) noun;
            if (isNull(cell.head)) {
                return atomToCord(cell.tail);
            }
        }
        return null;
    }

    private String versionString(NounCodec.Noun noun) {
        List<NounCodec.Noun> parts = tuple(noun);
        if (parts.size() < 3) {
            return null;
        }
        return atomLong(get(parts, 0)) + "." + atomLong(get(parts, 1)) + "." + atomLong(get(parts, 2));
    }

    private long atomLong(NounCodec.Noun noun) {
        if (!(noun instanceof NounCodec.Atom)) {
            return 0;
        }
        return ((NounCodec.Atom) noun).value.longValue();
    }

    private String atomToColor(NounCodec.Noun noun) {
        if (!(noun instanceof NounCodec.Atom)) {
            return "#c79338";
        }
        String hex = ((NounCodec.Atom) noun).value.toString(16);
        if (hex.length() < 6) {
            hex = "000000".substring(hex.length()) + hex;
        }
        if (hex.length() > 6) {
            hex = hex.substring(hex.length() - 6);
        }
        return "#" + hex;
    }

    private String atomToCord(NounCodec.Noun noun) {
        if (!(noun instanceof NounCodec.Atom)) {
            return null;
        }
        return ((NounCodec.Atom) noun).toCord();
    }

    private boolean isCord(NounCodec.Noun noun, String cord) {
        String value = atomToCord(noun);
        return cord.equals(value);
    }

    private boolean isAtomValue(NounCodec.Noun noun, BigInteger value) {
        return noun instanceof NounCodec.Atom && ((NounCodec.Atom) noun).value.equals(value);
    }

    private boolean isNull(NounCodec.Noun noun) {
        return isAtomValue(noun, BigInteger.ZERO);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private void writeErrorState(String reason) {
        JSONObject existing = readExistingHostedAppsJson();
        JSONArray apps = existing != null ? existing.optJSONArray("apps") : null;
        if (apps != null && apps.length() > 0) {
            try {
                existing.put("lastPollAttemptMs", System.currentTimeMillis());
                existing.put("lastError", reason);
                existing.put("stale", true);
                writeFile(HOSTED_APPS_PATH, existing.toString(2));
                return;
            } catch (JSONException e) {
                Log.w(TAG, "Failed to mark hosted apps inventory stale");
            }
        }

        writeEmptyApps(reason);
    }

    private JSONObject readExistingHostedAppsJson() {
        try {
            File file = new File(HOSTED_APPS_PATH);
            if (!file.exists()) {
                return null;
            }
            return new JSONObject(new String(Files.readAllBytes(file.toPath())));
        } catch (Exception e) {
            return null;
        }
    }

    private void writeEmptyApps(String reason) {
        try {
            long now = System.currentTimeMillis();
            JSONObject json = new JSONObject();
            json.put("version", 1);
            json.put("source", "docket");
            json.put("timestampMs", now);
            json.put("lastPollAttemptMs", now);
            json.put("lastError", reason);
            json.put("stale", true);
            json.put("apps", new JSONArray());
            writeFile(HOSTED_APPS_PATH, json.toString(2));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to build empty hosted apps JSON");
        }
    }

    private void writeFile(String path, String content) {
        String tempPath = path + ".tmp";
        File tempFile = new File(tempPath);
        File targetFile = new File(path);

        try {
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(content);
            }

            Os.chmod(tempPath, FILE_MODE_0640);

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

    private static final class Href {
        final String launchMode;
        final String basePath;
        final String sourceUrl;

        Href(String launchMode, String basePath, String sourceUrl) {
            this.launchMode = launchMode;
            this.basePath = basePath;
            this.sourceUrl = sourceUrl;
        }
    }
}
