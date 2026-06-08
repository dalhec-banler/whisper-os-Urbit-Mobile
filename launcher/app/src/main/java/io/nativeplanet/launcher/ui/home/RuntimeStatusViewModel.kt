package io.nativeplanet.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.nativeplanet.launcher.domain.DemoIdentity
import io.nativeplanet.launcher.domain.DemoSessionState
import io.nativeplanet.launcher.domain.IdentityMode
import io.nativeplanet.launcher.domain.NativePlanetClient
import io.nativeplanet.launcher.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RuntimeStatusUiState(
    val runtimeStatus: RuntimeStatus = RuntimeStatus.UNINITIALIZED,
    val networkStatus: NetworkStatus = NetworkStatus.DISCONNECTED,
    val bootPackageStatus: BootPackageStatus = BootPackageStatus.NONE,
    val diagnostics: DiagnosticsSummary = DiagnosticsSummary.EMPTY,
    val isLoading: Boolean = false,
    val actionResult: String? = null,
    val controllerAvailable: Boolean = false,
    val usingDemoData: Boolean = true,
    val demoIdentity: DemoIdentity = DemoIdentity(),
    val backendMode: String = "demo"
)

@HiltViewModel
class RuntimeStatusViewModel @Inject constructor(
    private val client: NativePlanetClient,
    private val demoSession: DemoSessionState
) : ViewModel() {

    private val _uiState = MutableStateFlow(RuntimeStatusUiState())
    val uiState: StateFlow<RuntimeStatusUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            demoSession.identity.collect { identity ->
                _uiState.update { it.copy(demoIdentity = identity) }
            }
        }

        viewModelScope.launch {
            demoSession.backendMode.collect { mode ->
                _uiState.update { it.copy(backendMode = mode) }
            }
        }
        viewModelScope.launch {
            client.observeRuntimeStatus().collect { status ->
                val fromController = status.state != RuntimeState.UNINITIALIZED || status.shipName != null
                _uiState.update {
                    it.copy(
                        runtimeStatus = status,
                        controllerAvailable = fromController || it.controllerAvailable,
                        usingDemoData = !fromController && !it.controllerAvailable
                    )
                }
            }
        }

        viewModelScope.launch {
            client.observeNetworkStatus().collect { status ->
                val fromController = status.resolverContents != null
                _uiState.update {
                    it.copy(
                        networkStatus = status,
                        controllerAvailable = fromController || it.controllerAvailable,
                        usingDemoData = !fromController && !it.controllerAvailable
                    )
                }
            }
        }

        viewModelScope.launch {
            client.observeBootPackageStatus().collect { status ->
                _uiState.update { it.copy(bootPackageStatus = status) }
            }
        }

        viewModelScope.launch {
            client.observeDiagnostics().collect { diagnostics ->
                _uiState.update { it.copy(diagnostics = diagnostics) }
            }
        }
    }

    fun startRuntime() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = client.startRuntime()
            val message = when (result) {
                is ControlResult.Success -> "Runtime started"
                is ControlResult.AlreadyInState -> "Already ${result.state}"
                is ControlResult.Failed -> "Failed: ${result.message}"
            }
            _uiState.update { it.copy(isLoading = false, actionResult = message) }
        }
    }

    fun stopRuntime() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = client.stopRuntime()
            val message = when (result) {
                is ControlResult.Success -> "Runtime stopped"
                is ControlResult.AlreadyInState -> "Already ${result.state}"
                is ControlResult.Failed -> "Failed: ${result.message}"
            }
            _uiState.update { it.copy(isLoading = false, actionResult = message) }
        }
    }

    fun restartRuntime() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = client.restartRuntime()
            val message = when (result) {
                is ControlResult.Success -> "Runtime restarted"
                is ControlResult.AlreadyInState -> "Already ${result.state}"
                is ControlResult.Failed -> "Failed: ${result.message}"
            }
            _uiState.update { it.copy(isLoading = false, actionResult = message) }
        }
    }

    fun clearActionResult() {
        _uiState.update { it.copy(actionResult = null) }
    }

    fun resetDemo() {
        demoSession.reset()
    }
}
