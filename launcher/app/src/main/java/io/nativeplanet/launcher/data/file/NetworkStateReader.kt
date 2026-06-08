package io.nativeplanet.launcher.data.file

import android.os.FileObserver
import android.util.Log
import io.nativeplanet.launcher.domain.model.NetworkStatus
import io.nativeplanet.launcher.domain.model.NetworkType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStateReader @Inject constructor() {

    companion object {
        private const val TAG = "NetworkStateReader"
        private const val NETWORK_STATE_PATH = "/data/nativeplanet/network-state.json"
        private const val RESOLV_CONF_PATH = "/data/nativeplanet/resolv.conf"
    }

    fun observeNetworkState(): Flow<NetworkStatus> = callbackFlow {
        val file = File(NETWORK_STATE_PATH)

        fun readAndEmit() {
            val status = readNetworkState()
            trySend(status)
        }

        val observer = object : FileObserver(file.parent ?: "/data/nativeplanet", MODIFY or CREATE) {
            override fun onEvent(event: Int, path: String?) {
                if (path == "network-state.json") {
                    Log.d(TAG, "network-state.json changed")
                    readAndEmit()
                }
            }
        }

        observer.startWatching()
        readAndEmit()

        awaitClose {
            observer.stopWatching()
        }
    }

    fun readNetworkState(): NetworkStatus {
        val file = File(NETWORK_STATE_PATH)
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "Cannot read $NETWORK_STATE_PATH")
            return NetworkStatus.DISCONNECTED
        }

        return try {
            val json = JSONObject(file.readText())
            val resolverContents = readResolverContents()

            NetworkStatus(
                type = parseNetworkType(json.optString("networkType", "NONE")),
                interfaceName = json.optString("interfaceName").takeIf { it.isNotEmpty() },
                stackedInterfaceName = json.optString("stackedInterfaceName").takeIf { it.isNotEmpty() },
                validated = json.optBoolean("validated", false),
                dnsServers = parseDnsServers(json),
                nat64Prefix = json.optString("nat64Prefix").takeIf { it.isNotEmpty() },
                timestampMs = json.optLong("timestampMs", System.currentTimeMillis()),
                resolverAvailable = resolverContents != null,
                resolverContents = resolverContents
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse network-state.json", e)
            NetworkStatus.DISCONNECTED
        }
    }

    private fun parseNetworkType(type: String): NetworkType {
        return when (type.uppercase()) {
            "WIFI" -> NetworkType.WIFI
            "CELLULAR" -> NetworkType.CELLULAR
            "ETHERNET" -> NetworkType.ETHERNET
            "VPN" -> NetworkType.VPN
            else -> NetworkType.NONE
        }
    }

    private fun parseDnsServers(json: JSONObject): List<String> {
        val servers = mutableListOf<String>()
        val array = json.optJSONArray("dnsServers") ?: return servers
        for (i in 0 until array.length()) {
            servers.add(array.getString(i))
        }
        return servers
    }

    private fun readResolverContents(): String? {
        val file = File(RESOLV_CONF_PATH)
        return if (file.exists() && file.canRead()) {
            file.readText()
        } else {
            null
        }
    }
}
