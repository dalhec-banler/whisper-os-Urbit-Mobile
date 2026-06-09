package io.nativeplanet.launcher.ui.onboarding

import io.nativeplanet.launcher.domain.model.ControlResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PairingMessagesTest {
    @Test
    fun artemisUnavailableUsesProductLanguage() {
        val message = PairingMessages.failure(
            ControlResult.Failed(
                code = "PARENT_PROTOCOL_UNSUPPORTED",
                message = "raw backend protocol message"
            )
        )

        assertEquals(
            "Your planet is reachable, but Artemis is not ready for mobile pairing yet. Use a moon key for now.",
            message
        )
        assertFalse(message.contains("PARENT_"))
    }

    @Test
    fun authFailureDoesNotExposeBackendCode() {
        val message = PairingMessages.failure(
            ControlResult.Failed(
                code = "PARENT_AUTH_FAILED",
                message = "auth failed"
            )
        )

        assertEquals("Login failed. Check the hosting URL and +code.", message)
        assertFalse(message.contains("PARENT_"))
    }

    @Test
    fun unknownFailureUsesControllerMessage() {
        val message = PairingMessages.failure(
            ControlResult.Failed(
                code = "SOMETHING_NEW",
                message = "Something changed upstream."
            )
        )

        assertEquals("Something changed upstream.", message)
    }

    @Test
    fun emptyUnknownFailureFallsBack() {
        val message = PairingMessages.failure(
            ControlResult.Failed(
                code = "SOMETHING_NEW",
                message = ""
            )
        )

        assertEquals("Pairing failed.", message)
    }
}
