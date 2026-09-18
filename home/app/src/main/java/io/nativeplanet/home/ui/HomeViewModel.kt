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

/** How long setup waits for a ship to stop or start before giving up on the wait (not on the ship). */
private const val RUNTIME_WAIT_MS = 90_000L
/** A fresh moon dawns over Ames on first boot; give it longer. */
private const val SETUP_BOOT_WAIT_MS = 10 * 60_000L
private const val RUNTIME_POLL_MS = 3_000L
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

enum class Surface { HOME, LATER, PEOPLE, PERSON, TYPE, SETTINGS, SERVER, SETUP }

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
    /** False only when the controller itself is missing; a stopped ship still has a controller. */
    val controllerAvailable: Boolean = true,
    val bootPackage: Controller.BootPackage? = null,
    val network: Controller.Network? = null,
    val diagnostics: Controller.Diagnostics? = null,
    val runtimeVersion: String? = null,
    val connSock: Boolean = false,
    val setup: SetupState = SetupState(),
)

/** The setup flow: which page, what is in flight, and what it ended with. */
data class SetupState(
    val page: SetupPage = SetupPage.WELCOME,
    val busy: Boolean = false,
    val status: String? = null,
    val error: String? = null,
    val ship: String? = null,
    val parent: String? = null,
    /** Set when setup replaces a ship that was already provisioned. */
    val replacing: Boolean = false,
)

