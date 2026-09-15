package io.nativeplanet.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import io.nativeplanet.home.model.Entry
import io.nativeplanet.home.model.Person
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PAD = 22.dp

@Composable
fun Root(vm: HomeViewModel) {
    val s by vm.state.collectAsState()
    Box(Modifier.fillMaxSize().background(W.Ink)) {
        when (s.surface) {
            Surface.HOME -> Home(vm, s)
            Surface.LATER -> Later(vm, s)
            Surface.PEOPLE -> People(vm, s)
            Surface.PERSON -> PersonPage(vm, s)
            Surface.TYPE -> TypeSurface(vm, s)
            Surface.SETTINGS -> Settings(vm, s)
        }
        s.toast?.let { msg ->
            LaunchedEffect(msg) { kotlinx.coroutines.delay(2200); vm.toast(null) }
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp).background(W.Paper).padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(msg, color = W.Ink, fontFamily = W.Mono, fontSize = 12.sp)
            }
        }
    }
}

// ---------- shared bits ----------

@Composable private fun Mono(text: String, color: Color = W.Paper60, size: Int = 11, tracking: Double = 0.12, modifier: Modifier = Modifier) =
    Text(text, color = color, fontFamily = W.Mono, fontSize = size.sp, letterSpacing = tracking.em, modifier = modifier)

@Composable private fun Hairline() = Box(Modifier.fillMaxWidth().height(1.dp).background(W.Hair))
@Composable private fun Rule() = Box(Modifier.fillMaxWidth().height(1.dp).background(W.Paper))

private fun clock(ms: Long): String = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(ms))
private fun dayLine(ms: Long): String = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(ms)).uppercase()
private fun hm(ms: Long): String = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(ms))
private fun ago(now: Long, then: Long): String {
    val m = ((now - then) / 60000L).coerceAtLeast(0)
    return when { m < 1 -> "now"; m < 60 -> "${m}m"; m < 60 * 24 -> "${m / 60}h"; else -> "${m / (60 * 24)}d" }
}

// ---------- HOME ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Home(vm: HomeViewModel, s: UiState) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -120f) vm.show(Surface.LATER) else if (total > 120f) vm.show(Surface.TYPE) },
                    onVerticalDrag = { _, dy -> total += dy }
                )
            }
            .padding(horizontal = PAD)
    ) {
        Spacer(Modifier.height(64.dp))
        Text(clock(s.nowMs), color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 112.sp, lineHeight = 100.sp, letterSpacing = (-0.04).em,
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { vm.show(Surface.SETTINGS) }))
        Mono(dayLine(s.nowMs), size = 11, tracking = 0.14, modifier = Modifier.padding(top = 10.dp))

        Column(Modifier.padding(top = 30.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            s.next?.let { n ->
                Row(Modifier.clickable { vm.openWord("calendar") }) {
                    Mono(hm(n.atMs), color = W.Paper60, size = 13, tracking = 0.0, modifier = Modifier.width(56.dp))
                    Mono(n.title, color = W.Paper, size = 13, tracking = 0.0)
                }
            }
            s.reach?.let { e ->
                Row(Modifier.clickable { vm.openEntry(e) }) {
                    Mono(e.who.take(9), color = W.Paper60, size = 13, tracking = 0.0, modifier = Modifier.width(56.dp))
                    Mono(e.text, color = W.Paper, size = 13, tracking = 0.0)
                }
            }
        }
        if (s.next != null || s.reach != null) { Spacer(Modifier.height(26.dp)); Hairline() }
        Spacer(Modifier.height(12.dp))
        s.toolWords.forEach { word ->
            Text(word.replaceFirstChar { it.uppercase() }, color = W.Paper, fontFamily = W.Serif, fontSize = 22.sp,
                modifier = Modifier.fillMaxWidth().height(46.dp).clickable { vm.openWord(word) }.padding(top = 10.dp))
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth().clickable { vm.show(Surface.LATER) }.padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Mono(if (s.folded > 0 || s.later.isNotEmpty()) "LATER · ${s.later.size}" else "LATER", color = W.Paper40, size = 10, tracking = 0.14)
            Box(Modifier.padding(top = 8.dp).width(28.dp).height(2.dp).background(W.Paper40))
        }
        if (!s.connected) Mono(if (s.runtime == "running") "connecting to your server" else "server · ${s.runtime}", color = W.Paper40, size = 10, tracking = 0.1, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp))
    }
}

// ---------- LATER ----------

