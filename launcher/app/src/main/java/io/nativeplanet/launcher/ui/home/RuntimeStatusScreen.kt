package io.nativeplanet.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import io.nativeplanet.launcher.domain.model.RuntimeState
import io.nativeplanet.launcher.platform.GuestLauncher
import io.nativeplanet.launcher.platform.LauncherAppInfo
import io.nativeplanet.launcher.theme.NPColors
import io.nativeplanet.launcher.theme.NPSpacing
import io.nativeplanet.launcher.theme.NPType
import io.nativeplanet.launcher.theme.NativePlanetTheme
import io.nativeplanet.launcher.ui.components.NPButton
import io.nativeplanet.launcher.ui.components.NPButtonStyle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

private data class HomeDragPreview(
    val tile: HomeTileSpec,
    val position: Offset
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RuntimeStatusScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDetails: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuntimeStatusViewModel = hiltViewModel()
) {
    val colors = NativePlanetTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val displayShip = uiState.demoIdentity.shipName ?: uiState.bootPackageStatus.ship
    val displayParent = uiState.demoIdentity.parentName ?: uiState.bootPackageStatus.parent
    val shipLabel = displayShip?.withPatpSig()
    val parentLabel = displayParent?.withPatpSig()
    val isConfigured = uiState.demoIdentity.isConfigured || uiState.bootPackageStatus.exists
    val now = LocalTime.now()
    val hostPatp = parentLabel ?: shipLabel
    var homeTiles by remember(hostPatp) {
        mutableStateOf(HomeLayoutStore.load(context, hostPatp))
    }
    var editMode by remember { mutableStateOf(false) }
    var removeTargetArmed by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var dragPreview by remember { mutableStateOf<HomeDragPreview?>(null) }
    val pageCount = remember(homeTiles) {
        maxOf(2, (homeTiles.maxOfOrNull { it.page } ?: 0) + 2)
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    val stripText = buildList {
        add(uiState.runtimeStatus.state.name.lowercase())
        if (uiState.networkStatus.validated) add(uiState.networkStatus.type.name.lowercase()) else add("offline")
        if (uiState.runtimeStatus.connSockAvailable) add("conn") else add("waiting")
    }.joinToString(" · ")
    val identityText = buildList {
        shipLabel?.let { add(it) }
        parentLabel?.let { add("under $it") }
    }.joinToString(" · ")
    val homeSwipeModifier = Modifier.quickSwipeUpToOpenDrawer(
        enabled = isConfigured && !editMode,
        onSwipeUp = onNavigateToApps
    )

    LaunchedEffect(context, hostPatp) {
        homeTiles = HomeLayoutStore.load(context, hostPatp)
        HomeLayoutEvents.changes.collectLatest {
            homeTiles = HomeLayoutStore.load(context, hostPatp)
        }
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2600)
            notice = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isConfigured) NPColors.paper else colors.background)
            .navigationBarsPadding()
            .then(homeSwipeModifier)
    ) {
        StatusStrip(
            text = stripText.ifBlank { "whisper os" },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (isConfigured && editMode) {
            HomeEditRail(
                armed = removeTargetArmed,
                onReset = {
                    homeTiles = HomeLayoutStore.reset(context, hostPatp)
                    editMode = false
                    removeTargetArmed = false
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = NPSpacing.screenPadX)
                    .padding(top = 54.dp)
                    .zIndex(2f)
            )
        }

        if (!isConfigured) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NPSpacing.screenPadXWide)
                    .padding(top = NPSpacing.screenPadTop, bottom = NPSpacing.screenPadBottom)
            ) {
                Spacer(modifier = Modifier.weight(1f))
                EmptyWhisperState(onNavigateToOnboarding = onNavigateToOnboarding)
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NPSpacing.screenPadX)
                    .padding(top = 58.dp, bottom = 82.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            var dragTotal = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onVerticalDrag = { _, dragAmount -> dragTotal += dragAmount },
                                onDragEnd = {
                                    if (dragTotal > 72.dp.toPx()) {
                                        onNavigateToSearch()
                                    }
                                },
                                onDragCancel = { dragTotal = 0f }
                            )
                        }
                ) {
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("H:mm")),
                        style = NPType.displayLg,
                        color = NPColors.ink,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE · MMM d")).uppercase(),
                        style = NPType.caption,
                        color = NPColors.inkDim,
                        maxLines = 1
                    )
                    if (identityText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = identityText,
                            style = NPType.caption,
                            color = NPColors.inkDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                WorkspacePageIndicator(
                    page = pagerState.currentPage,
                    pageCount = pageCount,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    beyondBoundsPageCount = 1
                ) { page ->
                    val pageTiles = homeTiles
                        .filter { it.page == page }
                        .sortedBy { it.cell }

                    HomeWorkspacePage(
                        page = page,
                        pageCount = pageCount,
                        tiles = pageTiles,
                        onTileClick = { tile ->
                            launchHomeTile(
                                context = context,
                                tile = tile,
                                onNavigateToApps = onNavigateToApps,
                                onNavigateToSearch = onNavigateToSearch,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToDetails = onNavigateToDetails,
                                onNotice = { notice = it }
                            )
                        },
                        onMove = { fromTile, toTile ->
                            homeTiles = HomeLayoutStore.moveToCell(
                                tiles = homeTiles,
                                tileId = fromTile.id,
                                page = toTile.page,
                                cell = toTile.cell
                            )
                            HomeLayoutStore.save(context, homeTiles)
                        },
                        onMoveToCell = { tile, cell ->
                            homeTiles = HomeLayoutStore.moveToCell(homeTiles, tile.id, page, cell)
                            HomeLayoutStore.save(context, homeTiles)
                        },
                        onMoveToPage = { tile, targetPage ->
                            val clampedTarget = targetPage.coerceIn(0, pageCount - 1)
                            homeTiles = HomeLayoutStore.moveToPage(homeTiles, tile.id, clampedTarget)
                            HomeLayoutStore.save(context, homeTiles)
                            scope.launch {
                                pagerState.animateScrollToPage(clampedTarget)
                            }
                        },
                        onRemove = { tile ->
                            homeTiles = HomeLayoutStore.remove(context, tile.id, hostPatp)
                            editMode = false
                            removeTargetArmed = false
                        },
                        onDragState = { dragging ->
                            editMode = dragging
                            if (!dragging) {
                                removeTargetArmed = false
                                dragPreview = null
                            }
                        },
                        onRemoveHover = { armed -> removeTargetArmed = armed },
                        onDragPreview = { dragPreview = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (editMode) {
                LauncherNavPill(
                    color = NPColors.ink.copy(alpha = 0.18f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
            } else {
                FindAskBar(
                    text = "all apps, or ask…",
                    onClick = onNavigateToApps,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = NPSpacing.screenPadX)
                        .padding(bottom = 24.dp)
                        .pointerInput(Unit) {
                            var dragTotal = 0f
                            detectVerticalDragGestures(
                                onDragStart = { dragTotal = 0f },
                                onVerticalDrag = { _, dragAmount -> dragTotal += dragAmount },
                                onDragEnd = {
                                    if (dragTotal < -72.dp.toPx()) {
                                        onNavigateToApps()
                                    }
                                },
                                onDragCancel = { dragTotal = 0f }
                            )
                        }
                )
            }

            notice?.let { text ->
                HomeNotice(
                    text = text,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = NPSpacing.screenPadX)
                        .padding(bottom = 92.dp)
                        .zIndex(3f)
                    )
            }

            dragPreview?.let { preview ->
                HomeTileCell(
                    tile = preview.tile,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                preview.position.x.roundToInt(),
                                preview.position.y.roundToInt()
                            )
                        }
                        .graphicsLayer(
                            scaleX = 1.08f,
                            scaleY = 1.08f,
                            alpha = 0.96f
                        )
                        .zIndex(20f),
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun WorkspacePageIndicator(
    page: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == page) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == page) {
                            NPColors.ink.copy(alpha = 0.62f)
                        } else {
                            NPColors.ink.copy(alpha = 0.18f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun HomeWorkspacePage(
    page: Int,
    pageCount: Int,
    tiles: List<HomeTileSpec>,
    onTileClick: (HomeTileSpec) -> Unit,
    onMove: (from: HomeTileSpec, to: HomeTileSpec) -> Unit,
    onMoveToCell: (tile: HomeTileSpec, cell: Int) -> Unit,
    onMoveToPage: (tile: HomeTileSpec, targetPage: Int) -> Unit,
    onRemove: (HomeTileSpec) -> Unit,
    onDragState: (Boolean) -> Unit,
    onRemoveHover: (Boolean) -> Unit,
    onDragPreview: (HomeDragPreview?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tiles.isEmpty()) {
        EmptyWorkspacePage(
            page = page,
            modifier = modifier
        )
        return
    }

    val density = LocalDensity.current
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(text = if (page == 0) "home" else "page ${page + 1}")
        Spacer(modifier = Modifier.height(12.dp))
        var gridPosition by remember(page) { mutableStateOf(Offset.Zero) }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onGloballyPositioned { coordinates ->
                    gridPosition = coordinates.positionInRoot()
                }
        ) {
            val cellWidth = maxWidth / HomeLayoutStore.COLUMNS
            val cellHeight = maxHeight / HomeLayoutStore.ROWS
            val cellWidthPx = with(density) { cellWidth.toPx() }
            val cellHeightPx = with(density) { cellHeight.toPx() }
            val pageWidthPx = with(density) { maxWidth.toPx() }
            val appCellPx = with(density) { NPSpacing.appCell.toPx() }
            val removeThresholdPx = with(density) { -92.dp.toPx() }
            val pageEdgeThreshold = pageWidthPx * 0.42f

            fun cellForPosition(position: Offset): Int? {
                if (position.x < 0f || position.y < 0f) return null
                val col = floor(position.x / cellWidthPx).toInt()
                val row = floor(position.y / cellHeightPx).toInt()
                if (col !in 0 until HomeLayoutStore.COLUMNS ||
                    row !in 0 until HomeLayoutStore.ROWS
                ) {
                    return null
                }
                return row * HomeLayoutStore.COLUMNS + col
            }

            fun cellOrigin(cell: Int): Offset {
                val col = cell % HomeLayoutStore.COLUMNS
                val row = cell / HomeLayoutStore.COLUMNS
                val x = cellWidthPx * col + ((cellWidthPx - appCellPx) / 2f)
                val y = cellHeightPx * row
                return Offset(x, y)
            }

            Box(
                modifier = Modifier
                    .zIndex(50f)
                    .fillMaxSize()
                    .pointerInput(tiles, page, pageCount, cellWidthPx, cellHeightPx, pageWidthPx) {
                        val touchSlop = viewConfiguration.touchSlop
                        val touchSlopSquared = touchSlop * touchSlop

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val start = down.position
                            val startCell = cellForPosition(start) ?: return@awaitEachGesture
                            val tile = tiles.firstOrNull { it.cell == startCell } ?: return@awaitEachGesture
                            val startOrigin = cellOrigin(startCell)
                            var dragging = false
                            var totalDelta = Offset.Zero

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: return@awaitEachGesture

                                if (!change.pressed) {
                                    if (dragging) {
                                        if (totalDelta.y < removeThresholdPx) {
                                            onRemove(tile)
                                        } else if (totalDelta.x > pageEdgeThreshold && page < pageCount - 1) {
                                            onMoveToPage(tile, page + 1)
                                        } else if (totalDelta.x < -pageEdgeThreshold && page > 0) {
                                            onMoveToPage(tile, page - 1)
                                        } else {
                                            val targetCell = cellForPosition(start + totalDelta) ?: startCell
                                            if (targetCell != startCell) {
                                                val targetTile = tiles.firstOrNull { it.cell == targetCell }
                                                if (targetTile != null) {
                                                    onMove(tile, targetTile)
                                                } else {
                                                    onMoveToCell(tile, targetCell)
                                                }
                                            }
                                        }
                                        onDragState(false)
                                        onRemoveHover(false)
                                        onDragPreview(null)
                                    } else {
                                        onTileClick(tile)
                                    }
                                    return@awaitEachGesture
                                }

                                val current = change.position
                                totalDelta = current - start
                                val movedPastSlop =
                                    (totalDelta.x * totalDelta.x) + (totalDelta.y * totalDelta.y) > touchSlopSquared
                                val heldLongEnough =
                                    change.uptimeMillis - down.uptimeMillis >= viewConfiguration.longPressTimeoutMillis

                                if (!dragging && (movedPastSlop || heldLongEnough)) {
                                    dragging = true
                                    onDragState(true)
                                }

                                if (dragging) {
                                    change.consume()
                                    onDragPreview(HomeDragPreview(tile, gridPosition + startOrigin + totalDelta))
                                    onRemoveHover(totalDelta.y < removeThresholdPx)
                                }
                            }
                        }
                    }
            )

            tiles.forEach { tile ->
                val cell = tile.cell.coerceIn(0, HomeLayoutStore.PAGE_SIZE - 1)
                val col = cell % HomeLayoutStore.COLUMNS
                val row = cell / HomeLayoutStore.COLUMNS
                val x = cellWidth * col + ((cellWidth - NPSpacing.appCell) / 2)
                val y = cellHeight * row
                DraggableHomeTile(
                    tile = tile,
                    cell = cell,
                    page = page,
                    pageCount = pageCount,
                    pageTiles = tiles,
                    cellWidthPx = cellWidthPx,
                    cellHeightPx = cellHeightPx,
                    pageWidthPx = pageWidthPx,
                    onTileClick = onTileClick,
                    onMove = onMove,
                    onMoveToCell = onMoveToCell,
                    onMoveToPage = onMoveToPage,
                    onRemove = onRemove,
                    onDragState = onDragState,
                    onRemoveHover = onRemoveHover,
                    onDragPreview = onDragPreview,
                    modifier = Modifier.offset(x = x, y = y)
                )
            }
        }
    }
}

@Composable
private fun EmptyWorkspacePage(
    page: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SectionLabel(text = "page ${page + 1}")
        Spacer(modifier = Modifier.height(96.dp))
        Text(
            text = "empty home page",
            style = NPType.bodySm,
            color = NPColors.inkDim
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "drag apps here as the workspace grows",
            style = NPType.caption,
            color = NPColors.inkFaint
        )
    }
}

