package io.nativeplanet.home.model

/** An app the moon hosts, as reported by the controller inventory. */
data class HostedApp(
    val id: String,
    val desk: String,
    val title: String,
    val launchMode: String,
    val startUrl: String?,
    val basePath: String?,
    val androidPackage: String?,
    val hostPatp: String?,
) {
    /** Openable through the ROM's hosted WebView path. */
    val openable: Boolean get() = launchMode == "local_webview" && (!startUrl.isNullOrBlank() || !basePath.isNullOrBlank())
}

/** Anything that can be opened by typing its name: a hosted app or an Android app. */
data class Tool(
    val key: String,
    val name: String,
    val sendable: Boolean,
    val hosted: HostedApp? = null,
    val packageName: String? = null,
)

/** A person the moon knows. */
data class Person(
    val ship: String,
    val nickname: String?,
    val avatar: String?,
) {
    val display: String get() = nickname?.takeIf { it.isNotBlank() } ?: ship
}

/** Where an Entry came from. Ship sources open Tlon; phone sources open the app that posted them. */
object Source {
    const val MESSAGE = "MESSAGE"
    const val CHAT = "CHAT"
    const val GROUP = "GROUP"
    const val ANDROID = "ANDROID"
    const val ONGOING = "ONGOING"
    /** Notification-fed sources: folded under "more from apps" unless priority. */
    val PHONE: Set<String> = setOf(ANDROID, ONGOING)
}

/** One line of the record: something that happened, from any source. */
data class Entry(
    val id: String,
    val timeMs: Long,
    val who: String,          // person or group display name
    val ship: String?,        // author ship if any
    val text: String,
    val source: String,       // one of Source
    val link: String? = null,
    val packageName: String? = null,
    val priority: Boolean = false,
)

/** The one time-bound thing coming up. */
data class Next(val atMs: Long, val title: String)
