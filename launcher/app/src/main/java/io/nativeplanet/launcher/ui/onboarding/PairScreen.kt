package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.domain.model.ControlResult
import io.nativeplanet.launcher.theme.*
import kotlinx.coroutines.launch

@Composable
fun PairScreen(
    onPairWithPlanet: suspend (hostUrl: String, accessCode: String) -> ControlResult,
    onImportManually: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors
    val scope = rememberCoroutineScope()

    var hostUrl by remember { mutableStateOf("") }
    var accessCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
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
            text = "Connect your planet.",
            style = NPType.displaySm,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        PairTextField(
            value = hostUrl,
            onValueChange = { hostUrl = it },
            label = "Hosting URL",
            placeholder = "https://example.tlon.network"
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        PairTextField(
            value = accessCode,
            onValueChange = { accessCode = it },
            label = "+code",
            placeholder = "lidlut-tabwed-pillex-ridrup",
            isSecret = true
        )

        statusMessage?.let { message ->
            Spacer(modifier = Modifier.height(NPSpacing.md))
            Text(
                text = message,
                style = NPType.caption,
                color = colors.foregroundDim
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(if (canConnect) colors.foreground else colors.backgroundSecondary)
                .clickable(enabled = canConnect) {
                    val submittedHost = hostUrl.trim()
                    val submittedCode = accessCode.trim()
                    accessCode = ""
                    statusMessage = "connecting..."
                    isConnecting = true

                    scope.launch {
                        val result = onPairWithPlanet(submittedHost, submittedCode)
                        isConnecting = false
                        statusMessage = when (result) {
                            is ControlResult.Success -> "connected"
                            is ControlResult.AlreadyInState -> "already ${result.state.name.lowercase()}"
                            is ControlResult.Failed -> "${result.code}: ${result.message}"
                        }
                    }
                }
                .padding(vertical = NPSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isConnecting) "Connecting" else "Connect",
                style = NPType.bodyLg,
                color = if (canConnect) colors.background else colors.foregroundFaint
            )
        }

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

@Composable
private fun PairTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isSecret: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = {
            Text(text = label, style = NPType.caption)
        },
        placeholder = {
            Text(text = placeholder, style = NPType.bodySm)
        },
        textStyle = NPType.bodySm,
        visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true
    )
}
