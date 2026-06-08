package io.nativeplanet.launcher.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.nativeplanet.launcher.theme.NPType
import io.nativeplanet.launcher.theme.NativePlanetTheme

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    Text(
        text = text.uppercase(),
        style = NPType.micro,
        color = colors.foregroundDim,
        modifier = modifier
    )
}