enum class SetupPage { WELCOME, PAIR, IMPORT, REVEAL }

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
        val bp = controller.bootPackage()
        _state.update {
            it.copy(
                runtime = rt?.state ?: "unavailable", self = rt?.shipName ?: bp?.ship ?: it.self,
                controllerAvailable = controller.available, bootPackage = bp,
                runtimeVersion = rt?.version, connSock = rt?.connSock ?: false,
            )
        }
        // A phone with a controller and no identity goes to setup, once, and stays there until it has one.
        if (controller.available && bp != null && !bp.exists && _state.value.surface == Surface.HOME && !_state.value.setup.busy) {
            _state.update { it.copy(surface = Surface.SETUP, setup = SetupState()) }
        }
        if (_state.value.surface == Surface.SERVER) {
            val net = controller.network(); val diag = controller.diagnostics()
            _state.update { it.copy(network = net, diagnostics = diag) }
        }
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
    fun settings() = _state.update { it.copy(surface = Surface.SETTINGS) }
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

    // ---- Server page and setup flow ----

    fun openServer() = viewModelScope.launch {
        _state.update { it.copy(surface = Surface.SERVER) }
        refresh()
    }

    fun startRuntime() = viewModelScope.launch {
        val r = controller.startRuntime()
        toast(if (r.accepted) "starting" else r.message ?: r.code)
        delay(SEND_SETTLE_MS); refresh()
    }

    /** Graceful stop: the ship is asked to exit; the state line shows whether it did. */
    fun stopRuntime() = viewModelScope.launch {
        val r = controller.stopRuntime()
        toast(if (r.accepted) "asked the ship to stop" else r.message ?: r.code)
        delay(SEND_SETTLE_MS); refresh()
    }

    fun openSetup(replacing: Boolean = false) = _state.update { it.copy(surface = Surface.SETUP, setup = SetupState(replacing = replacing)) }
    fun setupPage(p: SetupPage) = _state.update { it.copy(setup = it.setup.copy(page = p, error = null, status = null)) }
    fun leaveSetup() = _state.update { it.copy(surface = if (it.bootPackage?.exists == true) Surface.SETTINGS else Surface.HOME) }

    /** Pair: stop a running ship if this replaces it, ask the planet for a moon, then wait for it to come up and pair the mirror. */
    fun pair(hostUrl: String, accessCode: String) {
        val url = normalizeHostUrl(hostUrl) ?: run { setupError("Enter your planet's HTTPS hosting address."); return }
        if (accessCode.isBlank()) { setupError("Enter the access code from your planet."); return }
        viewModelScope.launch {
            setupBusy("reaching your planet")
            if (_state.value.runtime == "running") {
                setupBusy("stopping the current satellite")
                controller.stopRuntime()
                if (!awaitRuntime("stopped")) { setupError("The current satellite did not stop. Try again from the Server page."); return@launch }
            }
            setupBusy("waiting for your planet")
            val r = controller.pairWithPlanet(url, accessCode)
            if (!r.accepted) { setupError(pairingMessage(r)); return@launch }
            val bp = controller.bootPackage(r.bootPackage)
            _state.update { it.copy(setup = it.setup.copy(ship = bp?.ship, parent = bp?.parent)) }
            setupBusy("your satellite is starting")
            finishDelegation(bp?.parent)
            _state.update { it.copy(setup = it.setup.copy(busy = false, status = null, page = SetupPage.REVEAL)) }
            refresh()
        }
    }

    /** Import: a satellite you already hold the key for. */
    fun importMoon(shipIn: String, parentIn: String, key: String) {
        val ship = normalizeShip(shipIn); val parent = normalizeShip(parentIn)
        if (ship == null || parent == null) { setupError("Ship names look like ~sampel-palnet."); return }
        val k = key.trim()
        if (k.length !in 80..512 || !k.startsWith("0w") || !k.endsWith("3i5")) { setupError("That is not a satellite key. It starts with 0w and ends with 3i5."); return }
        viewModelScope.launch {
            setupBusy("checking the key")
            if (_state.value.runtime == "running") {
                setupBusy("stopping the current satellite")
                controller.stopRuntime()
                if (!awaitRuntime("stopped")) { setupError("The current satellite did not stop."); return@launch }
            }
            val r = controller.provisionMoon(ship, parent, k)
            if (!r.accepted) { setupError(r.message ?: r.code); return@launch }
            _state.update { it.copy(setup = it.setup.copy(ship = ship, parent = parent)) }
            setupBusy("your satellite is starting")
            finishDelegation(parent)
            _state.update { it.copy(setup = it.setup.copy(busy = false, status = null, page = SetupPage.REVEAL)) }
            refresh()
        }
    }

    /** After a fresh moon comes up: log in and tell its mirror which planet to act as. The mirror desk itself ships with the ROM's install step. */
    private suspend fun finishDelegation(parent: String?) {
        if (!awaitRuntime("running", SETUP_BOOT_WAIT_MS)) { setupBusy("still booting; it will finish on its own"); return }
        if (parent == null) return
        val code = controller.webLoginCode() ?: return
        if (!eyre.login(code)) return
        val ok = eyre.poke("nativeplanet-mobile", "json", org.json.JSONObject().put("pair", parent), _state.value.setup.ship ?: return)
        if (!ok) setupBusy("satellite up; the planet link will pair when its desk is installed")
    }

    private suspend fun awaitRuntime(state: String, timeoutMs: Long = RUNTIME_WAIT_MS): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            val rt = controller.runtime()
            if (rt?.state == state && (state != "running" || rt.connSock)) return true
            delay(RUNTIME_POLL_MS)
        }
        return false
    }

    private fun setupBusy(status: String) = _state.update { it.copy(setup = it.setup.copy(busy = true, status = status, error = null)) }
    private fun setupError(msg: String) = _state.update { it.copy(setup = it.setup.copy(busy = false, status = null, error = msg)) }

    private fun pairingMessage(r: Controller.Outcome): String = when (r.code) {
        "INVALID_HOST_URL" -> "Enter your planet's HTTPS hosting address."
        "MISSING_ACCESS_CODE" -> "Enter the access code from your planet."
        "PARENT_AUTH_FAILED" -> "Your planet did not accept that code. Check the address and the code."
        "PARENT_NETWORK_FAILED" -> "Could not reach your planet at that address."
        "PARENT_SERVICE_UNAVAILABLE" -> "Logged in, but Artemis is not installed on your planet yet."
        "PARENT_PROTOCOL_UNSUPPORTED" -> "Artemis is there but is not ready to mint a satellite yet."
        "PARENT_MOON_CREATE_FAILED" -> "Artemis did not accept the satellite request."
        "PARENT_MOON_CREATE_TIMEOUT" -> "Artemis did not answer in time. Try again."
        "RUNTIME_RUNNING" -> "Stop the current satellite first, from the Server page."
        "CONTROLLER_UNAVAILABLE" -> "The phone's controller is not answering."
        else -> r.message ?: "Pairing failed (${r.code})."
    }

    private fun normalizeHostUrl(raw: String): String? {
        var u = raw.trim().trimEnd('/')
        if (u.isEmpty()) return null
        if (!u.contains("://")) u = "https://$u"
        if (!u.startsWith("https://") || u.length > 512 || u.removePrefix("https://").isBlank()) return null
        return u
    }

    private fun normalizeShip(raw: String): String? {
        val s = raw.trim().removePrefix("~").lowercase()
        if (s.isEmpty() || s.length > 63 || !s.all { it.isLetterOrDigit() || it == '-' }) return null
        return "~$s"
    }

    fun markDone(e: Entry) = viewModelScope.launch { prefs.markDone(e.id) }
    fun mute(shipName: String, muted: Boolean) = viewModelScope.launch { prefs.setMuted(shipName, muted) }
    fun setReach(shipName: String, can: Boolean) = viewModelScope.launch { prefs.setReach(shipName, can) }
    fun setToolWords(words: List<String>) = viewModelScope.launch { prefs.setTools(words) }
}
