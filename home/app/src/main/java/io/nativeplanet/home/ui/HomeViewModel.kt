package io.nativeplanet.home.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.nativeplanet.home.data.Calendar
import io.nativeplanet.home.data.Controller
import io.nativeplanet.home.data.Eyre
import io.nativeplanet.home.data.NotificationStore
import io.nativeplanet.home.data.Prefs
import io.nativeplanet.home.data.Ship
import io.nativeplanet.home.data.Tools
import io.nativeplanet.home.model.Entry
import io.nativeplanet.home.model.HostedApp
import io.nativeplanet.home.model.Next
import io.nativeplanet.home.model.Person
import io.nativeplanet.home.model.Source
import io.nativeplanet.home.model.Tool
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** How often the home clock re-reads the time. */
private const val CLOCK_TICK_MS = 15_000L
/** How often the controller and ship are polled. */
private const val REFRESH_MS = 30_000L
/** A reach line must be newer than this. */
private const val REACH_WINDOW_MS = 24 * 60 * 60 * 1000L
/** A message this recent counts as reach even when the thread reads as read. */
private const val REACH_FRESH_MS = 3 * 60 * 60 * 1000L
/** Wait for the planet to relay a sent DM before refreshing. */
private const val SEND_SETTLE_MS = 2500L
/** Wait for a group join to land before refreshing. */
private const val JOIN_SETTLE_MS = 4000L

enum class Surface { HOME, LATER, PEOPLE, PERSON, TYPE, SETTINGS }

