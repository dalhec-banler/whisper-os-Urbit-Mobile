package io.nativeplanet.launcher.domain.model

sealed class ControlResult {
    data class Success(
        val shipName: String? = null,
        val parentName: String? = null
    ) : ControlResult()
    data class AlreadyInState(val state: RuntimeState) : ControlResult()
    data class Failed(val code: String, val message: String) : ControlResult()
}
