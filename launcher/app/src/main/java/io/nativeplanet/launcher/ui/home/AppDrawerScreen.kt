package io.nativeplanet.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import io.nativeplanet.launcher.platform.GuestLauncher
import io.nativeplanet.launcher.platform.LauncherAppInfo
import io.nativeplanet.launcher.theme.NPColors
import io.nativeplanet.launcher.theme.NPSpacing
import io.nativeplanet.launcher.theme.NPType
import io.nativeplanet.launcher.ui.components.GlyphKind
import io.nativeplanet.launcher.ui.components.SigilGlyph
import kotlin.math.roundToInt

@Composable
fun AppDrawerScreen(
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuntimeStatusViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val apps = remember(context) { GuestLauncher.installedApps(context) }
    var query by remember { mutableStateOf("") }
    var dropTargetVisible by remember { mutableStateOf(false) }
    var dropTargetArmed by remember { mutableStateOf(false) }
    var closeDragTotal by remember { mutableStateOf(0f) }
    val closeThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val gridState = rememberLazyGridState()
    val hostPatp = (uiState.bootPackageStatus.parent ?: uiState.runtimeStatus.shipName)?.withPatpSig()
    BackHandler(onBack = onBack)
    val closeScrollConnection = remember(dropTargetVisible, closeThreshold, gridState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (dropTargetVisible) return Offset.Zero
                val drawerIsAtTop = gridState.firstVisibleItemIndex == 0 &&
                    gridState.firstVisibleItemScrollOffset == 0
                if (!drawerIsAtTop) {
                    closeDragTotal = 0f
                    return Offset.Zero
                }

                if (available.y > 0f) {
                    closeDragTotal += available.y
                    if (closeDragTotal > closeThreshold) {
                        closeDragTotal = 0f
                        onBack()
                    }
                } else if (available.y < 0f) {
                    closeDragTotal = 0f
                }
                return Offset.Zero
            }
        }
    }
    val closeSwipeModifier = if (!dropTargetVisible) {
        Modifier.pointerInput(Unit) {
            var dragTotal = 0f
            detectVerticalDragGestures(
                onDragStart = { dragTotal = 0f },
                onVerticalDrag = { _, dragAmount -> dragTotal += dragAmount },
                onDragEnd = {
                    if (dragTotal > closeThreshold) {
                        onBack()
                    }
                },
                onDragCancel = { dragTotal = 0f }
            )
        }
    } else {
        Modifier
    }

    val visibleApps = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NPColors.paper)
            .navigationBarsPadding()
            .then(closeSwipeModifier)
    ) {
        StatusStrip(
            text = "all apps · ${apps.size} installed · ${uiState.networkStatus.type.name.lowercase()}",
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = NPSpacing.screenPadX)
                .padding(top = 60.dp, bottom = 78.dp)
        ) {
            FilterField(
                value = query,
                onValueChange = { query = it },
                count = visibleApps.size
            )

            Spacer(modifier = Modifier.height(18.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(NPSpacing.gridGap),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(closeScrollConnection)
            ) {
                items(visibleApps, key = { "${it.packageName}/${it.className}" }) { app ->
                    DrawerDraggableApp(
                        app = app,
                        hostPatp = hostPatp,
                        onLaunch = { GuestLauncher.launchApp(context, app) },
                        onAddedToHome = onBack,
                        onDragActive = { active ->
                            dropTargetVisible = active
                            if (!active) dropTargetArmed = false
                        },
                        onDropHover = { armed -> dropTargetArmed = armed }
                    )
                }
            }

            if (apps.size > 20) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "a b c d e f g h i j k l m n o p q r s t u v w x y z",
                    style = NPType.nano,
                    color = NPColors.inkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!dropTargetVisible) {
            FindAskBar(
                text = "find an app, or ask…",
                onClick = onSearch,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = NPSpacing.screenPadX)
                    .padding(bottom = 24.dp)
            )
        } else {
            LauncherNavPill(
                color = NPColors.ink.copy(alpha = 0.22f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }

        if (dropTargetVisible) {
            DrawerDropTarget(
                armed = dropTargetArmed,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = NPSpacing.screenPadX)
                    .padding(bottom = 42.dp)
                    .zIndex(1f)
            )
        }
    }
}

@Composable
private fun DrawerDraggableApp(
    app: LauncherAppInfo,
    hostPatp: String?,
    onLaunch: () -> Unit,
    onAddedToHome: () -> Unit,
    onDragActive: (Boolean) -> Unit,
    onDropHover: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val dropThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    var dragOffset by remember(app.packageName, app.className) { mutableStateOf(Offset.Zero) }
    var dragging by remember(app.packageName, app.className) { mutableStateOf(false) }
    var dropArmed by remember(app.packageName, app.className) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .zIndex(if (dragging) 20f else 0f)
            .graphicsLayer(
                scaleX = if (dragging) 1.08f else 1f,
                scaleY = if (dragging) 1.08f else 1f,
                alpha = if (dragging) 0.92f else 1f
            )
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .pointerInput(app.packageName, app.className) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragging = true
                        dragOffset = Offset.Zero
                        dropArmed = false
                        onDragActive(true)
                        onDropHover(false)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        val nextDropArmed = dragOffset.y > dropThreshold
                        if (nextDropArmed != dropArmed) {
                            dropArmed = nextDropArmed
                            onDropHover(nextDropArmed)
                        }
                    },
                    onDragEnd = {
                        if (dropArmed) {
                            HomeLayoutStore.addApp(context, app, hostPatp)
                            onAddedToHome()
                        }
                        dragging = false
                        dragOffset = Offset.Zero
                        dropArmed = false
                        onDragActive(false)
                        onDropHover(false)
                    },
                    onDragCancel = {
                        dragging = false
                        dragOffset = Offset.Zero
                        dropArmed = false
                        onDragActive(false)
                        onDropHover(false)
                    }
                )
            }
    ) {
        ProvenanceAppCell(
            label = app.label,
            origin = app.originLabel(),
            world = app.world(),
            glyph = glyphForApp(app),
            packageName = app.packageName,
            onClick = onLaunch
        )
    }
}

@Composable
private fun DrawerDropTarget(
    armed: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(0.86f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(if (armed) NPColors.ink else NPColors.ink.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SigilGlyph(
            glyph = GlyphKind.Browser,
            size = 28.dp,
            invert = armed
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (armed) "release to place on home" else "drag down to home",
            style = NPType.caption,
            color = if (armed) NPColors.paper else NPColors.ink
        )
    }
}

@Composable
private fun FilterField(
    value: String,
    onValueChange: (String) -> Unit,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NPColors.paper)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SigilGlyph(
            glyph = GlyphKind.Search,
            size = 24.dp,
            invert = true
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = NPType.bodyMd.copy(color = NPColors.ink),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "filter…",
                            style = NPType.bodyMd,
                            color = NPColors.inkDim
                        )
                    }
                    innerTextField()
                }
            }
        )
        Text(
            text = count.toString(),
            style = NPType.caption,
            color = NPColors.inkDim
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NPColors.inkHair)
    )
}
