package io.nativeplanet.home.data

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.nativeplanet.home.model.Entry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android apps reach the record only through their notifications. The listener
 * mirrors the active notifications into memory; Later renders them as ANDROID
 * lines. The user grants this once (Settings > Notification access) or the ROM
 * grants it for the home app.
 */
object NotificationStore {
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    fun replaceAll(sbns: Array<StatusBarNotification>?) {
        _entries.value = (sbns ?: emptyArray()).mapNotNull { toEntry(it) }.sortedByDescending { it.timeMs }
    }

    /** System housekeeping never enters the record; the status bar already shows it. */
    private val systemPackages = setOf("android", "com.android.systemui", "com.android.shell", "com.android.settings", "com.android.vending")

    private fun toEntry(sbn: StatusBarNotification): Entry? {
        val n = sbn.notification ?: return null
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        if (sbn.packageName in systemPackages) return null
        if (sbn.packageName == "io.nativeplanet.controller") return null
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString()?.trim().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return null
        val ongoing = n.flags and Notification.FLAG_ONGOING_EVENT != 0
        return Entry(
            id = "android:${sbn.key}",
            timeMs = sbn.postTime,
            who = title.ifEmpty { sbn.packageName.substringAfterLast('.') },
            ship = null,
            text = text.ifEmpty { title },
            source = if (ongoing) "ONGOING" else "ANDROID",
            packageName = sbn.packageName,
            priority = n.priority >= Notification.PRIORITY_HIGH,
        )
    }
}

class WhisperNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() { NotificationStore.replaceAll(safeActive()) }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { NotificationStore.replaceAll(safeActive()) }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { NotificationStore.replaceAll(safeActive()) }
    private fun safeActive(): Array<StatusBarNotification>? = try { activeNotifications } catch (e: Exception) { null }
}
