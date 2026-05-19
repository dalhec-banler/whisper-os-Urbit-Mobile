package io.nativeplanet.controller

/**
 * Runtime status enumeration.
 */
enum class RuntimeStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR,
    UNKNOWN
}

/**
 * Boot mode for the Urbit runtime.
 */
enum class BootMode {
    /** Fake/test ship for development */
    FAKE_TEST,

    /** Real moon satellite (not implemented in v0) */
    MOON
}

/**
 * Delegation permissions for satellite moon.
 * Controls what the satellite can do on behalf of the parent.
 */
data class DelegationConfig(
    val canReadNotifications: Boolean = false,
    val canMirrorMessages: Boolean = false,
    val canRequestPosts: Boolean = false,
    val canRequestReplies: Boolean = false,
    val canAccessFiles: Boolean = false,
    val revocationEpoch: Long = 0
)

/**
 * Network configuration for the runtime.
 * Reserved for future use.
 */
data class NetworkConfig(
    val amesPort: Int? = null,
    val httpPort: Int? = null,
    val httpsPort: Int? = null
)

/**
 * BootPackage configuration.
 *
 * Defines how the Urbit runtime should boot.
 * Written to /data/nativeplanet/boot-package.json before service start.
 *
 * Security: keyMaterialRef is a reference/handle, never raw key material.
 */
data class BootPackage(
    /** Ship name (e.g., "zod" for fake, "~sampel-palnet" for moon) */
    val ship: String,

    /** Parent planet for moon mode, null for fake ships */
    val parent: String?,

    /** Path to satellite pill */
    val pillPath: String,

    /** Path where pier will be created/resumed */
    val pierPath: String,

    /** Boot mode */
    val bootMode: BootMode,

    /**
     * Reference to key material.
     * For v0: "none"
     * For moon: reference to keystore entry, never raw key
     */
    val keyMaterialRef: String,

    /** Network configuration */
    val networkConfig: NetworkConfig = NetworkConfig(),

    /** Delegation permissions */
    val delegationConfig: DelegationConfig = DelegationConfig(),

    /** Creation timestamp in milliseconds */
    val createdAtMs: Long = System.currentTimeMillis(),

    /** Package version for compatibility */
    val packageVersion: Int = 1
)

/**
 * Result of BootPackage preparation.
 */
sealed class BootPackageResult {
    data class Success(val path: String) : BootPackageResult()
    data class ValidationError(val errors: List<String>) : BootPackageResult()
    data class WriteError(val message: String) : BootPackageResult()
}

/**
 * Result of runtime start.
 */
sealed class RuntimeStartResult {
    object Success : RuntimeStartResult()
    data class AlreadyRunning(val ship: String) : RuntimeStartResult()
    object NoBootPackage : RuntimeStartResult()
    data class StartFailed(val reason: String) : RuntimeStartResult()
}

/**
 * Result of runtime stop.
 */
sealed class RuntimeStopResult {
    object Success : RuntimeStopResult()
    object AlreadyStopped : RuntimeStopResult()
    data class StopFailed(val reason: String) : RuntimeStopResult()
}

/**
 * A single log line from the runtime.
 */
data class RuntimeLogLine(
    val timestamp: String,
    val level: String,
    val message: String
)

/**
 * Runtime health status.
 */
data class RuntimeHealth(
    val status: RuntimeStatus,
    val ship: String?,
    val pierExists: Boolean,
    val bootPackageValid: Boolean,
    val lastError: String?,
    val uptimeSeconds: Long?
)
