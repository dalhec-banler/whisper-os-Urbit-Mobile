// Whisper OS · Sigil Glyphs · Jetpack Compose reference implementation.
//
// One file, no external dependencies beyond compose.foundation /
// compose.ui.graphics. Drop into the design module.
//
// Companion docs:
//   SIGIL_GLYPHS.md   — the spec these match
//   test-vectors.md   — patp → hash → palette index, must stay in sync
//
// Pattern at the call site:
//
//   // system app
//   SigilGlyph(GlyphKind.Phone, size = 48.dp)
//
//   // hosted app — host @p picks palette
//   SigilGlyph(GlyphKind.Messages, patp = "~lodlur-modreg", size = 48.dp)
//
//   // hosted app with provenance dot
//   SigilGlyph(
//       GlyphKind.Messages,
//       patp = "~lodlur-modreg",
//       size = 48.dp,
//       provenance = true,
//   )
//
//   // a person — never a glyph
//   Sigil(patp = "~palfun-foslup", size = 40.dp)

package io.nativeplanet.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────
// 1 · Palette tokens
// ─────────────────────────────────────────────────────────────

/** The eight palette pairs. Single source of truth — do not add more. */
enum class SigilPalette(val bg: Color, val fg: Color) {
    Amber(   Color(0xFF1A1A1A), Color(0xFFF3A712)),
    Mint(    Color(0xFF10221C), Color(0xFF7FC7A3)),
    Lavender(Color(0xFF1A1530), Color(0xFFA18CD1)),
    Coral(   Color(0xFF2A1A1A), Color(0xFFD97757)),
    Sky(     Color(0xFF0E1A2A), Color(0xFF7DA6C9)),
    Paper(   Color(0xFF1C1C1C), Color(0xFFE8E8E8)),
    Wheat(   Color(0xFF221A10), Color(0xFFD6B370)),
    Sage(    Color(0xFF1A1A1A), Color(0xFF9BB380));

    companion object {
        private val byIndex = values()

        /** Look up by hash result. Handles unsigned mod correctly. */
        fun forIndex(i: Int): SigilPalette {
            // h is the result of fnv1a32; treat as unsigned before mod.
            val mod = (i.toUInt() % byIndex.size.toUInt()).toInt()
            return byIndex[mod]
        }

        /** @p → palette via FNV-1a hash. See test-vectors.md for conformance. */
        fun forPatp(patp: String): SigilPalette = forIndex(fnv1a32(patp))
    }
}

// ─────────────────────────────────────────────────────────────
// 2 · Hash (must match JS reference byte-for-byte)
// ─────────────────────────────────────────────────────────────

/**
 * FNV-1a 32-bit hash. Iterates over UTF-8 bytes of the patp string.
 *
 * Reference (JS):
 *   let h = 2166136261;
 *   for (let i = 0; i < s.length; i++) { h ^= s.charCodeAt(i); h = Math.imul(h, 16777619); }
 *   return h >>> 0;
 *
 * Whisper Launcher always passes ASCII patps (`~lodlur-modreg`), so the
 * UTF-8 byte sequence equals the char code sequence. If non-ASCII ever
 * appears, decide policy and update both impls in lockstep.
 *
 * Returns SIGNED Int. Callers do `(h.toUInt() % N.toUInt()).toInt()` for
 * a non-negative index.
 */
fun fnv1a32(s: String): Int {
    var h = 0x811C9DC5.toInt()              // 2166136261, signed
    for (c in s) {
        h = h xor c.code                    // ASCII: c.code == utf8 byte
        h = h * 0x01000193                  // signed int overflow OK
    }
    return h
}

// ─────────────────────────────────────────────────────────────
// 3 · Glyph kinds + defaults
// ─────────────────────────────────────────────────────────────

enum class GlyphKind {
    // communication
    Messages, Talk, Phone, Mail, Contact, Hark,
    // time + place
    Calendar, Clock, Maps, Weather,
    // capture + media
    Camera, Photos, Music, Studio,
    // documents
    Notes, Files, Read, Memex, Wallet,
    // system
    Settings, Browser, Search, Dojo, Ai,
}

