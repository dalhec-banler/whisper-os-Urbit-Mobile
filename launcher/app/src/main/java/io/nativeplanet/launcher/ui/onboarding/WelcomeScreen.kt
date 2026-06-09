package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.NPButtonStyle

@Composable
fun WelcomeScreen(
    onPairWithPlanet: () -> Unit,
    onImportShip: () -> Unit,
    onStartComet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(NPSpacing.screenGutter)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome.",
            style = NPType.displayMd,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.sm))

        Text(
            text = "First — your identity.",
            style = NPType.displaySm.copy(fontStyle = FontStyle.Italic),
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        NPButton(
            text = "Pair with planet",
            onClick = onPairWithPlanet,
            style = NPButtonStyle.FILLED
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        NPButton(
            text = "Use moon key",
            onClick = onImportShip,
            style = NPButtonStyle.GHOST
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        // Comet link (faint)
        Text(
            text = "or start as a comet",
            style = NPType.bodySm,
            color = colors.foregroundFaint,
            modifier = Modifier
                .clickable(onClick = onStartComet)
                .padding(vertical = NPSpacing.sm)
        )
    }
}
