package io.nativeplanet.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.nativeplanet.launcher.domain.model.HostedApp
import io.nativeplanet.launcher.platform.GuestLauncher
import io.nativeplanet.launcher.platform.HostedWebActivity
import io.nativeplanet.launcher.theme.NPColors
import io.nativeplanet.launcher.theme.NPSpacing
import io.nativeplanet.launcher.theme.NPType
import io.nativeplanet.launcher.ui.components.GlyphKind
import io.nativeplanet.launcher.ui.components.Sigil
import io.nativeplanet.launcher.ui.components.SigilGlyph

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RuntimeStatusViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val apps = remember(context) { GuestLauncher.installedApps(context) }
    var query by remember { mutableStateOf("") }
    var closeDragTotal by remember { mutableStateOf(0f) }
    val closeThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val resultsState = rememberLazyListState()
    val closeScrollConnection = remember(closeThreshold, resultsState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val resultsAtTop = resultsState.firstVisibleItemIndex == 0 &&
                    resultsState.firstVisibleItemScrollOffset == 0
                if (!resultsAtTop) {
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

    val appResults = remember(apps, query) {
        val q = query.trim()
        val filtered = if (q.isEmpty()) {
            apps.take(8)
        } else {
            apps.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }
        filtered.take(16)
    }
    val hostedResults = remember(uiState.hostedApps, query) {
        val q = query.trim()
        val filtered = uiState.hostedApps
            .filter { it.isLaunchable }
            .filter {
                q.isEmpty() ||
                    it.title.contains(q, ignoreCase = true) ||
                    it.desk.contains(q, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<HostedApp> { it.recommended }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
        filtered.take(8)
    }
    val people = listOfNotNull(
        uiState.runtimeStatus.shipName?.withPatpSig()?.let { PersonHit("your satellite", it, "this phone") },
        uiState.bootPackageStatus.parent?.withPatpSig()?.let { PersonHit("your planet", it, "root identity") }
    ).filter {
        query.isBlank() || it.label.contains(query, ignoreCase = true) || it.patp.contains(query, ignoreCase = true)
    }
    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NPColors.deep)
            .navigationBarsPadding()
            .pointerInput(Unit) {
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
    ) {
        StatusStrip(
            text = "search · ${hostedResults.size + appResults.size + people.size} hits across ${listOf(hostedResults.isNotEmpty(), appResults.isNotEmpty(), people.isNotEmpty()).count { it }} kinds",
            inverted = true,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = NPSpacing.screenPadX)
                .padding(top = 64.dp, bottom = 24.dp)
        ) {
            SearchInput(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                state = resultsState,
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(closeScrollConnection)
            ) {
                if (hostedResults.isNotEmpty()) {
                    item {
                        SectionLabel(text = "urbit apps · ${hostedResults.size}", dark = true)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    items(hostedResults, key = { "hosted:${it.id}" }) { app ->
                        AppSearchRow(
                            name = app.title,
                            sub = "urbit · ${app.desk}",
                            world = AppWorld.URBIT,
                            glyph = glyphForHostedSearchApp(app),
                            packageName = null,
                            onClick = {
                                GuestLauncher.launchHostedApp(
                                    context,
                                    app.title,
                                    HostedWebActivity.LOCAL_EYRE_ORIGIN + app.basePath
                                )
                            }
                        )
                    }
                }

                if (appResults.isNotEmpty()) {
                    item {
                        if (hostedResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                        SectionLabel(text = "apps · ${appResults.size}", dark = true)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    items(appResults, key = { "${it.packageName}/${it.className}" }) { app ->
                        AppSearchRow(
                            name = app.label,
                            sub = app.originLabel(),
                            world = app.world(),
                            glyph = glyphForApp(app),
                            packageName = app.packageName,
                            onClick = { GuestLauncher.launchApp(context, app) }
                        )
                    }
                }

                if (people.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(18.dp))
                        SectionLabel(text = "people · ${people.size}", dark = true)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    items(people, key = { it.patp }) { person ->
                        PersonSearchRow(person = person)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    SectionLabel(text = "messages · 0", dark = true)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "message index is not connected yet",
                        style = NPType.bodySm,
                        color = NPColors.paperDim
                    )
                }
            }

            LauncherNavPill(
                color = NPColors.paper.copy(alpha = 0.32f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private data class PersonHit(
    val label: String,
    val patp: String,
    val meta: String
)

private fun glyphForHostedSearchApp(app: HostedApp): GlyphKind {
    return when (app.id.lowercase()) {
        "groups", "tlon", "talk" -> GlyphKind.Messages
        "webterm", "dojo" -> GlyphKind.Dojo
        "grove", "files" -> GlyphKind.Files
        "kin", "memex" -> GlyphKind.Memex
        "landscape" -> GlyphKind.Browser
        "hark" -> GlyphKind.Hark
        else -> GlyphKind.Browser
    }
}

@Composable
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NPColors.paper.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SigilGlyph(
            glyph = GlyphKind.Search,
            size = 24.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = NPType.bodyMd.copy(color = NPColors.paper),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "find an app, or ask…",
                            style = NPType.bodyMd,
                            color = NPColors.paperDim
                        )
                    }
                    innerTextField()
                }
            }
        )
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .background(NPColors.accentAmber)
        )
    }
}

@Composable
private fun AppSearchRow(
    name: String,
    sub: String,
    world: AppWorld,
    glyph: GlyphKind?,
    packageName: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(44.dp)
                .background(world.color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (glyph != null) {
            SigilGlyph(glyph = glyph, size = 40.dp)
        } else {
            AppGlyph(label = name, world = world, packageName = packageName)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = NPType.bodyMd,
                color = NPColors.paper,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                style = NPType.nano,
                color = world.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NPColors.paperHair)
    )
}

@Composable
private fun PersonSearchRow(person: PersonHit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Sigil(patp = person.patp, size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.label,
                style = NPType.bodyMd,
                color = NPColors.paper,
                maxLines = 1
            )
            Text(
                text = "${person.patp} · ${person.meta}",
                style = NPType.nano,
                color = NPColors.paperDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NPColors.paperHair)
    )
}