data class UiState(
    val nowMs: Long = System.currentTimeMillis(),
    val self: String? = null,
    val runtime: String = "unknown",
    val connected: Boolean = false,
    val next: Next? = null,
    val reach: Entry? = null,
    val toolWords: List<String> = emptyList(),
    val tools: List<Tool> = emptyList(),
    val hosted: List<HostedApp> = emptyList(),
    val later: List<Entry> = emptyList(),
    val folded: Int = 0,
    val shipEntries: List<Entry> = emptyList(),
    val people: List<Person> = emptyList(),
    val reachShips: Set<String> = emptySet(),
    val mutedShips: Set<String> = emptySet(),
    val surface: Surface = Surface.HOME,
    val person: Person? = null,
    val query: String = "",
    val toast: String? = null,
    val delegated: Boolean = false,
    val parent: String? = null,
    val parentGroups: List<Ship.GroupRef> = emptyList(),
    val moonGroups: Set<String> = emptySet(),
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val controller = Controller(app)
    private val eyre = Eyre()
    private val ship = Ship(eyre)
    private val calendar = Calendar(app)
    private val prefs = Prefs(app)
    val tools = Tools(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var unreadDm: Set<String> = emptySet()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            combine(prefs.reachShips, prefs.mutedShips, prefs.doneIds, prefs.toolOrder, NotificationStore.entries) { r, m, d, t, n ->
                Prefs5(r, m, d, t, n)
            }.collect { p ->
                _state.update { it.copy(reachShips = p.reach, mutedShips = p.muted, toolWords = p.tools) }
                recompute(p.done, p.notifs)
            }
        }
        viewModelScope.launch { while (isActive) { _state.update { it.copy(nowMs = System.currentTimeMillis()) }; delay(CLOCK_TICK_MS) } }
        startRefreshing()
    }

    private data class Prefs5(val reach: Set<String>, val muted: Set<String>, val done: Set<String>, val tools: List<String>, val notifs: List<Entry>)
    private var lastDone: Set<String> = emptySet()
    private var lastNotifs: List<Entry> = emptyList()

    fun startRefreshing() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) { refresh(); delay(REFRESH_MS) }
        }
    }

    private val refreshing = Mutex()

    suspend fun refresh() = refreshing.withLock { refreshLocked() }

    private suspend fun refreshLocked() {
        val rt = controller.runtime()
        _state.update { it.copy(runtime = rt?.state ?: "unavailable", self = rt?.shipName ?: it.self) }
        val hosted = controller.hostedApps()
        _state.update { it.copy(hosted = hosted, tools = tools.all(hosted)) }
        _state.update { it.copy(next = calendar.next()) }
        if (rt?.state == "running") {
            if (!eyre.loggedIn) controller.webLoginCode()?.let { eyre.login(it) }
            if (eyre.loggedIn) {
                val snap = ship.snapshot(rt.shipName)
                unreadDm = snap.unreadDm
                _state.update { it.copy(shipEntries = snap.entries, people = snap.people, connected = true, delegated = snap.delegated, parent = snap.parent,
                    parentGroups = snap.parentGroups, moonGroups = snap.moonGroups) }
            } else _state.update { it.copy(connected = false) }
        } else _state.update { it.copy(connected = false) }
        recompute(lastDone, lastNotifs)
    }

    private fun recompute(done: Set<String>, notifs: List<Entry>) {
        lastDone = done; lastNotifs = notifs
        val s = _state.value
        val all = (s.shipEntries + notifs)
            .filter { it.id !in done }
            .filter { it.ship == null || it.ship !in s.mutedShips }
            .sortedByDescending { it.timeMs }
        val reach = all.firstOrNull { isReachWorthy(it) }
        val (loud, quiet) = all.partition { it.source !in Source.PHONE || it.priority }
        _state.update { it.copy(later = loud + quiet, folded = quiet.size, reach = reach) }
    }

    /** A recent DM from someone on the reach list, unread or fresh enough to count. */
    private fun isReachWorthy(e: Entry): Boolean {
        val s = _state.value
        val now = System.currentTimeMillis()
        return e.ship != null && e.ship in s.reachShips && e.ship != s.self && e.source == Source.MESSAGE &&
            e.timeMs > now - REACH_WINDOW_MS &&
            (unreadDm.contains(e.link?.substringAfterLast('/') ?: "") || e.timeMs > now - REACH_FRESH_MS)
    }

    fun show(surface: Surface) = _state.update { it.copy(surface = surface, query = if (surface == Surface.TYPE) it.query else "") }
    fun home() = _state.update { it.copy(surface = Surface.HOME, person = null, query = "") }
    fun openPerson(p: Person) = _state.update { it.copy(surface = Surface.PERSON, person = p) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun toast(msg: String?) = _state.update { it.copy(toast = msg) }

    fun entriesFor(p: Person): List<Entry> = _state.value.shipEntries.filter { it.ship == p.ship || it.link?.endsWith(p.ship) == true }.sortedByDescending { it.timeMs }

    fun openWord(word: String) {
        val s = _state.value
        if (word == "people") { show(Surface.PEOPLE); return }
        val t = tools.resolveHomeWord(word, s.tools)
        if (t == null || (t.key.startsWith("home:") && t.key != "home:people") || !tools.open(t)) toast("nothing opens $word yet")
    }

    fun openTool(t: Tool) { if (!tools.open(t)) toast("${t.name} cannot open yet") }

    fun openEntry(e: Entry) {
        val s = _state.value
        if (e.packageName != null) { if (!tools.openPackage(e.packageName)) toast("cannot open"); return }
        // Ship entries open Tlon through the hosted path, at the DM when we know it.
        val groups = s.hosted.firstOrNull { it.desk == "groups" } ?: run { toast("messages app is not ready"); return }
        val target = if (e.link != null) groups.copy(startUrl = eyreUrl(e.link)) else groups
        if (!tools.open(Tool("desk:groups", "tlon", true, hosted = target))) toast("cannot open")
    }

    private fun eyreUrl(path: String) = eyre.url(path)

    /** Send a DM. With delegation the planet sends it; otherwise the moon's own chat app opens. */
    fun sendDm(p: Person, text: String) {
        val s = _state.value
        if (!s.delegated) { openEntry(Entry("open:${p.ship}", 0, p.display, p.ship, "", Source.MESSAGE, link = "apps/groups/dm/${p.ship}")); return }
        val self = s.self ?: run { toast("no ship"); return }
        viewModelScope.launch {
            if (ship.sendDm(self, p.ship, text)) { toast("sent as ${s.parent}"); delay(SEND_SETTLE_MS); refresh() } else toast("send failed")
        }
    }

    /** Join one of the planet's groups; the relay invites the moon first when the planet hosts it. */
    fun joinGroup(g: Ship.GroupRef) {
        val self = _state.value.self ?: run { toast("no ship"); return }
        viewModelScope.launch {
            toast("joining ${g.title}…")
            if (ship.joinGroup(self, g)) { delay(JOIN_SETTLE_MS); refresh(); toast(if (g.flag in _state.value.moonGroups) "joined ${g.title}" else "asked to join ${g.title}") }
            else toast("could not join ${g.title}")
        }
    }

    fun markDone(e: Entry) = viewModelScope.launch { prefs.markDone(e.id) }
    fun mute(shipName: String, muted: Boolean) = viewModelScope.launch { prefs.setMuted(shipName, muted) }
    fun setReach(shipName: String, can: Boolean) = viewModelScope.launch { prefs.setReach(shipName, can) }
    fun setToolWords(words: List<String>) = viewModelScope.launch { prefs.setTools(words) }
}
