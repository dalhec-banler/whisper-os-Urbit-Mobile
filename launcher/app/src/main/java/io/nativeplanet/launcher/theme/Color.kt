package io.nativeplanet.launcher.theme

import androidx.compose.ui.graphics.Color

object NPColors {
    // Whisper OS source-of-truth surfaces.
    val paper = Color(0xFFF4F1EC)
    val ink = Color(0xFF1A1916)
    val deep = Color(0xFF0E0D0C)

    // Derived tints.
    val inkDim = ink.copy(alpha = 0.55f)
    val inkFaint = ink.copy(alpha = 0.30f)
    val inkHair = ink.copy(alpha = 0.10f)
    val paperDim = paper.copy(alpha = 0.55f)
    val paperFaint = paper.copy(alpha = 0.30f)
    val paperHair = paper.copy(alpha = 0.10f)

    // Backwards-compatible names used by existing screens.
    val bgWarmBlack = deep
    val bgWarmBlack2 = ink
    val bgCoolDark = deep
    val bgStream = ink
    val bgLedger = ink
    val bgPaper = paper
    val bgPaper2 = paper
    val bgPaper3 = inkHair
    val fgCream = paper
    val fgBone = paper
    val fgInk = ink
    val fgDim = paperDim
    val fgDimInk = inkDim
    val fgFaint = paperFaint

    // Accents.
    val accentAmber = Color(0xFFF3A712)
    val accentAmberSoft = Color(0xFFD6A35C)
    val mint = Color(0xFF7FC7A3)
    val coral = Color(0xFFD97757)
    val accentCoral = coral
    val accentSage = Color(0xFF9BB380)
    val accentSky = Color(0xFF7DA6C9)
    val accentLavender = Color(0xFFA18CD1)
    val accentSlate = Color(0xFF9AA4B3)
    val accentStone = Color(0xFFA89888)
    val accentClay = Color(0xFFC89678)

    // Errors/destructive states use coral, never pure red.
    val error = coral

    // Provenance worlds.
    val worldUrbit = accentAmberSoft
    val worldPlay = Color(0xFF8FB39A)
    val worldWeb = accentSlate
    val worldSystem = accentStone
    val worldDev = accentClay

    // Hairlines.
    val hairlineDark = paperHair
    val hairlineDarkEmphasis = paper.copy(alpha = 0.14f)
    val hairlinePaper = inkHair
}
