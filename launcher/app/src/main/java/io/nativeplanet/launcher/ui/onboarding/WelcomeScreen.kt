package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.GlyphKind
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.NPButtonStyle
import io.nativeplanet.launcher.ui.components.SigilGlyph

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
            text = "WHISPER OS · WELCOME · 1 OF 4",
            style = NPType.caption,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "Whisper OS.",
            style = NPType.displayLg,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.sm))

        Text(
            text = "Pair this phone with your planet.",
            style = NPType.headline.copy(fontStyle = FontStyle.Italic),
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        Text(
            text = "Your planet creates a satellite for this device. The satellite signs from here; your planet stays the root.",
            style = NPType.bodySm,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        NPButton(
            text = "pair with your planet",
            onClick = onPairWithPlanet,
            style = NPButtonStyle.FILLED
        )

        Spacer(modifier = Modifier.height(NPSpacing.md))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onImportShip)
                .padding(vertical = NPSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SigilGlyph(
                glyph = GlyphKind.Files,
                size = 28.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "import a satellite",
                style = NPType.bodySm,
                color = colors.foregroundDim
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.lg))

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
