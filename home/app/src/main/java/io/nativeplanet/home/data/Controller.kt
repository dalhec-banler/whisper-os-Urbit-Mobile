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

    /** A control result: the controller accepted the request, or said why not. */
    data class Outcome(val accepted: Boolean, val code: String, val message: String?, val bootPackage: JSONObject?)

    /** The provisioned identity as the controller sees it; key material never crosses this boundary. */
    data class BootPackage(
        val exists: Boolean, val valid: Boolean, val bootMode: String?, val ship: String?, val parent: String?,
        val pierPath: String?, val pillPath: String?, val pierExists: Boolean, val pillExists: Boolean,
        val keyFileExists: Boolean, val validationErrors: List<String>,
    )

    data class Network(val type: String, val iface: String?, val validated: Boolean, val dns: List<String>, val resolverAvailable: Boolean)

    data class Diagnostics(val recentErrors: List<String>, val controllerLogs: List<String>)

    /** True once any call has answered; false means the controller itself is missing, not a stopped ship. */
    @Volatile var available: Boolean = false
        private set

    private suspend fun call(method: String, json: String? = null): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val extras = json?.let { Bundle().apply { putString("json", it) } }
            val result: Bundle? = context.contentResolver.call(BASE, method, null, extras)
            available = true
            result?.getString("json")?.let { JSONObject(it) }
        } catch (e: Exception) {
            Log.w(TAG, "$method failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun outcome(o: JSONObject?): Outcome =
        if (o == null) Outcome(false, "CONTROLLER_UNAVAILABLE", null, null)
        else Outcome(o.optBoolean("accepted", false), o.optString("code", "UNKNOWN"), o.optString("message").takeIf { it.isNotBlank() && it != "null" }, o.optJSONObject("bootPackage"))

    fun bootPackage(o: JSONObject?): BootPackage? = o?.let {
        val errs = it.optJSONArray("validationErrors")
        BootPackage(
            exists = it.optBoolean("exists", false), valid = it.optBoolean("valid", false),
            bootMode = it.optString("bootMode").takeIf { s -> s.isNotBlank() && s != "null" },
            ship = it.optString("ship").takeIf { s -> s.isNotBlank() && s != "null" },
            parent = it.optString("parent").takeIf { s -> s.isNotBlank() && s != "null" },
            pierPath = it.optString("pierPath").takeIf { s -> s.isNotBlank() && s != "null" },
            pillPath = it.optString("pillPath").takeIf { s -> s.isNotBlank() && s != "null" },
            pierExists = it.optBoolean("pierExists", false), pillExists = it.optBoolean("pillExists", false),
            keyFileExists = it.optBoolean("keyFileExists", false),
            validationErrors = (0 until (errs?.length() ?: 0)).mapNotNull { i ->
                errs?.optJSONObject(i)?.let { e -> "${e.optString("field")}: ${e.optString("message")}" } ?: errs?.optString(i)
            },
        )
    }

    suspend fun bootPackage(): BootPackage? = bootPackage(call("getBootPackage"))

    suspend fun network(): Network? = call("getNetwork")?.let {
        val dns = it.optJSONArray("dnsServers")
        Network(
            type = it.optString("networkType", "NONE"),
            iface = it.optString("interfaceName").takeIf { s -> s.isNotBlank() && s != "null" },
            validated = it.optBoolean("validated", false),
            dns = (0 until (dns?.length() ?: 0)).map { i -> dns!!.optString(i) },
            resolverAvailable = it.optBoolean("resolverAvailable", false),
        )
    }

    suspend fun diagnostics(): Diagnostics? = call("getDiagnostics")?.let {
        val errs = it.optJSONArray("recentErrors"); val logs = it.optJSONArray("controllerLogs")
        Diagnostics(
            recentErrors = (0 until (errs?.length() ?: 0)).mapNotNull { i -> errs?.optJSONObject(i)?.let { e -> "[${e.optString("source")}] ${e.optString("message")}" } },
            controllerLogs = (0 until (logs?.length() ?: 0)).mapNotNull { i -> logs?.optJSONObject(i)?.let { l -> "${l.optString("timestamp").takeLast(12)} ${l.optString("level")} ${l.optString("message")}" } },
        )
    }

    suspend fun startRuntime(): Outcome = outcome(call("startRuntime"))

    /** Graceful: the controller sends the ship a drum-exit and reports the request, not the result. Watch runtime state for the outcome. */
    suspend fun stopRuntime(): Outcome = outcome(call("stopRuntime"))

    /** Asks the planet's Artemis for a mobile moon and provisions it. The code is used once and never kept. */
    suspend fun pairWithPlanet(hostUrl: String, accessCode: String): Outcome =
        outcome(call("pairWithPlanet", JSONObject().put("hostUrl", hostUrl).put("accessCode", accessCode).toString()))

    suspend fun provisionMoon(ship: String, parent: String, keyMaterial: String): Outcome =
        outcome(call("provisionMoon", JSONObject().put("bootMode", "MOON").put("ship", ship).put("parent", parent)
            .put("keyMaterial", keyMaterial).put("replaceExisting", true).toString()))

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