private fun GlyphKind.defaultPalette(): SigilPalette = when (this) {
    GlyphKind.Phone, GlyphKind.Talk, GlyphKind.Contact -> SigilPalette.Mint
    GlyphKind.Messages, GlyphKind.Calendar, GlyphKind.Hark, GlyphKind.Ai -> SigilPalette.Amber
    GlyphKind.Mail, GlyphKind.Wallet, GlyphKind.Read -> SigilPalette.Wheat
    GlyphKind.Camera, GlyphKind.Photos, GlyphKind.Studio -> SigilPalette.Coral
    GlyphKind.Maps, GlyphKind.Weather -> SigilPalette.Sky
    GlyphKind.Music, GlyphKind.Memex -> SigilPalette.Lavender
    GlyphKind.Files, GlyphKind.Notes -> SigilPalette.Sage
    else -> SigilPalette.Paper  // Clock, Settings, Browser, Search, Dojo
}

/** Glyphs that mirror under RTL. See SIGIL_GLYPHS.md §9. */
private fun GlyphKind.mirrorsInRtl(): Boolean = when (this) {
    GlyphKind.Messages, GlyphKind.Search, GlyphKind.Files, GlyphKind.Dojo,
    GlyphKind.Notes, GlyphKind.Read, GlyphKind.Memex -> true
    else -> false
}

/** Localized name read by accessibility services. Wire to your strings.xml. */
private fun GlyphKind.contentDescription(): String = when (this) {
    GlyphKind.Phone -> "Phone"
    GlyphKind.Messages -> "Messages"
    GlyphKind.Mail -> "Mail"
    GlyphKind.Camera -> "Camera"
    GlyphKind.Maps -> "Maps"
    GlyphKind.Calendar -> "Calendar"
    GlyphKind.Clock -> "Clock"
    GlyphKind.Music -> "Music"
    GlyphKind.Photos -> "Photos"
    GlyphKind.Settings -> "Settings"
    GlyphKind.Wallet -> "Wallet"
    GlyphKind.Notes -> "Notes"
    GlyphKind.Files -> "Files"
    GlyphKind.Weather -> "Weather"
    GlyphKind.Browser -> "Browser"
    GlyphKind.Search -> "Search"
    GlyphKind.Talk -> "Talk"
    GlyphKind.Memex -> "Memex"
    GlyphKind.Read -> "Read"
    GlyphKind.Studio -> "Studio"
    GlyphKind.Hark -> "Notifications"
    GlyphKind.Ai -> "Assistant"
    GlyphKind.Contact -> "Contacts"
    GlyphKind.Dojo -> "Developer console"
}

// ─────────────────────────────────────────────────────────────
// 4 · State variants (see spec §11)
// ─────────────────────────────────────────────────────────────

enum class SigilState { Default, Pressed, Focused, Disabled, Badged, Running }

// ─────────────────────────────────────────────────────────────
// 5 · Public Composables
// ─────────────────────────────────────────────────────────────

/**
 * Designed glyph cell. Use for system functions and hosted apps.
 *
 * @param glyph        the function shape
 * @param patp         host @p; provides palette. Null → use [glyph]'s default.
 * @param size         cell width = height; must be ≥ 16.dp
 * @param paletteOverride caller-supplied palette; escape hatch only
 * @param invert       swap bg/fg (paper cell, ink mark)
 * @param provenance   show a host-color dot in the bottom-right corner
 * @param state        visual state — see SIGIL_GLYPHS.md §11
 */
