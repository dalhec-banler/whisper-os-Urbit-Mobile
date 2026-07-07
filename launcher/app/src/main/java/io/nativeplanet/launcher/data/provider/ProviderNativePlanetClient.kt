package io.nativeplanet.launcher.data.provider

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.nativeplanet.launcher.data.stub.StubNativePlanetClient
import io.nativeplanet.launcher.domain.NativePlanetClient
import io.nativeplanet.launcher.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderNativePlanetClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stubClient: StubNativePlanetClient
) : NativePlanetClient {

    companion object {
        private const val TAG = "ProviderNativePlanetClient"
        private const val AUTHORITY = "io.nativeplanet.controller"
        private val BASE_URI: Uri = Uri.parse("content://$AUTHORITY")
        private const val POLL_INTERVAL_MS = 2000L
    }

    private val contentResolver: ContentResolver = context.contentResolver

    private var controllerAvailable: Boolean? = null

    override fun observeRuntimeStatus(): Flow<RuntimeStatus> = flow {
        while (true) {
            emit(fetchRuntimeStatus())
            delay(POLL_INTERVAL_MS)
        }
    }

    override fun observeNetworkStatus(): Flow<NetworkStatus> = flow {
        while (true) {
            emit(fetchNetworkStatus())
            delay(POLL_INTERVAL_MS)
        }
    }

    override fun observeBootPackageStatus(): Flow<BootPackageStatus> = flow {
        while (true) {
            emit(fetchBootPackageStatus())
            delay(POLL_INTERVAL_MS)
        }
    }

    override fun observeDiagnostics(): Flow<DiagnosticsSummary> = flow {
        while (true) {
            emit(fetchDiagnostics())
            delay(POLL_INTERVAL_MS * 2)
        }
    }

    override fun observeHostedApps(): Flow<List<HostedApp>> = flow {
        while (true) {
            emit(fetchHostedApps())
            delay(POLL_INTERVAL_MS * 2)
        }
    }

    override suspend fun startRuntime(): ControlResult {
        return callProviderControl("startRuntime")
    }

    override suspend fun stopRuntime(): ControlResult {
        return callProviderControl("stopRuntime")
    }

    override suspend fun restartRuntime(): ControlResult {
        val stop = callProviderControl("stopRuntime")
        if (stop is ControlResult.Failed) {
            return stop
        }
        delay(3000)
        return callProviderControl("startRuntime")
    }

    override suspend fun provisionMoon(
        shipName: String,
        parentName: String,
        keyMaterial: String
    ): ControlResult {
        val request = JSONObject()
            .put("bootMode", "MOON")
            .put("ship", shipName)
            .put("parent", parentName)
            .put("keyMaterial", keyMaterial)
            .put("replaceExisting", true)
            .toString()
        return callProviderControl("provisionMoon", request)
    }

    override suspend fun pairWithPlanet(hostUrl: String, accessCode: String): ControlResult {
        val request = JSONObject()
            .put("hostUrl", hostUrl)
            .put("accessCode", accessCode)
            .toString()
        return callProviderControl("pairWithPlanet", request)
    }

    fun isControllerAvailable(): Boolean = controllerAvailable == true

    private fun fetchRuntimeStatus(): RuntimeStatus {
        val json = queryProvider("getRuntime") ?: run {
            controllerAvailable = false
            return stubClient.observeRuntimeStatus().let { RuntimeStatus.UNINITIALIZED }
        }
        controllerAvailable = true

        return try {
            val obj = JSONObject(json)
            RuntimeStatus(
                state = parseRuntimeState(obj.optString("state", "unknown")),
                shipName = obj.optStringOrNull("shipName"),
                bootMode = obj.optStringOrNull("bootMode")?.let { parseBootMode(it) },
                pid = obj.optIntOrNull("pid"),
                uptimeMs = obj.optLongOrNull("uptimeMs"),
                lastStartTime = obj.optStringOrNull("lastStartTime")?.let { Instant.parse(it) },
                lastStopTime = obj.optStringOrNull("lastStopTime")?.let { Instant.parse(it) },
                lastError = obj.optJSONObject("lastError")?.let { parseRuntimeError(it) }
                    ?: obj.optStringOrNull("lastError")?.let { RuntimeError(it, it, Instant.now()) },
                exitCode = obj.optIntOrNull("exitCode"),
                version = obj.optStringOrNull("version"),
                lastSuccessfulPoll = obj.optLongOrNull("lastSuccessfulPoll"),
                connSockAvailable = obj.optBoolean("connSockAvailable", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse runtime status", e)
            RuntimeStatus.UNINITIALIZED
        }
    }

    private fun fetchNetworkStatus(): NetworkStatus {
        val json = queryProvider("getNetwork") ?: run {
            controllerAvailable = false
            return NetworkStatus.DISCONNECTED
        }
        controllerAvailable = true

        return try {
            val obj = JSONObject(json)
            NetworkStatus(
                type = parseNetworkType(obj.optString("networkType", "NONE")),
                interfaceName = obj.optStringOrNull("interfaceName"),
                stackedInterfaceName = obj.optStringOrNull("stackedInterfaceName"),
                validated = obj.optBoolean("validated", false),
                dnsServers = obj.optJSONArray("dnsServers")?.toStringList() ?: emptyList(),
                nat64Prefix = obj.optStringOrNull("nat64Prefix"),
                timestampMs = obj.optLong("timestampMs", System.currentTimeMillis()),
                resolverAvailable = obj.optBoolean("resolverAvailable", false),
                resolverContents = obj.optStringOrNull("resolverContents")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse network status", e)
            NetworkStatus.DISCONNECTED
        }
    }

    private fun fetchBootPackageStatus(): BootPackageStatus {
        val json = queryProvider("getBootPackage") ?: run {
            controllerAvailable = false
            return BootPackageStatus.NONE
        }
        controllerAvailable = true

        return try {
            val obj = JSONObject(json)
            BootPackageStatus(
                exists = obj.optBoolean("exists", false),
                valid = obj.optBoolean("valid", false),
                packageVersion = obj.optIntOrNull("packageVersion"),
                bootMode = obj.optStringOrNull("bootMode")?.let { parseBootMode(it) },
                ship = obj.optStringOrNull("ship"),
                parent = obj.optStringOrNull("parent"),
                pierPath = obj.optStringOrNull("pierPath"),
                pillPath = obj.optStringOrNull("pillPath"),
                pierExists = obj.optBoolean("pierExists", false),
                pillExists = obj.optBoolean("pillExists", false),
                keyFileExists = obj.optBoolean("keyFileExists", false),
                validationErrors = obj.optJSONArray("validationErrors")?.toValidationErrors() ?: emptyList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse boot package status", e)
            BootPackageStatus.NONE
        }
    }

    private fun fetchDiagnostics(): DiagnosticsSummary {
        val json = queryProvider("getDiagnostics") ?: run {
            controllerAvailable = false
            return DiagnosticsSummary.EMPTY
        }
        controllerAvailable = true

        return try {
            val obj = JSONObject(json)
            DiagnosticsSummary(
                controllerLogs = obj.optJSONArray("controllerLogs")?.toLogLines() ?: emptyList(),
                launcherLogs = emptyList(),
                recentErrors = obj.optJSONArray("recentErrors")?.toErrorEntries() ?: emptyList(),
                resolverContents = obj.optStringOrNull("resolverContents"),
                networkStateRaw = obj.optStringOrNull("networkStateRaw")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse diagnostics", e)
            DiagnosticsSummary.EMPTY
        }
    }

    private fun fetchHostedApps(): List<HostedApp> {
        val json = queryProvider("getHostedApps") ?: run {
            controllerAvailable = false
            return emptyList()
        }
        controllerAvailable = true

        return try {
            val array = JSONObject(json).optJSONArray("apps") ?: JSONArray()
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                HostedApp(
                    id = obj.optString("id", obj.optString("desk", "")),
                    desk = obj.optString("desk", obj.optString("id", "")),
                    title = obj.optString("title", obj.optString("desk", "Urbit")),
                    info = obj.optString("info", ""),
                    launchMode = obj.optString("launchMode", ""),
                    basePath = obj.optStringOrNull("basePath"),
                    startUrl = obj.optStringOrNull("startUrl"),
                    sourceUrl = obj.optStringOrNull("sourceUrl"),
                    imageUrl = obj.optStringOrNull("imageUrl"),
                    version = obj.optStringOrNull("version"),
                    website = obj.optStringOrNull("website"),
                    availability = obj.optString("availability", "unknown"),
                    androidPackage = obj.optStringOrNull("androidPackage"),
                    pwaManifestUrl = obj.optStringOrNull("pwaManifestUrl"),
                    recommended = obj.optBoolean("recommended", false),
                    hidden = obj.optBoolean("hidden", false),
                    mobileMetadata = obj.optBoolean("mobileMetadata", false)
                )
            }.filterNot { it.hidden }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse hosted apps", e)
            emptyList()
        }
    }

    private fun queryProvider(method: String): String? {
        return try {
            val result: Bundle? = contentResolver.call(BASE_URI, method, null, null)
            result?.getString("json")
        } catch (e: Exception) {
            Log.w(TAG, "Provider query failed for $method: ${e.message}")
            null
        }
    }

    private fun callProviderControl(method: String, requestJson: String? = null): ControlResult {
        return try {
            val extras = Bundle()
            if (requestJson != null) {
                extras.putString("json", requestJson)
            }
            val result: Bundle? = contentResolver.call(BASE_URI, method, null, extras)
            val json = result?.getString("json") ?: return ControlResult.Failed("NO_RESPONSE", "Controller did not respond")
            ProviderControlResultParser.parse(JSONObject(json))
        } catch (e: Exception) {
            Log.w(TAG, "Provider control failed for $method: ${e.message}")
            ControlResult.Failed("PROVIDER_UNAVAILABLE", e.message ?: "Controller unavailable")
        }
    }

    private fun parseRuntimeState(state: String): RuntimeState {
        return when (state.lowercase()) {
            "running" -> RuntimeState.RUNNING
            "stopped" -> RuntimeState.STOPPED
            "starting" -> RuntimeState.STARTING
            "stopping" -> RuntimeState.STOPPING
            "error" -> RuntimeState.ERROR
            "crashed" -> RuntimeState.CRASHED
            else -> RuntimeState.UNINITIALIZED
        }
    }

    private fun parseBootMode(mode: String): BootMode {
        return when (mode.uppercase()) {
            "MOON" -> BootMode.MOON
            "COMET" -> BootMode.COMET
            "PLANET" -> BootMode.PLANET
            "FAKE_TEST" -> BootMode.FAKE_TEST
            else -> BootMode.MOON
        }
    }

    private fun parseNetworkType(type: String): NetworkType {
        return when (type.uppercase()) {
            "WIFI" -> NetworkType.WIFI
            "CELLULAR" -> NetworkType.CELLULAR
            "ETHERNET" -> NetworkType.ETHERNET
            "VPN" -> NetworkType.VPN
            else -> NetworkType.NONE
        }
    }

    private fun parseRuntimeError(obj: JSONObject): RuntimeError {
        return RuntimeError(
            code = obj.optString("code", "UNKNOWN"),
            message = obj.optString("message", ""),
            timestamp = obj.optStringOrNull("timestamp")?.let { Instant.parse(it) } ?: Instant.now()
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        return if (isNull(key) || !has(key)) null else optInt(key)
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        return if (isNull(key) || !has(key)) null else optLong(key)
    }

    private fun JSONArray.toStringList(): List<String> {
        return (0 until length()).map { getString(it) }
    }

    private fun JSONArray.toValidationErrors(): List<ValidationError> {
        return (0 until length()).map { i ->
            val obj = getJSONObject(i)
            ValidationError(
                field = obj.optString("field", ""),
                code = obj.optString("code", ""),
                message = obj.optString("message", "")
            )
        }
    }

    private fun JSONArray.toLogLines(): List<LogLine> {
        return (0 until length()).map { i ->
            val obj = getJSONObject(i)
            LogLine(
                timestamp = obj.optStringOrNull("timestamp")?.let { Instant.parse(it) } ?: Instant.now(),
                level = parseLogLevel(obj.optString("level", "INFO")),
                message = obj.optString("message", "")
            )
        }
    }

    private fun JSONArray.toErrorEntries(): List<ErrorEntry> {
        return (0 until length()).map { i ->
            val obj = getJSONObject(i)
            ErrorEntry(
                source = obj.optString("source", ""),
                timestamp = obj.optStringOrNull("timestamp")?.let { Instant.parse(it) } ?: Instant.now(),
                message = obj.optString("message", "")
            )
        }
    }

    private fun parseLogLevel(level: String): LogLevel {
        return when (level.uppercase()) {
            "ERROR" -> LogLevel.ERROR
            "WARN" -> LogLevel.WARN
            else -> LogLevel.INFO
        }
    }
}
