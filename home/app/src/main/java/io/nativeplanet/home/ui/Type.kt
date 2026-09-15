package io.nativeplanet.home.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import io.nativeplanet.home.R

/** The whole system: one ground, one ink, an opacity ladder, two faces. */
object W {
    val Ink = Color(0xFF0E0D0C)
    val Paper = Color(0xFFF4F1EC)
    val Paper90 = Paper.copy(alpha = 0.90f)
    val Paper60 = Paper.copy(alpha = 0.60f)
    val Paper40 = Paper.copy(alpha = 0.40f)
    val Hair = Paper.copy(alpha = 0.16f)
    val Link = Color(0xFF7DA6C9)

    val Serif = FontFamily(Font(R.font.source_serif, FontWeight.Light), Font(R.font.source_serif, FontWeight.Normal), Font(R.font.source_serif, FontWeight.SemiBold))
    val Mono = FontFamily(Font(R.font.jetbrains_mono, FontWeight.Normal), Font(R.font.jetbrains_mono, FontWeight.Medium))
}
