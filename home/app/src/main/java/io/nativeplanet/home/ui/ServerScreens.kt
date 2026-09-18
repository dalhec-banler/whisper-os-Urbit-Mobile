package io.nativeplanet.home.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * The server page and the setup flow: what the old Planet Link launcher did with
 * a runtime console, network and diagnostics panels, identity settings, and a
 * four-step onboarding, rebuilt in Whisper Home's idiom. Facts as lines,
 * actions as words, one page per question.
 */

// ---------- SERVER ----------

@Composable
internal fun ServerPage(vm: HomeViewModel, s: UiState) {
    BackHandler { vm.settings() }
    val bp = s.bootPackage
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = PAGE_PAD)) {
        Spacer(Modifier.height(22.dp))
        Text("Server", color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 14.dp).clickable { vm.settings() })
        Rule()
        LazyColumn {
            if (!s.controllerAvailable) {
                item { Section("CONTROLLER") }
                item { Line("The phone's controller is not answering. Nothing below is current.") }
            }
            item { Section("SATELLITE") }
            item { Fact("state", s.runtime + if (s.connected) " · connected" else "") }
            item { Fact("ship", s.self ?: "none") }
            if (bp?.parent != null) item { Fact("planet", bp.parent + if (s.delegated) " · acting as it" else " · not linked yet") }
            item { Fact("runtime", s.runtimeVersion ?: "—") }
            item { Fact("conn.sock", if (s.connSock) "answering" else "not answering") }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    when (s.runtime) {
                        "running", "starting" -> Word("STOP", enabled = s.runtime == "running") { vm.stopRuntime() }
                        else -> Word("START", enabled = bp?.valid == true) { vm.startRuntime() }
                    }
                    Word("REPLACE") { vm.openSetup(replacing = true) }
                }
            }
            item { Mono("STOP asks the ship to exit cleanly. It can take a while; the state line says when it has.", size = 9, tracking = 0.06, modifier = Modifier.padding(bottom = 6.dp)) }

            item { Section("IDENTITY") }
            if (bp == null || !bp.exists) {
                item { Line("No satellite on this phone.") }
                item { Word("SET UP") { vm.openSetup() } }
            } else {
                item { Fact("boot mode", bp.bootMode ?: "—") }
                item { Fact("package", if (bp.valid) "valid" else "invalid") }
                item { Fact("pier", (bp.pierPath ?: "—") + if (bp.pierExists) "" else " · not created yet") }
                item { Fact("pill", if (bp.pillExists) "present" else "missing") }
                item { Fact("key", if (bp.keyFileExists) "on this phone" else "missing") }
                bp.validationErrors.forEach { e -> item { Fact("problem", e) } }
                item { Mono("Key material never leaves the phone and is never shown here.", size = 9, tracking = 0.06, modifier = Modifier.padding(vertical = 6.dp)) }
            }

            item { Section("NETWORK") }
            val net = s.network
            if (net == null) item { Line("—") } else {
                item { Fact("type", net.type.lowercase() + (net.iface?.let { " · $it" } ?: "")) }
                item { Fact("validated", if (net.validated) "yes" else "no") }
                item { Fact("dns", if (net.dns.isEmpty()) "none" else net.dns.joinToString(" ")) }
                item { Fact("resolver", if (net.resolverAvailable) "written" else "missing") }
            }

            item { Section("DIAGNOSTICS") }
            val diag = s.diagnostics
            if (diag == null) item { Line("—") } else {
                if (diag.recentErrors.isEmpty()) item { Line("No recent errors.") }
                diag.recentErrors.take(5).forEach { e -> item { Line(e) } }
                item { Mono("CONTROLLER LOG", size = 9, tracking = 0.14, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                diag.controllerLogs.takeLast(12).forEach { l -> item { Mono(l, size = 9, tracking = 0.02, color = W.Paper60) } }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// ---------- SETUP ----------

@Composable
internal fun SetupFlow(vm: HomeViewModel, s: UiState) {
    val st = s.setup
    BackHandler {
        when (st.page) {
            SetupPage.WELCOME -> if (s.bootPackage?.exists == true) vm.leaveSetup()
            SetupPage.REVEAL -> vm.leaveSetup()
            else -> if (!st.busy) vm.setupPage(SetupPage.WELCOME)
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = PAGE_PAD)) {
        Spacer(Modifier.height(48.dp))
        when (st.page) {
            SetupPage.WELCOME -> Welcome(vm, s)
            SetupPage.PAIR -> Pair(vm, st)
            SetupPage.IMPORT -> Import(vm, st)
            SetupPage.REVEAL -> Reveal(vm, st)
        }
    }
}

@Composable
private fun Welcome(vm: HomeViewModel, s: UiState) {
    val replacing = s.setup.replacing && s.bootPackage?.exists == true
    Mono(if (replacing) "REPLACE THE SATELLITE" else "WHISPER OS", size = 10, tracking = 0.14)
    Spacer(Modifier.height(18.dp))
    Text(if (replacing) "A different planet." else "This phone needs a planet.", color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 34.sp, lineHeight = 38.sp)
    Spacer(Modifier.height(14.dp))
    Line(
        if (replacing) "The satellite on this phone (${s.self ?: "unknown"}) stops and stays on disk. A new one takes its place."
        else "Your planet makes a satellite for this phone. The satellite lives here; the planet stays the root, and the phone speaks as the planet."
    )
    Spacer(Modifier.height(36.dp))
    BigWord("PAIR WITH YOUR PLANET") { vm.setupPage(SetupPage.PAIR) }
    Spacer(Modifier.height(18.dp))
    Word("IMPORT A SATELLITE") { vm.setupPage(SetupPage.IMPORT) }
    if (replacing) { Spacer(Modifier.height(18.dp)); Word("KEEP WHAT I HAVE") { vm.leaveSetup() } }
}

@Composable
private fun Pair(vm: HomeViewModel, st: SetupState) {
    var url by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    Mono("PAIR", size = 10, tracking = 0.14)
    Spacer(Modifier.height(18.dp))
    Text("Open your planet.", color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 34.sp)
    Spacer(Modifier.height(14.dp))
    Line("Its hosting address, and the code from +code in the dojo. The code is used once and not kept.")
    Spacer(Modifier.height(28.dp))
    Field("PLANET ADDRESS", url, { url = it }, hint = "your-planet.startram.io", enabled = !st.busy, keyboard = KeyboardType.Uri)
    Spacer(Modifier.height(18.dp))
    Field("ACCESS CODE", code, { code = it }, hint = "word-word-word-word", enabled = !st.busy, secret = true, ime = ImeAction.Go) {
        vm.pair(url, code); code = ""
    }
    Spacer(Modifier.height(28.dp))
    Status(st)
    if (!st.busy) BigWord("PAIR THIS PHONE") { vm.pair(url, code); code = "" }
}

@Composable
private fun Import(vm: HomeViewModel, st: SetupState) {
    var ship by remember { mutableStateOf("") }
    var parent by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    Mono("IMPORT", size = 10, tracking = 0.14)
    Spacer(Modifier.height(18.dp))
    Text("A satellite you already have.", color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 34.sp, lineHeight = 38.sp)
    Spacer(Modifier.height(14.dp))
    Line("Its name, your planet's name, and its key. The key stays on this phone.")
    Spacer(Modifier.height(28.dp))
    Field("SATELLITE", ship, { ship = it }, hint = "~sampel-palnet-sampel-palnet", enabled = !st.busy)
    Spacer(Modifier.height(18.dp))
    Field("YOUR PLANET", parent, { parent = it }, hint = "~sampel-palnet", enabled = !st.busy)
    Spacer(Modifier.height(18.dp))
    Field("KEY", key, { key = it }, hint = "0w…3i5", enabled = !st.busy, secret = true, ime = ImeAction.Go) { vm.importMoon(ship, parent, key); key = "" }
    Spacer(Modifier.height(28.dp))
    Status(st)
    if (!st.busy) BigWord("IMPORT") { vm.importMoon(ship, parent, key); key = "" }
}

@Composable
private fun Reveal(vm: HomeViewModel, st: SetupState) {
    Mono("PAIRED", size = 10, tracking = 0.14)
    Spacer(Modifier.height(18.dp))
    Text(st.ship ?: "your satellite", color = W.Paper, fontFamily = W.Serif, fontWeight = FontWeight.Light, fontSize = 30.sp, lineHeight = 36.sp)
    Spacer(Modifier.height(8.dp))
    Mono("UNDER ${st.parent?.uppercase() ?: "YOUR PLANET"}", size = 11, tracking = 0.14)
    Spacer(Modifier.height(22.dp))
    Line("This satellite signs from the phone and speaks as your planet. Its key is on this phone and nowhere else; there is no backup of it yet, so keep the planet's own key safe.")
    st.status?.let { Spacer(Modifier.height(14.dp)); Mono(it.uppercase(), size = 9, tracking = 0.12) }
    Spacer(Modifier.height(36.dp))
    BigWord("GO HOME") { vm.leaveSetup(); vm.home() }
}

// ---------- pieces ----------

@Composable private fun Section(t: String) = Mono(t, size = 10, tracking = 0.14, modifier = Modifier.padding(top = 22.dp, bottom = 6.dp))

@Composable private fun Line(t: String) =
    Text(t, color = W.Paper90, fontFamily = W.Serif, fontSize = 16.sp, lineHeight = 22.sp, modifier = Modifier.padding(vertical = 6.dp))

@Composable private fun Fact(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Mono(k.uppercase(), size = 10, modifier = Modifier.padding(end = 16.dp))
        Text(v, color = W.Paper90, fontFamily = W.Serif, fontSize = 15.sp, modifier = Modifier.weight(1f, fill = false))
    }
    Hairline()
}

@Composable private fun Word(t: String, enabled: Boolean = true, onTap: () -> Unit) =
    Mono(t, color = if (enabled) W.Paper else W.Paper40, size = 12, tracking = 0.14, modifier = Modifier.clickable(enabled = enabled) { onTap() }.padding(vertical = 8.dp))

@Composable private fun BigWord(t: String, onTap: () -> Unit) =
    Text(t, color = W.Ink, fontFamily = W.Mono, fontSize = 12.sp, letterSpacing = 0.14.em,
        modifier = Modifier.fillMaxWidth().background(W.Paper).clickable { onTap() }.padding(vertical = 14.dp, horizontal = 16.dp))

@Composable private fun Status(st: SetupState) {
    st.status?.let { Mono("$it…".uppercase(), size = 10, tracking = 0.12, modifier = Modifier.padding(bottom = 16.dp)) }
    st.error?.let { Text(it, color = W.Paper, fontFamily = W.Serif, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(bottom = 16.dp)) }
}

@Composable private fun Field(
    label: String, value: String, onChange: (String) -> Unit, hint: String, enabled: Boolean,
    secret: Boolean = false, keyboard: KeyboardType = KeyboardType.Text, ime: ImeAction = ImeAction.Next, onGo: (() -> Unit)? = null,
) {
    Mono(label, size = 10, tracking = 0.14, modifier = Modifier.padding(bottom = 6.dp))
    BasicTextField(
        value = value, onValueChange = onChange, enabled = enabled, singleLine = !secret || label != "KEY",
        textStyle = TextStyle(color = W.Paper, fontFamily = if (secret) W.Mono else W.Serif, fontSize = 18.sp),
        cursorBrush = SolidColor(W.Paper),
        visualTransformation = if (secret && label != "KEY") PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard, imeAction = ime, autoCorrect = false),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onGo = { onGo?.invoke() }),
        decorationBox = { inner ->
            Column {
                if (value.isEmpty()) Text(hint, color = W.Paper40, fontFamily = if (secret) W.Mono else W.Serif, fontSize = 18.sp)
                inner()
                Spacer(Modifier.height(6.dp)); Hairline()
            }
        },
    )
}
