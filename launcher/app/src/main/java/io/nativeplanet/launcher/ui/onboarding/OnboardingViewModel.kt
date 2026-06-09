package io.nativeplanet.launcher.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.nativeplanet.launcher.domain.DemoSessionState
import io.nativeplanet.launcher.domain.IdentityMode
import io.nativeplanet.launcher.domain.NativePlanetClient
import io.nativeplanet.launcher.domain.model.ControlResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val demoSession: DemoSessionState,
    private val client: NativePlanetClient
) : ViewModel() {

    fun completeComet(shipName: String) {
        demoSession.setComet(shipName)
    }

    fun completePairing(shipName: String, parentName: String) {
        demoSession.setPairedMoon(shipName, parentName)
    }

    fun completeImport(shipName: String, parentName: String?) {
        demoSession.setImported(shipName, parentName)
    }

    suspend fun provisionMoon(shipName: String, parentName: String, keyMaterial: String): ControlResult {
        return withContext(Dispatchers.IO) {
            client.provisionMoon(shipName, parentName, keyMaterial)
        }
    }
}
