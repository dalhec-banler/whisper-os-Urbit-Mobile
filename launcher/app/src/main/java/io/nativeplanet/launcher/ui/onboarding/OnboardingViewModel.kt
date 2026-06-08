package io.nativeplanet.launcher.ui.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.nativeplanet.launcher.domain.DemoSessionState
import io.nativeplanet.launcher.domain.IdentityMode
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val demoSession: DemoSessionState
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
}
