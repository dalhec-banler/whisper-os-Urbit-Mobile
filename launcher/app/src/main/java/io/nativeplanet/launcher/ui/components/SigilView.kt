package io.nativeplanet.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.nativeplanet.launcher.theme.NPColors
import kotlin.math.abs

private val PALETTES = listOf(
    Pair(Color(0xFF1A1916), Color(0xFFF3A712)),  // warm-black / amber
    Pair(Color(0xFF1A1916), Color(0xFFD6A35C)),  // warm-black / amber-soft
    Pair(Color(0xFF1A1916), Color(0xFFD97757)),  // warm-black / coral
    Pair(Color(0xFF1A1916), Color(0xFF8FB39A)),  // warm-black / sage
    Pair(Color(0xFF1A1916), Color(0xFF7DA6C9)),  // warm-black / sky
    Pair(Color(0xFF1A1916), Color(0xFFA18CD1)),  // warm-black / lavender
    Pair(Color(0xFF1A1916), Color(0xFF9AA4B3)),  // warm-black / slate
    Pair(Color(0xFF1A1916), Color(0xFFA89888)),  // warm-black / stone
)

@Composable
fun SigilView(
    patp: String,
    modifier: Modifier = Modifier
) {
    val hash = remember(patp) { patpHash(patp) }
    val paletteIndex = remember(hash) { abs(hash) % PALETTES.size }
    val palette = PALETTES[paletteIndex]

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSigil(hash, palette.first, palette.second)
        }
    }
}

private fun patpHash(patp: String): Int {
    var hash = 0
    for (c in patp) {
        hash = 31 * hash + c.code
    }
    return hash
}

private fun DrawScope.drawSigil(hash: Int, bgColor: Color, fgColor: Color) {
    val w = size.width
    val h = size.height

    drawRect(color = bgColor, size = size)

    val cellSize = w / 4
    val bits = abs(hash)

    for (row in 0 until 4) {
        for (col in 0 until 2) {
            val bitIndex = row * 2 + col
            if ((bits shr bitIndex) and 1 == 1) {
                val x = col * cellSize + cellSize / 2
                val mirrorX = w - x - cellSize
                val y = row * cellSize

                drawRect(
                    color = fgColor,
                    topLeft = Offset(x, y),
                    size = Size(cellSize, cellSize)
                )

                drawRect(
                    color = fgColor,
                    topLeft = Offset(mirrorX, y),
                    size = Size(cellSize, cellSize)
                )
            }
        }
    }
}
