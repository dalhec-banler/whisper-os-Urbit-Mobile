package io.nativeplanet.launcher.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SourceSerif = FontFamily.Serif

val Inter = FontFamily.SansSerif

val JetBrainsMono = FontFamily.Monospace

object NPType {
    // Display (Source Serif)
    val displayXl = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Light,
        fontSize = 110.sp,
        lineHeight = (110 * 0.85).sp,
        letterSpacing = (-2).sp
    )

    val displayLg = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = (56 * 0.90).sp,
        letterSpacing = (-1.5).sp
    )

    val displayMd = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        lineHeight = (38 * 1.05).sp,
        letterSpacing = (-1).sp
    )

    val displaySm = TextStyle(
        fontFamily = SourceSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = (28 * 1.10).sp,
        letterSpacing = (-0.5).sp
    )

    // Body (Inter)
    val bodyLg = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = (16 * 1.50).sp
    )

    val bodyMd = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = (14 * 1.50).sp
    )

    val bodySm = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = (13 * 1.55).sp
    )

    // Caption/Micro (JetBrains Mono for patps)
    val caption = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = (12 * 1.40).sp
    )

    val micro = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = (11 * 1.50).sp,
        letterSpacing = 1.5.sp
    )

    val nano = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = (10 * 1.30).sp
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
