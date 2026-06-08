package io.nativeplanet.launcher.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class IdentityMode {
    NONE,
    COMET,
    PAIRED_MOON,
    IMPORTED
}

data class DemoIdentity(
    val shipName: String? = null,
    val parentName: String? = null,
    val mode: IdentityMode = IdentityMode.NONE
) {
    val isConfigured: Boolean get() = mode != IdentityMode.NONE && shipName != null

    val modeLabel: String get() = when (mode) {
        IdentityMode.NONE -> "none"
        IdentityMode.COMET -> "comet"
        IdentityMode.PAIRED_MOON -> "moon"
        IdentityMode.IMPORTED -> "imported"
    }
}

@Singleton
class DemoSessionState @Inject constructor() {

    private val _identity = MutableStateFlow(DemoIdentity())
    val identity: StateFlow<DemoIdentity> = _identity.asStateFlow()

    private val _backendMode = MutableStateFlow("demo")
    val backendMode: StateFlow<String> = _backendMode.asStateFlow()

    fun setIdentity(shipName: String, parentName: String?, mode: IdentityMode) {
        _identity.update {
            DemoIdentity(shipName = shipName, parentName = parentName, mode = mode)
        }
    }

    fun setComet(shipName: String) {
        setIdentity(shipName, null, IdentityMode.COMET)
    }

    fun setPairedMoon(shipName: String, parentName: String) {
        setIdentity(shipName, parentName, IdentityMode.PAIRED_MOON)
    }

    fun setImported(shipName: String, parentName: String?) {
        setIdentity(shipName, parentName, IdentityMode.IMPORTED)
    }

    fun setBackendMode(mode: String) {
        _backendMode.update { mode }
    }

    fun reset() {
        _identity.update { DemoIdentity() }
        _backendMode.update { "demo" }
    }
}
