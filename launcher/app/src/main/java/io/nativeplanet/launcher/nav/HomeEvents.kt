package io.nativeplanet.launcher.nav

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object HomeEvents {
    private val mutableRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests = mutableRequests.asSharedFlow()

    fun requestHome() {
        mutableRequests.tryEmit(Unit)
    }
}
