package io.nativeplanet.home.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/** Small avatars, kept in memory for the session; a miss stays a monogram. */
private val avatarCache = LruCache<String, Bitmap>(64)
private val avatarMiss = HashSet<String>()

/**
 * A person as a mark: their profile picture when their planet has one, else
 * the first two syllable pairs of the ship name in a hairline circle. Sigils
 * proper need the glyph set; until then the name is the sigil.
 */
@Composable
internal fun Avatar(ship: String, url: String?, size: Dp = 28.dp) {
    var bmp by remember(url) { mutableStateOf(url?.let { avatarCache.get(it) }) }
    LaunchedEffect(url) {
        if (url != null && bmp == null && url !in avatarMiss) {
            bmp = withContext(Dispatchers.IO) { fetch(url) }?.also { avatarCache.put(url, it) } ?: run { avatarMiss.add(url); null }
        }
    }
    Box(Modifier.size(size).clip(CircleShape).border(1.dp, W.Hair, CircleShape), contentAlignment = Alignment.Center) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), contentDescription = ship, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        else Text(monogram(ship), color = W.Paper60, fontFamily = W.Mono, fontSize = (size.value * 0.34f).sp)
    }
}

/** "~sampel-palnet" → "SP"; "~zod" → "ZO". */
internal fun monogram(ship: String): String {
    val parts = ship.removePrefix("~").split('-', '.').filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

private fun fetch(url: String): Bitmap? = try {
    val c = URL(url).openConnection() as HttpURLConnection
    c.connectTimeout = 4000; c.readTimeout = 6000
    c.inputStream.use { s ->
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        BitmapFactory.decodeStream(s, null, opts)
    }
} catch (_: Exception) { null }
