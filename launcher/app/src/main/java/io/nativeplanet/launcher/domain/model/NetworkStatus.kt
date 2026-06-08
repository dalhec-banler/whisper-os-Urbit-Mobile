package io.nativeplanet.launcher.domain.model

data class NetworkStatus(
    val type: NetworkType,
    val interfaceName: String?,
    val stackedInterfaceName: String?,
    val validated: Boolean,
    val dnsServers: List<String>,
    val nat64Prefix: String?,
    val timestampMs: Long,
    val resolverAvailable: Boolean,
    val resolverContents: String?
) {
    companion object {
        val DISCONNECTED = NetworkStatus(
            type = NetworkType.NONE,
            interfaceName = null,
            stackedInterfaceName = null,
            validated = false,
            dnsServers = emptyList(),
            nat64Prefix = null,
            timestampMs = System.currentTimeMillis(),
            resolverAvailable = false,
            resolverContents = null
        )
    }
}

enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    VPN,
    NONE
}
