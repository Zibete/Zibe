package com.zibete.proyecto1.core.ui

import com.zibete.proyecto1.ui.components.ZibeSnackType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ZibeSnackEvent(
    val uiText: UiText,
    val type: ZibeSnackType
)

@Singleton
class SnackBarManager @Inject constructor() {

    private val _events = MutableSharedFlow<ZibeSnackEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun show(uiText: UiText, type: ZibeSnackType) {
        _events.tryEmit(ZibeSnackEvent(uiText = uiText, type = type))
    }
}
