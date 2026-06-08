package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.theme.*
import kotlinx.coroutines.delay

@Composable
fun CometScreen(
    onBootComplete: (cometName: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    var bootState by remember { mutableStateOf(CometBootState.READY) }
    var statusMessage by remember { mutableStateOf("") }

    LaunchedEffect(bootState) {
        if (bootState == CometBootState.BOOTING) {
            statusMessage = "initializing comet…"
            delay(1500)
            statusMessage = "generating identity…"
            delay(1500)
            statusMessage = "booting urbit…"
            delay(2000)
            bootState = CometBootState.COMPLETE
            onBootComplete("~sampel-palnet-marzod-marzod--marzod-marzod-marzod-marzod")
        }
    }

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
            text = "COMET",
            style = NPType.micro,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "Throwaway identity.",
            style = NPType.displaySm,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        Text(
            text = "A comet is a temporary Urbit identity. It boots instantly with no sponsor required, but has limited network access and cannot be recovered if lost.",
            style = NPType.bodySm,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.sm))

        Text(
            text = "Comets are ideal for testing or development. For a permanent identity, pair with a planet instead.",
            style = NPType.bodySm,
            color = colors.foregroundFaint
        )

        Spacer(modifier = Modifier.weight(1f))

        when (bootState) {
            CometBootState.READY -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.foreground)
                        .clickable { bootState = CometBootState.BOOTING }
                        .padding(vertical = NPSpacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Boot comet",
                        style = NPType.bodyLg,
                        color = colors.background
                    )
                }
            }

            CometBootState.BOOTING -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = colors.accent,
                        strokeWidth = 2.dp
                    )

                    Spacer(modifier = Modifier.height(NPSpacing.md))

                    Text(
                        text = statusMessage,
                        style = NPType.caption,
                        color = colors.foregroundDim
                    )
                }
            }

            CometBootState.COMPLETE -> {
                Text(
                    text = "booted!",
                    style = NPType.caption,
                    color = colors.accent,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        if (bootState == CometBootState.READY) {
            Text(
                text = "← back",
                style = NPType.bodySm,
                color = colors.foregroundFaint,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(NPSpacing.sm)
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.lg))
    }
}

private enum class CometBootState {
    READY,
    BOOTING,
    COMPLETE
}