@Composable
fun SigilGlyph(
    glyph: GlyphKind,
    modifier: Modifier = Modifier,
    patp: String? = null,
    size: Dp = 40.dp,
    paletteOverride: SigilPalette? = null,
    invert: Boolean = false,
    provenance: Boolean = false,
    state: SigilState = SigilState.Default,
) {
    val basePalette = paletteOverride
        ?: patp?.let(SigilPalette::forPatp)
        ?: glyph.defaultPalette()
    val bg = if (invert) basePalette.fg else basePalette.bg
    val fg = if (invert) basePalette.bg else basePalette.fg
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl && glyph.mirrorsInRtl()
    val alpha = if (state == SigilState.Disabled) 0.35f else 1f

    Box(
        modifier
            .size(size)
            .semantics { contentDescription = glyph.contentDescription() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size).background(bg)) {
            val inset = this.size.width * 0.14f
            val viewport = this.size.width - inset * 2f
            val u = viewport / 24f
            translate(left = inset, top = inset) {
                if (isRtl) {
                    // Mirror around the 24-unit grid center
                    scaleAroundX(viewport / 2f) { drawGlyph(glyph, fg, bg, u) }
                } else {
                    drawGlyph(glyph, fg, bg, u)
                }
            }
            // Provenance dot — bottom-right corner, host fg color, bordered in bg
            if (provenance && patp != null) {
                val dotSize = this.size.width * 0.22f
                val border = this.size.width * 0.025f
                drawRect(
                    color = bg,
                    topLeft = Offset(this.size.width - dotSize - border * 2, this.size.height - dotSize - border * 2),
                    size = Size(dotSize + border * 2, dotSize + border * 2),
                )
                drawRect(
                    color = SigilPalette.forPatp(patp).fg,
                    topLeft = Offset(this.size.width - dotSize - border, this.size.height - dotSize - border),
                    size = Size(dotSize, dotSize),
                )
            }
            // State overlays — pressed / badged / running
            when (state) {
                SigilState.Pressed -> drawRect(Color.White.copy(alpha = 0.06f))
                SigilState.Badged -> drawRect(
                    color = SigilPalette.Amber.fg,
                    topLeft = Offset(this.size.width - this.size.width * 0.16f, 0f),
                    size = Size(this.size.width * 0.16f, this.size.height * 0.16f),
                )
                else -> Unit
            }
        }
        if (alpha < 1f) {
            // disabled overlay — full-cell wash
            Canvas(Modifier.size(size)) {
                drawRect(Color(0x80000000.toInt()))
            }
        }
        // Focused outline is drawn outside the cell — caller can layer if needed
    }
}

/**
 * Hashed identity sigil. ONE rule: only ever use this for a person or @p.
 * Never for an app function. See spec §7.
 *
 * Note: this reference impl uses one of 12 simple primitive shapes hashed
 * from the @p. The production component may swap in the full
 * Tlon/Urbit @p sigil generator — the contract (square cell, two-color,
 * hashed palette) stays the same.
 */
