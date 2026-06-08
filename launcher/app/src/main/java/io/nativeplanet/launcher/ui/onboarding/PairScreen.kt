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
fun PairScreen(
    onPairingComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    var isPairing by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000)
        isPairing = false
        onPairingComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(NPSpacing.screenGutter)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        Text(
            text = "PAIR WITH PLANET",
            style = NPType.micro,
            color = colors.foregroundDim
        )

        Spacer(modifier = Modifier.weight(1f))

        // QR placeholder
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.foreground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "QR",
                style = NPType.displayMd,
                color = colors.background
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "Open Urbit on your laptop and approve this device.",
            style = NPType.bodySm,
            color = colors.foregroundDim,
            modifier = Modifier.padding(horizontal = NPSpacing.xl)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Status footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isPairing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colors.accent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(NPSpacing.sm))
            }
            Text(
                text = if (isPairing) "waiting for pairing…" else "paired!",
                style = NPType.caption,
                color = if (isPairing) colors.foregroundDim else colors.accent
            )
        }

        Spacer(modifier = Modifier.height(NPSpacing.xl))

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
