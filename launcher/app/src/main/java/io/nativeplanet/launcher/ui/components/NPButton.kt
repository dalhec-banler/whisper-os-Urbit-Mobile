package io.nativeplanet.launcher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.theme.NPMotion
import io.nativeplanet.launcher.theme.NPSpacing
import io.nativeplanet.launcher.theme.NPType
import io.nativeplanet.launcher.theme.NativePlanetTheme

enum class NPButtonStyle {
    FILLED,
    SECONDARY,
    GHOST
}

@Composable
fun NPButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: NPButtonStyle = NPButtonStyle.FILLED,
    fillWidth: Boolean = true
) {
    val colors = NativePlanetTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(durationMillis = NPMotion.durationMicro),
        label = "buttonScale"
    )

    val (bgColor, fgColor) = when {
        !enabled -> Pair(colors.backgroundSecondary, colors.foregroundFaint)
        style == NPButtonStyle.FILLED -> Pair(colors.foreground, colors.background)
        style == NPButtonStyle.SECONDARY -> Pair(colors.backgroundSecondary, colors.foreground)
        else -> Pair(colors.background, colors.foregroundDim)
    }

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = NPSpacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = NPType.bodyLg,
            color = fgColor
        )
    }
}

@Composable
fun NPButtonSmall(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: NPButtonStyle = NPButtonStyle.FILLED
) {
    val colors = NativePlanetTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.98f else 1f,
        animationSpec = tween(durationMillis = NPMotion.durationMicro),
        label = "buttonScale"
    )

    val (bgColor, fgColor) = when {
        !enabled -> Pair(colors.backgroundSecondary, colors.foregroundFaint)
        style == NPButtonStyle.FILLED -> Pair(colors.foreground, colors.background)
        style == NPButtonStyle.SECONDARY -> Pair(colors.backgroundSecondary, colors.foreground)
        else -> Pair(colors.background, colors.foregroundDim)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = NPSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = NPType.bodySm,
            color = fgColor
        )
    }
}
