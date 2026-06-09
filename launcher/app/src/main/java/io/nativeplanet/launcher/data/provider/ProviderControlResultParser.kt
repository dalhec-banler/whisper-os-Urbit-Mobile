package io.nativeplanet.launcher.data.provider

import io.nativeplanet.launcher.domain.model.ControlResult
import org.json.JSONObject

internal object ProviderControlResultParser {
    fun parse(obj: JSONObject): ControlResult {
        val accepted = obj.optBoolean("accepted", false)
        val code = obj.optString("code", if (accepted) "OK" else "UNKNOWN")
        val message = obj.optStringOrNull("message") ?: code
        return if (accepted) {
            val bootPackage = obj.optJSONObject("bootPackage")
            ControlResult.Success(
                shipName = bootPackage?.optStringOrNull("ship"),
                parentName = bootPackage?.optStringOrNull("parent")
            )
        } else {
            ControlResult.Failed(code, message)
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
    }
}
