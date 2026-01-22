package com.zibete.proyecto1.core.navigation

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class AppNavigator @Inject constructor() {
    private val _events = MutableSharedFlow<NavAppEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    fun finishFlowNavigateToSplash() = _events.tryEmit(NavAppEvent.FinishFlowNavigateToSplash)
}
