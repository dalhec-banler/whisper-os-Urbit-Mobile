package io.nativeplanet.controller;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;

import android.system.ErrnoException;
import android.system.Os;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * NativePlanet Controller Service
 *
 * Observes Android network state and writes resolver configuration
 * for the static vere binary to use via symlinked /etc/resolv.conf.
 *
 * Gate NB-2: Controller network observer
 */
public class NativePlanetControllerService extends Service {

    private static final String TAG = "NativePlanetController";

    private static final String NATIVEPLANET_DIR = "/data/nativeplanet";
    private static final String BOOT_PACKAGE_PATH = NATIVEPLANET_DIR + "/boot-package.json";
    private static final String RESOLV_CONF_PATH = NATIVEPLANET_DIR + "/resolv.conf";
    private static final String NETWORK_STATE_PATH = NATIVEPLANET_DIR + "/network-state.json";
    private static final String VERE_ENABLED_PROP = "nativeplanet.vere.enabled";

    // File mode for resolver files (group-readable via setgid inheritance)
    private static final int FILE_MODE_0640 = 0640;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private RuntimeStatusPoller runtimeStatusPoller;
    private HostedAppsPoller hostedAppsPoller;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "NativePlanet Controller Service starting");

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Ensure directory exists
        ensureNativePlanetDir();

        // Start network observation
        startNetworkObserver();

        // Start runtime status polling
        startRuntimeStatusPoller();

        // Start hosted Urbit app inventory polling
        startHostedAppsPoller();

        // Start init-managed vere automatically when a boot package exists.
        // The explicit "0" state is respected so manual stop remains possible.
        maybeStartVereFromBootPackage();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "NativePlanet Controller Service stopping");
        stopNetworkObserver();
        stopRuntimeStatusPoller();
        stopHostedAppsPoller();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not a bound service
        return null;
    }

    // --- Network observation ---

    private void startNetworkObserver() {
        Log.i(TAG, "Starting network observer");

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Network available: " + network);
                // Always recompute from current active network
                recomputeActiveNetworkState();
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Network lost: " + network);
                // Always recompute - another network may now be active
                // Important for WiFi <-> Helium handoff
                recomputeActiveNetworkState();
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                Log.d(TAG, "Network capabilities changed: " + network);
                // Always recompute from current active network
                recomputeActiveNetworkState();
            }

            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties props) {
                Log.d(TAG, "Link properties changed: " + network);
                // Always recompute from current active network
                recomputeActiveNetworkState();
            }
        };

        connectivityManager.registerNetworkCallback(request, networkCallback);

        // Write initial state
        recomputeActiveNetworkState();
    }

    /**
     * Recompute and write network state based on current active validated network.
     * Called on every network callback to handle WiFi <-> mobile handoff correctly.
     */
    private void recomputeActiveNetworkState() {
        Network activeNetwork = connectivityManager.getActiveNetwork();

        if (activeNetwork == null) {
            Log.i(TAG, "No active network");
            clearNetworkState();
            return;
        }

        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (caps == null) {
            Log.w(TAG, "Active network has no capabilities");
            clearNetworkState();
            return;
        }

        // Only use validated networks
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            Log.i(TAG, "Active network not validated, treating as offline");
            clearNetworkState();
            return;
        }

        updateNetworkState(activeNetwork);
    }

    private void stopNetworkObserver() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    // --- Runtime status polling ---

    private void startRuntimeStatusPoller() {
        Log.i(TAG, "Starting runtime status poller");
        runtimeStatusPoller = new RuntimeStatusPoller();
        runtimeStatusPoller.start();
    }

    private void stopRuntimeStatusPoller() {
        if (runtimeStatusPoller != null) {
            runtimeStatusPoller.stop();
            runtimeStatusPoller = null;
        }
    }

    private void startHostedAppsPoller() {
        Log.i(TAG, "Starting hosted apps poller");
        hostedAppsPoller = new HostedAppsPoller();
        hostedAppsPoller.start();
    }

    private void stopHostedAppsPoller() {
        if (hostedAppsPoller != null) {
            hostedAppsPoller.stop();
            hostedAppsPoller = null;
        }
    }

    private void maybeStartVereFromBootPackage() {
        File bootPackage = new File(BOOT_PACKAGE_PATH);
        if (!bootPackage.exists()) {
            Log.i(TAG, "No boot package present; not auto-starting vere");
            return;
        }

        String enabled = SystemProperties.get(VERE_ENABLED_PROP, "");
        if ("0".equals(enabled)) {
            Log.i(TAG, "Vere explicitly disabled; not auto-starting");
            return;
        }

        if ("running".equals(SystemProperties.get("init.svc.nativeplanet_vere", ""))) {
            Log.d(TAG, "Vere service already running");
            return;
        }

        Log.i(TAG, "Boot package present; auto-starting vere");
        SystemProperties.set(VERE_ENABLED_PROP, "1");
    }

    private void updateNetworkState(Network network) {
        LinkProperties linkProps = connectivityManager.getLinkProperties(network);
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);

        if (linkProps == null || caps == null) {
            Log.w(TAG, "Could not get link properties or capabilities");
            return;
        }

        String networkType = determineNetworkType(caps);
        List<String> dnsServers = extractDnsServers(linkProps);
        String interfaceName = linkProps.getInterfaceName();
        String nat64Prefix = extractNat64Prefix(linkProps);
        boolean validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        // Stacked link inspection is not exposed in this build's public SDK.
        // Keep the optional field empty while preserving DNS/network reporting.
        String stackedInterface = null;

        // Log sanitized state
        logNetworkState(networkType, interfaceName, stackedInterface, dnsServers, nat64Prefix, validated);

        // Write files
        writeResolvConf(networkType, interfaceName, dnsServers);
        writeNetworkState(networkType, interfaceName, stackedInterface, dnsServers,
                          linkProps, nat64Prefix, validated);
    }

    private void clearNetworkState() {
        Log.i(TAG, "No active network, clearing state");
        writeResolvConfFallback();
        writeNetworkStateDisconnected();
    }

    private String determineNetworkType(NetworkCapabilities caps) {
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "WIFI";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "CELLULAR";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return "ETHERNET";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return "VPN";
        }
        return "UNKNOWN";
    }

    private List<String> extractDnsServers(LinkProperties linkProps) {
        List<String> servers = new ArrayList<>();
        for (InetAddress addr : linkProps.getDnsServers()) {
            String hostAddr = addr.getHostAddress();
            if (hostAddr != null) {
                servers.add(hostAddr);
            }
        }
        return servers;
    }

    private String extractNat64Prefix(LinkProperties linkProps) {
        try {
            // NAT64 prefix available on API 30+
            Object prefix = linkProps.getNat64Prefix();
            if (prefix != null) {
                return prefix.toString();
            }
        } catch (Exception e) {
            // API not available
        }
        return null;
    }

    private void logNetworkState(String networkType, String iface, String stackedIface,
                                  List<String> dnsServers, String nat64Prefix, boolean validated) {
        int dnsCount = dnsServers.size();
        boolean hasIpv4 = false;
        boolean hasIpv6 = false;
        for (String dns : dnsServers) {
            try {
                InetAddress addr = InetAddress.getByName(dns);
                if (addr instanceof Inet4Address) hasIpv4 = true;
                if (addr instanceof Inet6Address) hasIpv6 = true;
            } catch (Exception e) {
                // Ignore
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Network state: type=").append(networkType);
        sb.append(", iface=").append(iface != null ? iface : "none");
        if (stackedIface != null) {
            sb.append(", clat=").append(stackedIface);
        }
        sb.append(", dns=").append(dnsCount).append(" servers");
        sb.append(" (v4=").append(hasIpv4).append(", v6=").append(hasIpv6).append(")");
        if (nat64Prefix != null) {
            sb.append(", nat64=yes");
        }
        sb.append(", validated=").append(validated);

        Log.i(TAG, sb.toString());
    }

    // --- File writing ---

    private void ensureNativePlanetDir() {
        File dir = new File(NATIVEPLANET_DIR);
        if (!dir.exists()) {
            Log.w(TAG, "NativePlanet directory does not exist: " + NATIVEPLANET_DIR);
            // init.rc should create this, but log if missing
        }
    }

    private void writeResolvConf(String networkType, String iface, List<String> dnsServers) {
        StringBuilder content = new StringBuilder();
        content.append("# Generated by NativePlanet Controller\n");
        content.append("# Network: ").append(networkType).append(" via ").append(iface).append("\n");
        content.append("# Updated: ").append(new Date()).append("\n");
        content.append("\n");

        for (String dns : dnsServers) {
            content.append("nameserver ").append(dns).append("\n");
        }

        // If no DNS servers, the file will be empty of nameservers
        // Vere will fail to resolve, which is correct behavior for no network

        writeFile(RESOLV_CONF_PATH, content.toString());
        Log.d(TAG, "Wrote resolv.conf with " + dnsServers.size() + " DNS servers");
    }

    private void writeResolvConfFallback() {
        StringBuilder content = new StringBuilder();
        content.append("# Generated by NativePlanet Controller\n");
        content.append("# Network: DISCONNECTED\n");
        content.append("# Updated: ").append(new Date()).append("\n");
        content.append("\n");
        content.append("# No network available\n");

        writeFile(RESOLV_CONF_PATH, content.toString());
        Log.d(TAG, "Wrote disconnected resolv.conf");
    }

    private void writeNetworkState(String networkType, String iface, String stackedIface,
                                    List<String> dnsServers, LinkProperties linkProps,
                                    String nat64Prefix, boolean validated) {
        long timestamp = System.currentTimeMillis();

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"networkType\": \"").append(networkType).append("\",\n");
        json.append("  \"interfaceName\": ").append(iface != null ? "\"" + iface + "\"" : "null").append(",\n");
        json.append("  \"stackedInterfaceName\": ").append(stackedIface != null ? "\"" + stackedIface + "\"" : "null").append(",\n");

        // DNS servers array
        json.append("  \"dnsServers\": [");
        for (int i = 0; i < dnsServers.size(); i++) {
            if (i > 0) json.append(", ");
            json.append("\"").append(dnsServers.get(i)).append("\"");
        }
        json.append("],\n");

        // Routes array
        json.append("  \"routes\": [\n");
        List<?> routes = linkProps.getRoutes();
        for (int i = 0; i < routes.size(); i++) {
            Object route = routes.get(i);
            if (i > 0) json.append(",\n");
            json.append("    ").append(routeToJson(route));
        }
        if (!routes.isEmpty()) json.append("\n");
        json.append("  ],\n");

        json.append("  \"nat64Prefix\": ").append(nat64Prefix != null ? "\"" + nat64Prefix + "\"" : "null").append(",\n");
        json.append("  \"validated\": ").append(validated).append(",\n");
        json.append("  \"timestampMs\": ").append(timestamp).append("\n");
        json.append("}");

        writeFile(NETWORK_STATE_PATH, json.toString());
        Log.d(TAG, "Wrote network-state.json");
    }

    private void writeNetworkStateDisconnected() {
        long timestamp = System.currentTimeMillis();

        String json = "{\n" +
                "  \"networkType\": \"NONE\",\n" +
                "  \"interfaceName\": null,\n" +
                "  \"stackedInterfaceName\": null,\n" +
                "  \"dnsServers\": [],\n" +
                "  \"routes\": [],\n" +
                "  \"nat64Prefix\": null,\n" +
                "  \"validated\": false,\n" +
                "  \"timestampMs\": " + timestamp + "\n" +
                "}";

        writeFile(NETWORK_STATE_PATH, json);
        Log.d(TAG, "Wrote disconnected network-state.json");
    }

    private String routeToJson(Object routeObj) {
        // RouteInfo object - extract fields via toString parsing or reflection
        // For simplicity, use toString representation
        String routeStr = routeObj.toString();
        // RouteInfo format varies, simplified extraction
        return "\"" + routeStr.replace("\"", "\\\"") + "\"";
    }

    private void writeFile(String path, String content) {
        try {
            FileWriter writer = new FileWriter(path);
            writer.write(content);
            writer.close();

            // Set ownership to system:shell and mode 0640 so vere (running as shell) can read
            setFilePermissions(path);
        } catch (IOException e) {
            Log.e(TAG, "Failed to write " + path + ": " + e.getMessage());
        }
    }

    private void setFilePermissions(String path) {
        try {
            // Group shell is inherited via setgid on /data/nativeplanet (mode 2770)
            // We only need to chmod to 0640 for group-readable
            Os.chmod(path, FILE_MODE_0640);
        } catch (ErrnoException e) {
            Log.w(TAG, "Failed to chmod " + path + ": " + e.getMessage());
        }
    }
}
