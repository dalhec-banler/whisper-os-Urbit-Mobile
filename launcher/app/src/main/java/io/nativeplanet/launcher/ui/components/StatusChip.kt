package io.nativeplanet.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.nativeplanet.launcher.domain.model.RuntimeState
import io.nativeplanet.launcher.theme.NPColors
import io.nativeplanet.launcher.theme.NPSpacing
import io.nativeplanet.launcher.theme.NPType

@Composable
fun StatusChip(
    state: RuntimeState,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, label) = when (state) {
        RuntimeState.RUNNING -> Triple(NPColors.accentSage, NPColors.fgInk, "RUNNING")
        RuntimeState.STOPPED -> Triple(NPColors.accentStone, NPColors.fgCream, "STOPPED")
        RuntimeState.STARTING -> Triple(NPColors.accentAmber, NPColors.fgInk, "STARTING")
        RuntimeState.STOPPING -> Triple(NPColors.accentAmber, NPColors.fgInk, "STOPPING")
        RuntimeState.ERROR -> Triple(NPColors.error, NPColors.fgCream, "ERROR")
        RuntimeState.CRASHED -> Triple(NPColors.error, NPColors.fgCream, "CRASHED")
        RuntimeState.UNINITIALIZED -> Triple(NPColors.accentSlate, NPColors.fgCream, "NOT CONFIGURED")
    }

    Text(
        text = label,
        style = NPType.micro,
        color = fgColor,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = NPSpacing.sm, vertical = NPSpacing.xs)
    )
}
