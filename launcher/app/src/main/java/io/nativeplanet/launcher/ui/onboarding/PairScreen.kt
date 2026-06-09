package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.nativeplanet.launcher.domain.model.ControlResult
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.NPTextField
import kotlinx.coroutines.launch

@Composable
fun PairScreen(
    onPairWithPlanet: suspend (hostUrl: String, accessCode: String) -> ControlResult,
    onPairComplete: (shipName: String, parentName: String?) -> Unit,
    onImportManually: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors
    val scope = rememberCoroutineScope()

    var hostUrl by remember { mutableStateOf("") }
    var accessCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    val canConnect = hostUrl.trim().isNotEmpty() && accessCode.trim().isNotEmpty() && !isConnecting

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(NPSpacing.screenGutter)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        Text(
            text = "PAIR WITH PLANET",
            style = NPType.micro,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "Create your phone moon.",
            style = NPType.displaySm,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        Text(
            text = "Use your parent ship's hosting URL and +code. Artemis on the parent will create a mobile moon for this phone.",
            style = NPType.bodySm,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        NPTextField(
            value = hostUrl,
            onValueChange = { hostUrl = it },
            label = "Hosting URL",
            placeholder = "https://example.tlon.network"
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        NPTextField(
            value = accessCode,
            onValueChange = { accessCode = it },
            label = "+code",
            placeholder = "xxxx-xxxx-xxxx-xxxx",
            isSecret = true
        )

        statusMessage?.let { message ->
            Spacer(modifier = Modifier.height(NPSpacing.md))
            Text(
                text = message,
                style = NPType.caption,
                color = if (statusIsError) colors.error else colors.foregroundDim
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        NPButton(
            text = if (isConnecting) "Connecting" else "Connect",
            enabled = canConnect,
            onClick = {
                val submittedHost = hostUrl.trim()
                val submittedCode = accessCode.trim()
                accessCode = ""
                statusMessage = "connecting..."
                statusIsError = false
                isConnecting = true

                scope.launch {
                    val result = onPairWithPlanet(submittedHost, submittedCode)
                    isConnecting = false
                    statusMessage = when (result) {
                        is ControlResult.Success -> {
                            statusIsError = false
                            val pairedShip = result.shipName
                            if (pairedShip != null) {
                                onPairComplete(pairedShip, result.parentName)
                                "connected"
                            } else {
                                "connected"
                            }
                        }
                        is ControlResult.AlreadyInState -> {
                            statusIsError = false
                            "already ${result.state.name.lowercase()}"
                        }
                        is ControlResult.Failed -> {
                            statusIsError = true
                            friendlyPairingError(result)
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        Text(
            text = "Use moon key instead",
            style = NPType.bodySm,
            color = colors.foregroundFaint,
            modifier = Modifier
                .clickable(onClick = onImportManually)
                .padding(NPSpacing.sm)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Back
        Text(
            text = "← back",
            style = NPType.bodySm,
            color = colors.foregroundFaint,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(NPSpacing.sm)
        )

        Spacer(modifier = Modifier.height(NPSpacing.lg))
    }
}

private fun friendlyPairingError(result: ControlResult.Failed): String {
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
