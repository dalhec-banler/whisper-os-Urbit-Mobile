package io.nativeplanet.home.data

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import io.nativeplanet.home.model.HostedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The NativePlanet controller is the only truth path for runtime state and the
 * local web login code. Everything here is a content-provider call; nothing
 * touches the pier directly.
 */
class Controller(private val context: Context) {
    companion object {
        private const val TAG = "WhisperHome.Controller"
        private val BASE: Uri = Uri.parse("content://io.nativeplanet.controller")
    }

    data class Runtime(val state: String, val shipName: String?, val version: String?, val connSock: Boolean)

    private suspend fun call(method: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val result: Bundle? = context.contentResolver.call(BASE, method, null, null)
            result?.getString("json")?.let { JSONObject(it) }
        } catch (e: Exception) {
            Log.w(TAG, "$method failed: ${e.javaClass.simpleName}")
            null
        }
    }

    suspend fun runtime(): Runtime? = call("getRuntime")?.let {
        Runtime(
            state = it.optString("state", "unknown"),
            shipName = it.optString("shipName").takeIf { s -> s.isNotBlank() },
            version = it.optString("version").takeIf { s -> s.isNotBlank() },
            connSock = it.optBoolean("connSockAvailable", false),
        )
    }

    suspend fun webLoginCode(): String? = call("getWebLoginCode")?.let {
        if (it.optBoolean("ok", false)) it.optString("code").takeIf { c -> c.isNotBlank() } else null
    }

    suspend fun hostedApps(): List<HostedApp> {
        val arr = call("getHostedApps")?.optJSONArray("apps") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            HostedApp(
                id = o.optString("id"),
                desk = o.optString("desk"),
                title = o.optString("title").ifBlank { o.optString("desk") },
                launchMode = o.optString("launchMode"),
                startUrl = o.optString("startUrl").takeIf { s -> s.isNotBlank() },
                basePath = o.optString("basePath").takeIf { s -> s.isNotBlank() },
                androidPackage = o.optString("androidPackage").takeIf { s -> s.isNotBlank() },
                hostPatp = o.optString("hostPatp").takeIf { s -> s.isNotBlank() },
            )
        }
    }
}
