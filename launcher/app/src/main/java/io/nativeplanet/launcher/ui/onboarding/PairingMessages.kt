package io.nativeplanet.launcher.ui.onboarding

import io.nativeplanet.launcher.domain.model.ControlResult

internal object PairingMessages {
    fun failure(result: ControlResult.Failed): String {
        return when (result.code) {
            "INVALID_HOST_URL" -> "Enter a valid HTTPS hosting URL."
            "MISSING_ACCESS_CODE" -> "Enter your +code."
            "PARENT_AUTH_FAILED" -> "Login failed. Check the hosting URL and +code."
            "PARENT_NETWORK_FAILED" -> "Could not reach the planet hosting URL."
            "PARENT_SERVICE_UNAVAILABLE",
            "PARENT_PROTOCOL_UNSUPPORTED" -> "Your planet is reachable, but Artemis is not ready for mobile pairing yet. Use a moon key for now."
            "PARENT_MOON_CREATE_FAILED" -> "Artemis did not accept the mobile moon request."
            "PARENT_MOON_CREATE_TIMEOUT" -> "Artemis did not return a new mobile moon in time."
            else -> result.message.ifBlank { "Pairing failed." }
        }
    }
}
