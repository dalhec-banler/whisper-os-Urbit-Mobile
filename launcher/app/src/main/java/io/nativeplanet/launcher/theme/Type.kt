package io.nativeplanet.launcher.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.nativeplanet.launcher.R

val SourceSerif = FontFamily(
    Font(R.font.source_serif_variable, FontWeight.Light),
    Font(R.font.source_serif_variable, FontWeight.Normal),
    Font(R.font.source_serif_variable, FontWeight.Medium),
    Font(R.font.source_serif_variable, FontWeight.SemiBold)
)

val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_variable, FontWeight.Normal),
    Font(R.font.jetbrains_mono_variable, FontWeight.Medium)
)

object NPType {
    // Display (Source Serif 4 fallback)
    val displayXl = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Light,
        fontSize = 88.sp,
        lineHeight = (88 * 0.85).sp,
        letterSpacing = 0.sp
    )

    val displayLg = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = (44 * 0.90).sp,
        letterSpacing = 0.sp
    )

    val displayMd = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 32.sp,
        lineHeight = (32 * 1.10).sp,
        letterSpacing = 0.sp
    )

    val displaySm = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = (28 * 1.10).sp,
        letterSpacing = 0.sp
    )

    val headline = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = (22 * 1.20).sp,
        letterSpacing = 0.sp
    )

    // Body (Inter)
    val bodyLg = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.40).sp,
        letterSpacing = 0.sp
    )

    val bodyMd = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.40).sp,
        letterSpacing = 0.sp
    )

    val bodySm = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.40).sp,
        letterSpacing = 0.sp
    )

    // Caption/Micro (JetBrains Mono for patps)
    val caption = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = (11 * 1.40).sp,
        letterSpacing = 0.sp
    )

    val micro = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = (10 * 1.40).sp,
        letterSpacing = 0.sp
    )

    val nano = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = (9 * 1.30).sp,
        letterSpacing = 0.sp
    )

    val sectionLabel = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = (10 * 1.40).sp,
        letterSpacing = 2.sp
    )

    // @p always in mono
    val patp = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.40).sp
    )

    val patpLg = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = (18 * 1.40).sp
    )
}
