package io.nativeplanet.launcher.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.theme.*
import io.nativeplanet.launcher.ui.components.SigilView

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
            text = "this is you.",
            style = NPType.displaySm.copy(fontStyle = FontStyle.Italic),
            color = colors.foregroundDim,
            modifier = Modifier.alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxl))

        // Sigil (156dp)
        SigilView(
            patp = shipName,
            modifier = Modifier
                .size(156.dp)
                .scale(animatedScale)
                .alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = shipName,
            style = NPType.patpLg,
            color = colors.foreground,
            modifier = Modifier.alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(NPSpacing.sm))

        Text(
            text = if (parentName != null) "mobile moon · under $parentName" else "comet · temporary identity",
            style = NPType.bodySm,
            color = colors.foregroundDim,
            modifier = Modifier.alpha(animatedAlpha)
        )

        Spacer(modifier = Modifier.height(NPSpacing.xxxl))

        // Continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(colors.foreground)
                .clickable(onClick = onContinue)
                .padding(vertical = NPSpacing.lg)
                .alpha(animatedAlpha),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "continue",
                style = NPType.bodyLg,
                color = colors.background
            )
        }
    }
}
