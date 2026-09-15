package io.nativeplanet.home.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import io.nativeplanet.home.model.Next
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Next = the first calendar instance that starts within the window and has not ended. */
class Calendar(private val context: Context) {
    suspend fun next(windowMs: Long = 4 * 60 * 60 * 1000L): Next? = withContext(Dispatchers.IO) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return@withContext null
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString()).appendPath((now + windowMs).toString()).build()
        val proj = arrayOf(CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.TITLE, CalendarContract.Instances.ALL_DAY)
        try {
            context.contentResolver.query(uri, proj, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val begin = c.getLong(0); val end = c.getLong(1); val allDay = c.getInt(3) == 1
                    if (allDay || end < now) continue
                    return@withContext Next(begin, c.getString(2)?.trim().orEmpty().ifEmpty { "busy" })
                }
            }
        } catch (_: Exception) {}
        null
    }
}
