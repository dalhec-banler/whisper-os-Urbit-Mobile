package io.nativeplanet.launcher.data.provider

import io.nativeplanet.launcher.domain.model.ControlResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderControlResultParserTest {
    @Test
    fun acceptedResponseIncludesProvisionedShipAndParent() {
        val result = ProviderControlResultParser.parse(
            JSONObject(
                """
                {
                  "accepted": true,
                  "code": "OK",
                  "bootPackage": {
                    "ship": "milweg-dapseg-palrum-roclur",
                    "parent": "~palrum-roclur"
                  }
                }
                """.trimIndent()
            )
        )

        assertTrue(result is ControlResult.Success)
        result as ControlResult.Success
        assertEquals("milweg-dapseg-palrum-roclur", result.shipName)
        assertEquals("~palrum-roclur", result.parentName)
    }

    @Test
    fun acceptedResponseCanOmitBootPackage() {
        val result = ProviderControlResultParser.parse(
            JSONObject("""{"accepted":true,"code":"OK"}""")
        )

        assertTrue(result is ControlResult.Success)
        result as ControlResult.Success
        assertNull(result.shipName)
        assertNull(result.parentName)
    }

    @Test
    fun rejectedResponseUsesStructuredCodeAndMessage() {
        val result = ProviderControlResultParser.parse(
            JSONObject(
                """
                {
                  "accepted": false,
                  "code": "PARENT_SERVICE_UNAVAILABLE",
                  "message": "Artemis is not installed."
                }
                """.trimIndent()
            )
        )

        assertTrue(result is ControlResult.Failed)
        result as ControlResult.Failed
        assertEquals("PARENT_SERVICE_UNAVAILABLE", result.code)
        assertEquals("Artemis is not installed.", result.message)
    }
}
