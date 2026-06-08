package io.nativeplanet.launcher.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class NPColorScheme(
    val background: Color,
    val backgroundSecondary: Color,
    val foreground: Color,
    val foregroundDim: Color,
    val foregroundFaint: Color,
    val accent: Color,
    val accentSoft: Color,
    val error: Color,
    val hairline: Color
)

val LocalNPColors = staticCompositionLocalOf {
    NPColorScheme(
        background = NPColors.bgWarmBlack,
        backgroundSecondary = NPColors.bgWarmBlack2,
        foreground = NPColors.fgCream,
        foregroundDim = NPColors.fgDim,
        foregroundFaint = NPColors.fgFaint,
        accent = NPColors.accentAmber,
        accentSoft = NPColors.accentAmberSoft,
        error = NPColors.error,
        hairline = NPColors.hairlineDark
    )
}

@Composable
fun NativePlanetTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        NPColorScheme(
            background = NPColors.bgWarmBlack,
            backgroundSecondary = NPColors.bgWarmBlack2,
            foreground = NPColors.fgCream,
            foregroundDim = NPColors.fgDim,
            foregroundFaint = NPColors.fgFaint,
            accent = NPColors.accentAmber,
            accentSoft = NPColors.accentAmberSoft,
            error = NPColors.error,
            hairline = NPColors.hairlineDark
        )
    } else {
        NPColorScheme(
            background = NPColors.bgPaper,
            backgroundSecondary = NPColors.bgPaper2,
            foreground = NPColors.fgInk,
            foregroundDim = NPColors.fgDimInk,
            foregroundFaint = NPColors.fgDimInk,
            accent = NPColors.accentAmber,
            accentSoft = NPColors.accentAmberSoft,
            error = NPColors.error,
            hairline = NPColors.hairlinePaper
        )
    }

    CompositionLocalProvider(LocalNPColors provides colors) {
        content()
    }
}

object NativePlanetTheme {
    val colors: NPColorScheme
        @Composable
        get() = LocalNPColors.current
}
