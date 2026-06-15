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
            text = "PAIR · WAITING · 2 OF 4",
            style = NPType.micro,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "Open your planet.",
            style = NPType.displaySm,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        Text(
            text = "Your planet creates a satellite for this device. Enter the hosting URL and access code shown by your planet.",
            style = NPType.bodySm,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        NPTextField(
            value = hostUrl,
            onValueChange = { hostUrl = it },
            label = "Planet hosting URL",
            placeholder = "https://your-planet.example"
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        NPTextField(
            value = accessCode,
            onValueChange = { accessCode = it },
            label = "Access code",
            placeholder = "word-word-word-word",
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
            text = if (isConnecting) "waiting for your planet" else "pair this phone",
            enabled = canConnect,
            onClick = {
                val submittedHost = hostUrl.trim()
                val submittedCode = accessCode.trim()
                accessCode = ""
                statusMessage = "waiting for your planet…"
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
                                "paired"
                            } else {
                                "paired"
                            }
                        }
                        is ControlResult.AlreadyInState -> {
                            statusIsError = false
                            "already ${result.state.name.lowercase()}"
                        }
                        is ControlResult.Failed -> {
                            statusIsError = true
                            PairingMessages.failure(result)
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        Text(
            text = "import a satellite instead",
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
