package io.nativeplanet.launcher.domain.model

import java.time.Instant

data class RuntimeStatus(
    val state: RuntimeState,
    val shipName: String?,
    val bootMode: BootMode?,
    val pid: Int?,
    val uptimeMs: Long?,
    val lastStartTime: Instant?,
    val lastStopTime: Instant?,
    val lastError: RuntimeError?,
    val exitCode: Int?,
    val version: String? = null,
    val lastSuccessfulPoll: Long? = null,
    val connSockAvailable: Boolean = false
) {
    companion object {
        val UNINITIALIZED = RuntimeStatus(
            state = RuntimeState.UNINITIALIZED,
            shipName = null,
            bootMode = null,
            pid = null,
            uptimeMs = null,
            lastStartTime = null,
            lastStopTime = null,
            lastError = null,
            exitCode = null,
            version = null,
            lastSuccessfulPoll = null,
            connSockAvailable = false
        )
    }
}

enum class RuntimeState {
    RUNNING,
    STOPPED,
    STARTING,
    STOPPING,
    ERROR,
    CRASHED,
    UNINITIALIZED
}

enum class BootMode {
    MOON,
    COMET,
    PLANET,
    FAKE_TEST
}

data class RuntimeError(
    val code: String,
    val message: String,
    val timestamp: Instant
)