@Composable
private fun Later(vm: HomeViewModel, s: UiState) {
    BackHandler { vm.home() }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = PAD)) {
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Later", color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 28.sp, modifier = Modifier.clickable { vm.home() })
            Mono("TODAY", size = 11)
        }
        Rule()
        val loud = s.later.filter { it.source != "ANDROID" || it.priority }
        val quiet = s.later.filter { it.source == "ANDROID" && !it.priority }
        LazyColumn(Modifier.weight(1f)) {
            items(loud, key = { it.id }) { e -> EntryRow(vm, s, e) }
            if (quiet.isNotEmpty()) item {
                var open by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${quiet.size} more, mostly from Play apps", color = W.Paper60, fontFamily = W.Serif, fontSize = 15.sp)
                    Mono(if (open) "–" else "›", size = 12)
                }
                if (open) Column { quiet.forEach { e -> EntryRow(vm, s, e) } }
            }
            if (s.later.isEmpty()) item { Text("Nothing waiting.", color = W.Paper60, fontFamily = W.Serif, fontSize = 16.sp, modifier = Modifier.padding(vertical = 24.dp)) }
        }
        Hairline()
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Mono("TAP · OPEN   HOLD · DONE", color = W.Paper40, size = 10)
            Mono("PULL DOWN · HOME", color = W.Paper40, size = 10)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(vm: HomeViewModel, s: UiState, e: Entry) {
    Column(Modifier.fillMaxWidth().combinedClickable(onClick = { vm.openEntry(e) }, onLongClick = { vm.markDone(e); vm.toast("done") }).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(e.who, color = W.Paper, fontFamily = W.Serif, fontSize = 16.sp)
            Mono(ago(s.nowMs, e.timeMs), size = 10, tracking = 0.0)
        }
        Text(e.text, color = W.Paper90, fontFamily = W.Serif, fontSize = 16.sp, lineHeight = 22.sp, maxLines = 3)
        Mono(e.source, color = W.Paper40, size = 10, tracking = 0.08)
    }
    Hairline()
}

// ---------- PEOPLE / PERSON ----------

@Composable
private fun People(vm: HomeViewModel, s: UiState) {
    BackHandler { vm.home() }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = PAD)) {
        Spacer(Modifier.height(22.dp))
        Text("People", color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 28.sp, modifier = Modifier.padding(bottom = 14.dp).clickable { vm.home() })
        Rule()
        LazyColumn {
            items(s.people, key = { it.ship }) { p ->
                Row(Modifier.fillMaxWidth().clickable { vm.openPerson(p) }.padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(p.display, color = W.Paper, fontFamily = W.Serif, fontSize = 18.sp)
                        if (p.nickname != null) Mono(p.ship, color = W.Paper40, size = 10, tracking = 0.0, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (p.ship in s.reachShips) Mono("REACH", size = 10) else if (p.ship in s.mutedShips) Mono("MUTED", color = W.Paper40, size = 10)
                }
                Hairline()
            }
            if (s.people.isEmpty()) item { Text(if (s.connected) "Nobody yet." else "Connecting to your server.", color = W.Paper60, fontFamily = W.Serif, fontSize = 16.sp, modifier = Modifier.padding(vertical = 24.dp)) }
        }
    }
}

@Composable
private fun PersonPage(vm: HomeViewModel, s: UiState) {
    val p = s.person ?: return
    BackHandler { vm.show(Surface.PEOPLE) }
    val entries = remember(p, s.later) { vm.entriesFor(p) }
    val canReach = p.ship in s.reachShips
    val muted = p.ship in s.mutedShips
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = PAD)) {
        Spacer(Modifier.height(22.dp))
        Text(p.display, color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 30.sp, modifier = Modifier.clickable { vm.show(Surface.PEOPLE) })
        Mono(p.ship + when { canReach -> " · can reach you"; muted -> " · muted"; else -> "" }, color = W.Paper40, size = 11, tracking = 0.0, modifier = Modifier.padding(top = 4.dp))
        Row(Modifier.padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Mono("MESSAGE", color = W.Paper, modifier = Modifier.clickable { vm.openEntry(Entry("open:${p.ship}", 0, p.display, p.ship, "", "MESSAGE", link = "apps/groups/dm/${p.ship}")) })
            Mono(if (canReach) "REACH · ON" else "REACH", color = W.Paper, modifier = Modifier.clickable { vm.setReach(p.ship, !canReach) })
            Mono(if (muted) "UNMUTE" else "MUTE", color = W.Paper, modifier = Modifier.clickable { vm.mute(p.ship, !muted) })
        }
        Spacer(Modifier.height(16.dp)); Rule()
        Mono("EVERYTHING BETWEEN YOU", size = 10, tracking = 0.14, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))
        LazyColumn {
            items(entries, key = { it.id }) { e ->
                Row(Modifier.fillMaxWidth().clickable { vm.openEntry(e) }.padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(e.text, color = W.Paper, fontFamily = W.Serif, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(end = 12.dp), maxLines = 2)
                    Mono("${e.source} · ${ago(s.nowMs, e.timeMs)}", size = 10, tracking = 0.0)
                }
                Hairline()
            }
            if (entries.isEmpty()) item { Text("Nothing yet.", color = W.Paper60, fontFamily = W.Serif, fontSize = 16.sp, modifier = Modifier.padding(vertical = 24.dp)) }
        }
    }
}