@Composable
private fun DraggableHomeTile(
    tile: HomeTileSpec,
    cell: Int,
    page: Int,
    pageCount: Int,
    pageTiles: List<HomeTileSpec>,
    cellWidthPx: Float,
    cellHeightPx: Float,
    pageWidthPx: Float,
    onTileClick: (HomeTileSpec) -> Unit,
    onMove: (from: HomeTileSpec, to: HomeTileSpec) -> Unit,
    onMoveToCell: (tile: HomeTileSpec, cell: Int) -> Unit,
    onMoveToPage: (tile: HomeTileSpec, targetPage: Int) -> Unit,
    onRemove: (HomeTileSpec) -> Unit,
    onDragState: (Boolean) -> Unit,
    onRemoveHover: (Boolean) -> Unit,
    onDragPreview: (HomeDragPreview?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(NPSpacing.appCell)
            .height(NPSpacing.appCell + 30.dp)
    ) {
        HomeTileCell(
            tile = tile,
            modifier = Modifier.align(Alignment.TopCenter),
            onClick = null
        )
    }
}

@Composable
private fun HomeEditRail(
    armed: Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (armed) NPColors.accentCoral else NPColors.ink.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (armed) "release to remove" else "drag here to remove",
            style = NPType.caption,
            color = if (armed) NPColors.paper else NPColors.ink
        )
        Text(
            text = "reset layout",
            style = NPType.caption,
            color = if (armed) NPColors.paper else NPColors.inkDim,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onReset)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HomeTileCell(
    tile: HomeTileSpec,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    val glyph = tile.glyph
    val tileAlpha = if (tile.action == HomeLayoutStore.ACTION_HOSTED_PENDING &&
        hostedPathForTile(tile) == null
    ) {
        0.44f
    } else {
        1f
    }
    if (glyph != null) {
        LauncherGridCell(
            label = tile.label,
            glyph = glyph,
            modifier = modifier.graphicsLayer(alpha = tileAlpha),
            hostPatp = tile.hostPatp,
            provenance = false,
            onClick = onClick
        )
    } else {
        Column(
            modifier = modifier
                .graphicsLayer(alpha = tileAlpha)
                .width(NPSpacing.appCell)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppGlyph(
                label = tile.label,
                world = tile.world,
                size = NPSpacing.appCell,
                packageName = tile.packageName
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tile.label,
                style = NPType.nano,
                color = NPColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun launchHomeTile(
    context: android.content.Context,
    tile: HomeTileSpec,
    onNavigateToApps: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetails: () -> Unit,
    onNotice: (String) -> Unit
) {
    when (tile.action) {
        "phone" -> GuestLauncher.launchDialer(context)
        "messages" -> {
            if (!GuestLauncher.launchMessaging(context)) {
                onNotice("messages is not available yet")
            }
        }
        "browser" -> GuestLauncher.launchBrowser(context)
        "camera" -> GuestLauncher.launchCamera(context)
        "settings" -> onNavigateToSettings()
        "apps" -> onNavigateToApps()
        "details" -> onNavigateToDetails()
        "search" -> onNavigateToSearch()
        HomeLayoutStore.ACTION_HOSTED_LOCAL,
        HomeLayoutStore.ACTION_HOSTED_PENDING -> {
            val path = hostedPathForTile(tile)
            if (path == null || !GuestLauncher.launchHostedApp(context, tile.label, LOCAL_EYRE_ORIGIN + path)) {
                onNotice("${tile.label} is not available yet")
            }
        }
        else -> {
            if (tile.packageName != null && tile.className != null) {
                val launched = GuestLauncher.launchApp(
                    context,
                    LauncherAppInfo(
                        label = tile.label,
                        packageName = tile.packageName,
                        className = tile.className
                    )
                )
                if (!launched) {
                    onNotice("${tile.label} is not available yet")
                }
            } else {
                onNotice("${tile.label} is not available yet")
            }
        }
    }
}

private const val LOCAL_EYRE_ORIGIN = "http://127.0.0.1:8080"

private fun hostedPathForTile(tile: HomeTileSpec): String? {
    tile.hostedPath?.takeIf { it.isNotBlank() }?.let { return it }
    return when (tile.id.removePrefix("hosted:")) {
        "tlon", "talk" -> "/apps/groups/"
        "dojo" -> "/apps/webterm/"
        "notes", "hark" -> "/apps/landscape/"
        else -> null
    }
}

private fun Modifier.quickSwipeUpToOpenDrawer(
    enabled: Boolean,
    onSwipeUp: () -> Unit
): Modifier {
    if (!enabled) return this
    return pointerInput(onSwipeUp) {
        val threshold = 96.dp.toPx()
        val maxHorizontalDrift = 96.dp.toPx()
        val maxGestureMs = 450L
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = down.position
            val startTime = down.uptimeMillis
            var triggered = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: event.changes.firstOrNull()
                if (change != null) {
                    val last = change.position
                    val elapsedMs = change.uptimeMillis - startTime
                    val deltaY = last.y - start.y
                    val deltaX = abs(last.x - start.x)
                    if (!triggered &&
                        elapsedMs <= maxGestureMs &&
                        deltaY < -threshold &&
                        deltaX < maxHorizontalDrift
                    ) {
                        triggered = true
                        onSwipeUp()
                    }
                }
                if (event.changes.none { it.pressed }) break
            }
        }
    }
}

@Composable
private fun HomeNotice(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NPColors.ink)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(NPColors.accentAmber)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = NPType.bodySm,
            color = NPColors.paper,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RuntimeDetailsScreen(
    onBack: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuntimeStatusViewModel = hiltViewModel()
) {
    val colors = NativePlanetTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val isConfigured = uiState.demoIdentity.isConfigured || uiState.bootPackageStatus.exists
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NPSpacing.screenGutter, vertical = NPSpacing.md)
    ) {
        StatusStrip(
            text = "settings · identity · ${uiState.runtimeStatus.version ?: "whisper os"}"
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        Text(
            text = "System details",
            style = NPType.displaySm,
            color = colors.foreground
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        NPButton(
            text = if (isConfigured) "Add identity" else "Set up satellite",
            onClick = onNavigateToOnboarding,
            style = if (isConfigured) NPButtonStyle.SECONDARY else NPButtonStyle.FILLED
        )

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        RuntimeDetailsPanel(runtimeStatus = uiState.runtimeStatus)

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        BootPackagePanel(bootPackageStatus = uiState.bootPackageStatus)

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        NetworkStatusPanel(networkStatus = uiState.networkStatus)

        Spacer(modifier = Modifier.height(NPSpacing.lg))

        DiagnosticsPanel(diagnostics = uiState.diagnostics)

        Spacer(modifier = Modifier.height(NPSpacing.xl))

        LauncherNavPill(
            color = colors.foreground.copy(alpha = 0.28f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(NPSpacing.lg))
    }
}

@Composable
private fun EmptyWhisperState(onNavigateToOnboarding: () -> Unit) {
    val colors = NativePlanetTheme.colors

    Column {
        Text(
            text = "No satellite on this phone.",
            style = NPType.displaySm,
            color = colors.foreground
        )
        Spacer(modifier = Modifier.height(NPSpacing.md))
        Text(
            text = "Pair with your planet or import a satellite.",
            style = NPType.bodySm,
            color = colors.foregroundDim
        )
        Spacer(modifier = Modifier.height(NPSpacing.xl))
        NPButton(
            text = "Set up satellite",
            onClick = onNavigateToOnboarding
        )
    }
}

@Composable
private fun WhisperClock(
    time: LocalTime,
    subline: String,
    large: Boolean
) {
    val colors = NativePlanetTheme.colors

    Column {
        Text(
            text = time.format(DateTimeFormatter.ofPattern("H:mm")),
            style = if (large) {
                NPType.displayLg.copy(fontSize = 88.sp, lineHeight = (88 * 0.92).sp)
            } else {
                NPType.displayLg.copy(fontSize = 64.sp, lineHeight = (64 * 0.92).sp)
            },
            color = colors.foreground,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(NPSpacing.xs))
        Text(
            text = subline,
            style = NPType.caption,
            color = colors.foregroundDim
        )
    }
}

@Composable
private fun WhisperItem(
    from: String,
    line: String,
    meta: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = NativePlanetTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.hairline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .padding(top = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                if (active) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(NPColors.accentAmber)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.foregroundFaint)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = from,
                    style = NPType.bodySm,
                    color = colors.foreground,
                    maxLines = 1
                )
                Text(
                    text = line,
                    style = NPType.bodySm,
                    color = colors.foregroundDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(NPSpacing.md))

            Text(
                text = meta,
                style = NPType.nano,
                color = colors.foregroundFaint,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WhisperCommandBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NativePlanetTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.foreground.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(NPColors.accentAmber)
        )
        Spacer(modifier = Modifier.width(NPSpacing.md))
        Text(
            text = text,
            style = NPType.bodySm,
            color = colors.foregroundDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
