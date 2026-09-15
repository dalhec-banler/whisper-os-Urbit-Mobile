package io.nativeplanet.home.data

import android.util.Log
import io.nativeplanet.home.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin client for the moon's local web server. Logs in once with the code the
 * controller hands us, keeps the session cookie in memory only, and answers
 * JSON scries. The URL is a build-time property so the same code runs against a
 * ship on the development Mac.
 */
class Eyre(private val baseUrl: String = BuildConfig.SHIP_URL) {
    companion object { private const val TAG = "WhisperHome.Eyre" }

    private val jar = object : CookieJar {
        private val store = java.util.concurrent.ConcurrentHashMap<String, Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { store[it.name] = it }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = store.values.toList()
    }
    private val http = OkHttpClient.Builder()
        .cookieJar(jar)
        .followRedirects(false)
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile var loggedIn = false
        private set

    suspend fun login(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$baseUrl/~/login")
                .post(FormBody.Builder().add("password", code).build())
                .build()
            http.newCall(req).execute().use { res ->
                val ok = res.code in 200..399 && res.headers("set-cookie").any { it.startsWith("urbauth-") }
                loggedIn = ok
                ok
            }
        } catch (e: Exception) {
            Log.w(TAG, "login failed: ${e.javaClass.simpleName}"); loggedIn = false; false
        }
    }

    /** JSON scry: `app/path` without leading slash, e.g. "contacts/all". */
    suspend fun scry(appPath: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/~/scry/$appPath.json").get().build()
            http.newCall(req).execute().use { res ->
                if (res.code != 200) return@withContext null
                val body = res.body?.string() ?: return@withContext null
                if (body.trimStart().startsWith("[")) JSONObject("{\"list\":$body}") else JSONObject(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "scry $appPath failed: ${e.javaClass.simpleName}: ${e.message}"); null
        }
    }

    private var channelId: String? = null
    private var channelSeq = 0

    /** Poke an agent through an Eyre channel. Returns true when Eyre accepted the poke. */
    suspend fun poke(app: String, mark: String, json: JSONObject, ship: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val id = channelId ?: ("whisper-" + System.currentTimeMillis()).also { channelId = it }
            channelSeq += 1
            val body = org.json.JSONArray().put(JSONObject()
                .put("id", channelSeq).put("action", "poke").put("ship", ship.trimStart('~'))
                .put("app", app).put("mark", mark).put("json", json))
            val req = Request.Builder().url("$baseUrl/~/channel/$id")
                .put(okhttp3.RequestBody.create("application/json".toMediaType(), body.toString())).build()
            http.newCall(req).execute().use { res -> res.code in 200..299 }
        } catch (e: Exception) {
            Log.w(TAG, "poke $app failed: ${e.javaClass.simpleName}"); false
        }
    }

    /** Absolute URL for an app path on the ship, for opening in the ROM's hosted view. */
    fun url(path: String): String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