// ---------- TYPE ----------

@Composable
private fun TypeSurface(vm: HomeViewModel, s: UiState) {
    BackHandler { vm.home() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val q = s.query.trim().lowercase()
    val people = if (q.isEmpty()) emptyList() else s.people.filter { it.display.lowercase().contains(q) || it.ship.contains(q) }.take(3)
    val apps = if (q.isEmpty()) s.tools else s.tools.filter { it.name.contains(q) }.take(6)
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding().padding(horizontal = PAD)) {
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Mono(">", color = W.Paper60, size = 20, tracking = 0.0, modifier = Modifier.padding(end = 10.dp))
            BasicTextField(
                value = s.query, onValueChange = vm::setQuery, singleLine = true,
                textStyle = TextStyle(color = W.Paper, fontFamily = W.Mono, fontSize = 20.sp),
                cursorBrush = SolidColor(W.Paper),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { (people.firstOrNull()?.let { vm.openPerson(it) } ?: apps.firstOrNull()?.let { vm.openTool(it) }) }),
                modifier = Modifier.weight(1f).focusRequester(focus)
            )
        }
        Rule()
        LazyColumn(Modifier.weight(1f)) {
            items(people, key = { "p:" + it.ship }) { p ->
                Row(Modifier.fillMaxWidth().clickable { vm.openPerson(p) }.padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.display, color = W.Paper, fontFamily = W.Serif, fontSize = 18.sp); Mono("PERSON", size = 10)
                }
                Hairline()
            }
            if (q.isEmpty()) item { Mono("EVERY APP", color = W.Paper40, size = 10, tracking = 0.14, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)) }
            items(apps, key = { "a:" + it.key }) { t ->
                Row(Modifier.fillMaxWidth().clickable { vm.openTool(t) }.height(40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Mono(t.name, color = if (q.isEmpty()) W.Paper60 else W.Paper, size = 14, tracking = 0.0)
                    if (t.sendable) Mono("sendable", color = W.Paper40, size = 10, tracking = 0.0)
                }
            }
            if (q.isNotEmpty() && people.isEmpty() && apps.isEmpty()) item { Text("Nothing called that.", color = W.Paper60, fontFamily = W.Serif, fontSize = 16.sp, modifier = Modifier.padding(vertical = 24.dp)) }
        }
    }
}

// ---------- SETTINGS ----------

@Composable
private fun Settings(vm: HomeViewModel, s: UiState) {
    BackHandler { vm.home() }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val all = listOf("messages", "people", "things", "camera", "notes", "maps", "calls", "browser", "calendar", "terminal")
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = PAD)) {
        Spacer(Modifier.height(22.dp))
        Text("Settings", color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 28.sp, modifier = Modifier.padding(bottom = 14.dp).clickable { vm.home() })
        Rule()
        LazyColumn {
            item { Mono("SERVER", size = 10, tracking = 0.14, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)) }
            item { Text("${s.self ?: "no ship"} · ${s.runtime}" + if (s.connected) " · connected" else "", color = W.Paper90, fontFamily = W.Serif, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp)) }
            item { Mono("TOOLS ON THE HOME SCREEN", size = 10, tracking = 0.14, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)) }
            items(all) { w ->
                val on = w in s.toolWords
                Row(Modifier.fillMaxWidth().clickable { vm.setToolWords(if (on) s.toolWords - w else s.toolWords + w) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(w.replaceFirstChar { it.uppercase() }, color = if (on) W.Paper else W.Paper60, fontFamily = W.Serif, fontSize = 18.sp)
                    Mono(if (on) "ON" else "OFF", color = if (on) W.Paper else W.Paper40, size = 10)
                }
                Hairline()
            }
            item { Mono("WHO CAN REACH YOU", size = 10, tracking = 0.14, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)) }
            items(s.people, key = { it.ship }) { p ->
                val on = p.ship in s.reachShips
                Row(Modifier.fillMaxWidth().clickable { vm.setReach(p.ship, !on) }.padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.display, color = if (on) W.Paper else W.Paper60, fontFamily = W.Serif, fontSize = 18.sp)
                    Mono(if (on) "REACH" else "QUIET", color = if (on) W.Paper else W.Paper40, size = 10)
                }
                Hairline()
            }
            item { Mono("PHONE", size = 10, tracking = 0.14, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp)) }
            item { Text("Notification access", color = W.Paper90, fontFamily = W.Serif, fontSize = 16.sp, modifier = Modifier.clickable {
                ctx.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }.padding(vertical = 8.dp)) }
            item { Text("Default home app", color = W.Paper90, fontFamily = W.Serif, fontSize = 16.sp, modifier = Modifier.clickable {
                ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }.padding(vertical = 8.dp)) }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