@Composable
fun Sigil(
    patp: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val palette = SigilPalette.forPatp(patp)
    val hash = fnv1a32(patp)
    val shapeIdx = ((hash ushr 4).toUInt() % SIGIL_SHAPE_COUNT.toUInt()).toInt()

    Box(
        modifier
            .size(size)
            .semantics { contentDescription = patp },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size).background(palette.bg)) {
            val inset = this.size.width * 0.14f
            val viewport = this.size.width - inset * 2f
            val u = viewport / 24f
            translate(left = inset, top = inset) {
                drawSigilShape(shapeIdx, palette.fg, u)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 6 · Drawing helpers — unit-scaled primitives
// All coordinates are in 24-unit logical space; multiplied by `u`.
// ─────────────────────────────────────────────────────────────

private fun DrawScope.rectU(c: Color, x: Float, y: Float, w: Float, h: Float, u: Float) =
    drawRect(c, Offset(x * u, y * u), Size(w * u, h * u))

private fun DrawScope.circleU(c: Color, cx: Float, cy: Float, r: Float, u: Float) =
    drawCircle(c, radius = r * u, center = Offset(cx * u, cy * u))

private fun DrawScope.strokeCircleU(c: Color, cx: Float, cy: Float, r: Float, w: Float, u: Float) =
    drawCircle(c, radius = r * u, center = Offset(cx * u, cy * u), style = Stroke(width = w * u))

private fun DrawScope.strokeOvalU(c: Color, cx: Float, cy: Float, rx: Float, ry: Float, w: Float, u: Float) =
    drawOval(
        c,
        topLeft = Offset((cx - rx) * u, (cy - ry) * u),
        size = Size(rx * 2 * u, ry * 2 * u),
        style = Stroke(width = w * u),
    )

private fun DrawScope.pathU(c: Color, u: Float, build: Path.() -> Unit) =
    drawPath(Path().apply(build), c)

private fun Path.mU(x: Float, y: Float, u: Float) = moveTo(x * u, y * u)
private fun Path.lU(x: Float, y: Float, u: Float) = lineTo(x * u, y * u)
private fun Path.qU(cx: Float, cy: Float, x: Float, y: Float, u: Float) =
    quadraticBezierTo(cx * u, cy * u, x * u, y * u)

// Helper: rotate a sub-drawing around a unit-space pivot
private fun DrawScope.rotateU(deg: Float, px: Float, py: Float, u: Float, block: DrawScope.() -> Unit) =
    rotate(degrees = deg, pivot = Offset(px * u, py * u)) { block() }

// Mirror horizontally for RTL
private fun DrawScope.scaleAroundX(x: Float, block: DrawScope.() -> Unit) {
    val mat = androidx.compose.ui.graphics.Matrix().apply {
        translate(x, 0f, 0f); scale(-1f, 1f, 1f); translate(-x, 0f, 0f)
    }
    drawContext.canvas.save()
    drawContext.canvas.concat(mat)
    block()
    drawContext.canvas.restore()
}

// ─────────────────────────────────────────────────────────────
// 7 · Glyph dispatch + per-glyph painters
// Each function is a direct port of its JSX counterpart in
// sigil-glyphs.jsx. Keep this in sync if either side changes.
// ─────────────────────────────────────────────────────────────

private fun DrawScope.drawGlyph(g: GlyphKind, fg: Color, bg: Color, u: Float) {
    when (g) {
        GlyphKind.Messages -> messages(fg, u)
        GlyphKind.Talk     -> talk(fg, u)
        GlyphKind.Phone    -> phone(fg, u)
        GlyphKind.Mail     -> mail(fg, bg, u)
        GlyphKind.Contact  -> contact(fg, u)
        GlyphKind.Hark     -> hark(fg, u)
        GlyphKind.Calendar -> calendar(fg, bg, u)
        GlyphKind.Clock    -> clock(fg, bg, u)
        GlyphKind.Maps     -> maps(fg, bg, u)
        GlyphKind.Weather  -> weather(fg, u)
        GlyphKind.Camera   -> camera(fg, bg, u)
        GlyphKind.Photos   -> photos(fg, bg, u)
        GlyphKind.Music    -> music(fg, u)
        GlyphKind.Studio   -> studio(fg, u)
        GlyphKind.Notes    -> notes(fg, u)
        GlyphKind.Files    -> files(fg, u)
        GlyphKind.Read     -> read(fg, bg, u)
        GlyphKind.Memex    -> memex(fg, u)
        GlyphKind.Wallet   -> wallet(fg, u)
        GlyphKind.Settings -> settings(fg, u)
        GlyphKind.Browser  -> browser(fg, bg, u)
        GlyphKind.Search   -> search(fg, u)
        GlyphKind.Dojo     -> dojo(fg, u)
        GlyphKind.Ai       -> ai(fg, u)
    }
}

// — communication —
private fun DrawScope.messages(fg: Color, u: Float) = pathU(fg, u) {
    mU(3f, 5f, u); lU(21f, 5f, u); lU(21f, 16f, u); lU(10f, 16f, u)
    lU(5f, 20f, u); lU(7f, 16f, u); lU(3f, 16f, u); close()
}

private fun DrawScope.talk(fg: Color, u: Float) {
    circleU(fg, 12f, 12f, 3.5f, u)
    pathU(fg, u) {
        mU(5.5f, 5.5f, u); qU(3f, 12f, 5.5f, 18.5f, u)
        lU(7f, 18.5f, u);  qU(5f, 12f, 7f, 5.5f, u); close()
    }
    pathU(fg, u) {
        mU(18.5f, 5.5f, u); qU(21f, 12f, 18.5f, 18.5f, u)
        lU(17f, 18.5f, u);   qU(19f, 12f, 17f, 5.5f, u); close()
    }
}

private fun DrawScope.phone(fg: Color, u: Float) {
    circleU(fg, 6.5f, 6.5f, 3f, u)
    circleU(fg, 17.5f, 17.5f, 3f, u)
    rotateU(-45f, 12f, 12f, u) { rectU(fg, 11f, 2f, 2f, 20f, u) }
}

private fun DrawScope.mail(fg: Color, bg: Color, u: Float) {
    rectU(fg, 3f, 6f, 18f, 13f, u)
    pathU(bg, u) {
        mU(3f, 6f, u); lU(12f, 14f, u); lU(21f, 6f, u)
        lU(21f, 8.5f, u); lU(12f, 16.5f, u); lU(3f, 8.5f, u); close()
    }
}

private fun DrawScope.contact(fg: Color, u: Float) {
    circleU(fg, 12f, 8f, 4f, u)
    pathU(fg, u) {
        mU(3f, 22f, u); qU(3f, 13f, 12f, 13f, u); qU(21f, 13f, 21f, 22f, u); close()
    }
}

private fun DrawScope.hark(fg: Color, u: Float) {
    circleU(fg, 12f, 11f, 7f, u)
    rectU(fg, 9f, 18f, 6f, 3f, u)
}

// — time + place —
private fun DrawScope.calendar(fg: Color, bg: Color, u: Float) {
    rectU(fg, 3f, 5f, 18f, 16f, u)
    rectU(bg, 3f, 9f, 18f, 1.5f, u)
    rectU(fg, 6f, 3f, 2f, 5f, u)
    rectU(fg, 16f, 3f, 2f, 5f, u)
}

private fun DrawScope.clock(fg: Color, bg: Color, u: Float) {
    circleU(fg, 12f, 12f, 9f, u)
    rectU(bg, 11f, 5f, 2f, 8f, u)
    rectU(bg, 11f, 11f, 7f, 2f, u)
}

private fun DrawScope.maps(fg: Color, bg: Color, u: Float) {
    pathU(fg, u) {
        mU(12f, 2f, u); lU(21f, 12f, u); lU(12f, 22f, u); lU(3f, 12f, u); close()
    }
    circleU(bg, 12f, 10f, 2.5f, u)
}

private fun DrawScope.weather(fg: Color, u: Float) {
    circleU(fg, 12f, 12f, 5f, u)
    rectU(fg, 11f, 2f, 2f, 3f, u)
    rectU(fg, 11f, 19f, 2f, 3f, u)
    rectU(fg, 2f, 11f, 3f, 2f, u)
    rectU(fg, 19f, 11f, 3f, 2f, u)
    rotateU(-45f, 5.5f, 6f, u)   { rectU(fg, 4.5f,  4.5f, 2f, 3f, u) }
    rotateU( 45f, 18.5f, 6f, u)  { rectU(fg, 17.5f, 4.5f, 2f, 3f, u) }
    rotateU( 45f, 5.5f, 18f, u)  { rectU(fg, 4.5f, 16.5f, 2f, 3f, u) }
    rotateU(-45f, 18.5f, 18f, u) { rectU(fg, 17.5f, 16.5f, 2f, 3f, u) }
}

// — capture + media —
private fun DrawScope.camera(fg: Color, bg: Color, u: Float) {
    rectU(fg, 3f, 7f, 18f, 14f, u)
    rectU(fg, 8f, 4f, 8f, 3f, u)
    circleU(bg, 12f, 14f, 4f, u)
    circleU(fg, 12f, 14f, 1.5f, u)
}

private fun DrawScope.photos(fg: Color, bg: Color, u: Float) {
    rectU(fg, 3f, 3f, 13f, 13f, u)
    rectU(bg, 6f, 6f, 14f, 14f, u)
    rectU(fg, 7f, 7f, 13f, 13f, u)
}

private fun DrawScope.music(fg: Color, u: Float) {
    rectU(fg, 11f, 4f, 2f, 14f, u)
    circleU(fg, 8f, 17f, 3.5f, u)
}

private fun DrawScope.studio(fg: Color, u: Float) {
    rectU(fg, 4f, 4f, 7f, 7f, u)
    rectU(fg, 13f, 4f, 7f, 7f, u)
    rectU(fg, 4f, 13f, 7f, 7f, u)
    rectU(fg, 13f, 13f, 7f, 7f, u)
}

// — documents —
private fun DrawScope.notes(fg: Color, u: Float) = pathU(fg, u) {
    mU(5f, 4f, u); lU(16f, 4f, u); lU(20f, 8f, u); lU(20f, 20f, u); lU(5f, 20f, u); close()
}

private fun DrawScope.files(fg: Color, u: Float) {
    rectU(fg, 3f, 4f, 9f, 4f, u)
    rectU(fg, 3f, 7f, 18f, 14f, u)
}

private fun DrawScope.read(fg: Color, bg: Color, u: Float) {
    rectU(fg, 3f, 5f, 18f, 14f, u)
    rectU(bg, 11.5f, 5f, 1f, 14f, u)
}

private fun DrawScope.memex(fg: Color, u: Float) {
    rectU(fg, 4f, 6f, 13f, 2f, u)
    rectU(fg, 4f, 11f, 16f, 2f, u)
    rectU(fg, 4f, 16f, 10f, 2f, u)
    rectU(fg, 18f, 6f, 2f, 2f, u)
}

private fun DrawScope.wallet(fg: Color, u: Float) {
    rectU(fg, 3f, 6f, 18f, 5f, u)
    rectU(fg, 3f, 13f, 18f, 5f, u)
}

// — system —
private fun DrawScope.settings(fg: Color, u: Float) {
    rectU(fg, 11f, 3f, 2f, 18f, u)
    rectU(fg, 3f, 11f, 18f, 2f, u)
    rotateU(45f, 12f, 12f, u) {
        rectU(fg, 11f, 3f, 2f, 18f, u)
        rectU(fg, 3f, 11f, 18f, 2f, u)
    }
}

private fun DrawScope.browser(fg: Color, bg: Color, u: Float) {
    circleU(fg, 12f, 12f, 9f, u)
    rectU(bg, 3f, 11f, 18f, 2f, u)
    strokeOvalU(bg, 12f, 12f, 4f, 9f, 2f, u)
}

private fun DrawScope.search(fg: Color, u: Float) {
    strokeCircleU(fg, 10f, 10f, 6f, 3f, u)
    rotateU(-45f, 15.5f, 18f, u) { rectU(fg, 14f, 14f, 3f, 8f, u) }
}

private fun DrawScope.dojo(fg: Color, u: Float) {
    drawPath(
        Path().apply {
            mU(4f, 6f, u); lU(11f, 12f, u); lU(4f, 18f, u)
        },
        fg,
        style = Stroke(width = 2.5f * u),
    )
    rectU(fg, 12f, 17f, 9f, 2f, u)
}

private fun DrawScope.ai(fg: Color, u: Float) = pathU(fg, u) {
    mU(12f, 2f, u);  lU(13.5f, 10.5f, u); lU(22f, 12f, u); lU(13.5f, 13.5f, u)
    lU(12f, 22f, u); lU(10.5f, 13.5f, u); lU(2f, 12f, u); lU(10.5f, 10.5f, u); close()
}

// ─────────────────────────────────────────────────────────────
// 8 · Person sigil shapes (12 primitives — same as JS reference)
// ─────────────────────────────────────────────────────────────

private const val SIGIL_SHAPE_COUNT = 12

private fun DrawScope.drawSigilShape(idx: Int, fg: Color, u: Float) {
    when (idx) {
        0 -> rectU(fg, 6f, 6f, 12f, 12f, u)
        1 -> circleU(fg, 12f, 12f, 6f, u)
        2 -> pathU(fg, u) { mU(12f, 5f, u); lU(19f, 19f, u); lU(5f, 19f, u); close() }
        3 -> pathU(fg, u) { mU(12f, 4f, u); lU(20f, 12f, u); lU(12f, 20f, u); lU(4f, 12f, u); close() }
        4 -> {
            pathU(fg, u) {
                mU(5f, 12f, u)
                // approximate arc with two quadratic curves
                qU(5f, 5f, 12f, 5f, u); qU(19f, 5f, 19f, 12f, u)
                lU(12f, 19f, u); close()
            }
        }
        5 -> rectU(fg, 5f, 11f, 14f, 2f, u)
        6 -> {
            rectU(fg, 5f, 5f, 6f, 6f, u)
            rectU(fg, 13f, 13f, 6f, 6f, u)
        }
        7 -> {
            circleU(fg, 9f, 9f, 3f, u)
            circleU(fg, 15f, 15f, 3f, u)
        }
        8 -> pathU(fg, u) { mU(5f, 5f, u); lU(19f, 5f, u); lU(12f, 19f, u); close() }
        9 -> pathU(fg, u) { mU(5f, 19f, u); lU(19f, 19f, u); lU(12f, 5f, u); close() }
        10 -> {
            rectU(fg, 5f, 5f, 6f, 14f, u)
            rectU(fg, 13f, 5f, 6f, 14f, u)
        }
        11 -> pathU(fg, u) {
            mU(5f, 12f, u); lU(12f, 5f, u); lU(19f, 12f, u); lU(12f, 19f, u); close()
            mU(9f, 12f, u); lU(12f, 9f, u); lU(15f, 12f, u); lU(12f, 15f, u); close()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 9 · Test vector data class
// Unit test reads test-vectors.md (or test-vectors.kt.txt) and validates.
// ─────────────────────────────────────────────────────────────

data class SigilTestVec(
    val patp: String,
    val hashS: Int,
    val paletteIdx: Int,
    val palette: SigilPalette,
)

/**
 * Reference test (paste into androidTest/ or commonTest/):
 *
 *   @Test fun palettesMatchJsReference() {
 *       SIGIL_TEST_VECTORS.forEach { v ->
 *           assertEquals(v.patp, v.hashS, fnv1a32(v.patp))
 *           val actual = SigilPalette.forPatp(v.patp)
 *           assertEquals(v.patp, v.palette, actual)
 *       }
 *   }
 *
 * Vectors live in handoff/compose/test-vectors.kt.txt — copy as needed.
 */
