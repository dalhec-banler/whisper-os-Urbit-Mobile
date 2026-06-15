package io.nativeplanet.launcher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.nativeplanet.launcher.domain.model.RuntimeState
import io.nativeplanet.launcher.platform.LauncherAppInfo
import io.nativeplanet.launcher.theme.NPColors
import io.nativeplanet.launcher.theme.NPSpacing
import io.nativeplanet.launcher.theme.NPType
import io.nativeplanet.launcher.theme.NativePlanetTheme
import io.nativeplanet.launcher.ui.components.GlyphKind
import io.nativeplanet.launcher.ui.components.SigilGlyph

enum class AppWorld(val label: String, val color: Color) {
    URBIT("urbit", NPColors.worldUrbit),
    PLAY("play", NPColors.worldPlay),
    WEB("web", NPColors.worldWeb),
    SYSTEM("system", NPColors.worldSystem),
    DEV("dev", NPColors.worldDev)
}

data class LauncherTile(
    val label: String,
    val glyph: GlyphKind,
    val origin: String,
    val world: AppWorld,
    val hostPatp: String? = null,
    val onClick: () -> Unit
)

@Composable
fun StatusStrip(
    text: String,
    modifier: Modifier = Modifier,
    inverted: Boolean = false
) {
    val background = if (inverted) NPColors.paper else NPColors.ink
    val foreground = if (inverted) NPColors.ink else NPColors.paper

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "%",
            style = NPType.caption.copy(letterSpacing = 0.3.sp),
            color = NPColors.accentAmber
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = NPType.caption.copy(letterSpacing = 0.3.sp),
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    dark: Boolean = false
) {
    Text(
        text = "── ${text.uppercase()}",
        style = NPType.sectionLabel,
        color = if (dark) NPColors.paperFaint else NPColors.inkFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun FindAskBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false
) {
    val background = if (dark) NPColors.paper.copy(alpha = 0.06f) else NPColors.ink.copy(alpha = 0.06f)
    val foreground = if (dark) NPColors.paperDim else NPColors.inkDim

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(NPColors.accentAmber)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = NPType.bodySm,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        SigilGlyph(
            glyph = GlyphKind.Ai,
            size = 22.dp,
            invert = dark
        )
    }
}

@Composable
fun LauncherGridCell(
    label: String,
    glyph: GlyphKind,
    modifier: Modifier = Modifier,
    hostPatp: String? = null,
    provenance: Boolean = false,
    size: Dp = NPSpacing.appCell,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(size)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SigilGlyph(
            glyph = glyph,
            patp = hostPatp,
            provenance = provenance,
            size = size
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = NPType.nano,
            color = NPColors.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ProvenanceAppCell(
    label: String,
    origin: String,
    world: AppWorld,
    modifier: Modifier = Modifier,
    glyph: GlyphKind? = null,
    packageName: String? = null,
    hostPatp: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .width(NPSpacing.appCell)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            if (glyph != null) {
                SigilGlyph(
                    glyph = glyph,
                    patp = hostPatp,
                    provenance = hostPatp != null,
                    size = NPSpacing.appCell
                )
            } else {
                PlatformIconCell(
                    label = label,
                    world = world,
                    size = NPSpacing.appCell,
                    packageName = packageName
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(4.dp)
                    .height(NPSpacing.appCell)
                    .background(world.color)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = NPType.nano,
            color = NPColors.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = origin,
            style = NPType.nano.copy(fontSize = 8.sp),
            color = world.color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlatformIconCell(
    label: String,
    world: AppWorld,
    size: Dp,
    packageName: String? = null
) {
    val density = LocalDensity.current
    val initial = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val iconSize = size - 16.dp
    val iconBitmap = rememberPlatformIcon(packageName, with(density) { iconSize.roundToPx() })

    Box(
        modifier = Modifier
            .size(size)
            .background(NPColors.paper),
        contentAlignment = Alignment.Center
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(NPColors.inkHair),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = NPType.headline,
                    color = world.color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun rememberPlatformIcon(packageName: String?, sizePx: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName, sizePx) {
        if (packageName == null) {
            null
        } else {
            runCatching {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(width = sizePx, height = sizePx)
                    .asImageBitmap()
            }.getOrNull()
        }
    }
}

@Composable
fun UtilityDock(
    onPhone: () -> Unit,
    onBrowser: () -> Unit,
    onCamera: () -> Unit,
    onSettings: () -> Unit,
    onApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NPColors.ink)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockItem("phone", GlyphKind.Phone, onPhone)
        DockItem("messages", GlyphKind.Messages, onBrowser)
        DockItem("camera", GlyphKind.Camera, onCamera)
        DockItem("settings", GlyphKind.Settings, onSettings)
        DockItem("apps", GlyphKind.Browser, onApps)
    }
}

@Composable
private fun DockItem(
    label: String,
    glyph: GlyphKind,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SigilGlyph(glyph = glyph, size = 44.dp)
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = NPType.nano,
            color = NPColors.paperDim,
            maxLines = 1
        )
    }
}

@Composable
fun LauncherNavPill(
    color: Color = NPColors.paper.copy(alpha = 0.4f),
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(132.dp)
            .height(4.dp)
    ) {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            size = size
        )
    }
}

