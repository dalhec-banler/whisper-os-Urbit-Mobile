package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.theme.*

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

        // Pair with planet button (filled)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.foreground)
                .clickable(onClick = onPairWithPlanet)
                .padding(vertical = NPSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pair with planet",
                style = NPType.bodyLg,
                color = colors.background
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.md))

        // Import existing ship button (outlined)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.background)
                .clickable(onClick = onImportShip)
                .padding(vertical = NPSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Import existing ship",
                style = NPType.bodyLg,
                color = colors.foreground
            )
        }

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
