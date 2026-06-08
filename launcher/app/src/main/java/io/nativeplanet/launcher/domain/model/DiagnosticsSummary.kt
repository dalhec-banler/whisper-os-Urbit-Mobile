package io.nativeplanet.launcher.domain.model

import java.time.Instant

data class DiagnosticsSummary(
    val controllerLogs: List<LogLine>,
    val launcherLogs: List<LogLine>,
    val recentErrors: List<ErrorEntry>,
    val resolverContents: String?,
    val networkStateRaw: String?
) {
    companion object {
        val EMPTY = DiagnosticsSummary(
            controllerLogs = emptyList(),
            launcherLogs = emptyList(),
            recentErrors = emptyList(),
            resolverContents = null,
            networkStateRaw = null
        )
    }
}

data class LogLine(
    val timestamp: Instant,
    val level: LogLevel,
    val message: String
)

enum class LogLevel {
    INFO,
    WARN,
    ERROR
}

data class ErrorEntry(
    val source: String,
    val timestamp: Instant,
    val message: String
)
