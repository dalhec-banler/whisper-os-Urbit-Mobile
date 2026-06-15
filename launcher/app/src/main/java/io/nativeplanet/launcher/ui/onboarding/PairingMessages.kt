package io.nativeplanet.launcher.ui.onboarding

import io.nativeplanet.launcher.domain.model.ControlResult

internal object PairingMessages {
    fun failure(result: ControlResult.Failed): String {
        return when (result.code) {
            "INVALID_HOST_URL" -> "Enter a valid HTTPS hosting URL."
            "MISSING_ACCESS_CODE" -> "Enter the access code from your planet."
            "PARENT_AUTH_FAILED" -> "Login failed. Check the hosting URL and access code."
            "PARENT_NETWORK_FAILED" -> "Could not reach the planet hosting URL."
            "PARENT_SERVICE_UNAVAILABLE",
            "PARENT_PROTOCOL_UNSUPPORTED" -> "Your planet is reachable, but Artemis is not ready for mobile pairing yet. Import a satellite for now."
            "PARENT_MOON_CREATE_FAILED" -> "Artemis did not accept the satellite request."
            "PARENT_MOON_CREATE_TIMEOUT" -> "Artemis did not return a satellite in time."
            else -> result.message.ifBlank { "Pairing failed." }
        }
    }
}
