package io.nativeplanet.launcher.domain.model

sealed class ControlResult {
    object Success : ControlResult()
    data class AlreadyInState(val state: RuntimeState) : ControlResult()
    data class Failed(val code: String, val message: String) : ControlResult()
}