@Composable
fun AppRow(
    app: LauncherAppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val world = app.world()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = NPSpacing.rowPadY),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(44.dp)
                .background(world.color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        AppGlyph(label = app.label, world = world)
        Spacer(modifier = Modifier.width(NPSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = NPType.bodyMd,
                color = NPColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.originLabel(),
                style = NPType.nano,
                color = world.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppGlyph(
    label: String,
    modifier: Modifier = Modifier,
    world: AppWorld = AppWorld.DEV,
    size: Dp = 40.dp,
    packageName: String? = null
) {
    Box(modifier = modifier) {
        PlatformIconCell(label = label, world = world, size = size, packageName = packageName)
    }
}

@Composable
fun RuntimeDot(
    state: RuntimeState,
    modifier: Modifier = Modifier
) {
    val color = when (state) {
        RuntimeState.RUNNING -> NPColors.mint
        RuntimeState.STARTING, RuntimeState.STOPPING -> NPColors.accentAmber
        RuntimeState.ERROR, RuntimeState.CRASHED -> NPColors.error
        RuntimeState.STOPPED -> NPColors.accentStone
        RuntimeState.UNINITIALIZED -> NPColors.accentSlate
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .background(color)
    )
}

fun LauncherAppInfo.world(): AppWorld = when {
    packageName.startsWith("io.nativeplanet") -> AppWorld.URBIT
    packageName.startsWith("app.grapheneos") -> AppWorld.SYSTEM
    packageName.startsWith("com.android") ||
        packageName.startsWith("com.google.android") ||
        packageName.contains("settings", ignoreCase = true) -> AppWorld.SYSTEM
    packageName.contains("browser", ignoreCase = true) ||
        packageName.contains("vanadium", ignoreCase = true) -> AppWorld.WEB
    packageName.contains("debug", ignoreCase = true) -> AppWorld.DEV
    else -> AppWorld.PLAY
}

fun LauncherAppInfo.originLabel(): String = when (world()) {
    AppWorld.URBIT -> "urbit"
    AppWorld.PLAY -> "play"
    AppWorld.WEB -> "web"
    AppWorld.SYSTEM -> "system"
    AppWorld.DEV -> "dev"
}

fun glyphForApp(app: LauncherAppInfo): GlyphKind? = when {
    app.packageName.startsWith("io.nativeplanet") -> GlyphKind.Ai
    app.label.contains("phone", ignoreCase = true) -> GlyphKind.Phone
    app.label.contains("message", ignoreCase = true) -> GlyphKind.Messages
    app.label.contains("camera", ignoreCase = true) -> GlyphKind.Camera
    app.label.contains("settings", ignoreCase = true) -> GlyphKind.Settings
    app.label.contains("browser", ignoreCase = true) ||
        app.label.contains("vanadium", ignoreCase = true) -> GlyphKind.Browser
    else -> null
}

fun String.withPatpSig(): String {
    return if (startsWith("~")) this else "~$this"
}
