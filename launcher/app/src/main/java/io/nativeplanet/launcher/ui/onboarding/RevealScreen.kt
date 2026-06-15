package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.Sigil

@Composable
fun RevealScreen(
    shipName: String,
    parentName: String?,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    var animationStarted by remember { mutableStateOf(false) }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(
            durationMillis = NPMotion.durationRitual,
            easing = NPMotion.standardEasing
        ),
        label = "alpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.85f,
        animationSpec = tween(
            durationMillis = NPMotion.durationRitual,
            easing = NPMotion.standardEasing
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        animationStarted = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(NPSpacing.screenGutter)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PAIRED · 3 OF 4",
            style = NPType.caption,
            color = colors.foregroundDim,
            modifier = Modifier.alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = if (parentName != null) "This is your satellite." else "This is your temporary identity.",
            style = NPType.headline.copy(fontStyle = FontStyle.Italic),
            color = colors.foregroundDim,
            modifier = Modifier.alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxl))

        PairedLockup(
            satellite = shipName,
            planet = parentName,
            modifier = Modifier
                .scale(animatedScale)
                .alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = if (parentName != null) {
                "satellite signs from this phone; your planet stays the root."
            } else {
                "temporary identity for testing and recovery."
            },
            style = NPType.bodySm,
            color = colors.foregroundDim,
            modifier = Modifier.alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        NPButton(
            text = "continue",
            onClick = onContinue,
            modifier = Modifier.alpha(animatedAlpha)
        )
    }
}

@Composable
private fun PairedLockup(
    satellite: String,
    planet: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IdentityMark(
            patp = satellite,
            label = "SATELLITE"
        )

        if (planet != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .width(34.dp)
                    .height(1.dp)
                    .background(NPColors.paperFaint)
            )

            IdentityMark(
                patp = planet,
                label = "YOUR PLANET"
            )
        }
    }
}

@Composable
private fun IdentityMark(
    patp: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Sigil(patp = patp, size = 120.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = patp,
            style = NPType.caption,
            color = NPColors.paper,
            maxLines = 1
        )
        Text(
            text = label,
            style = NPType.nano,
            color = NPColors.paperDim,
            maxLines = 1
        )
    }
}
