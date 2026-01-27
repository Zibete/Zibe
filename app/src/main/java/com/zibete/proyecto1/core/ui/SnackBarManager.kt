package com.zibete.proyecto1.core.ui

import com.zibete.proyecto1.ui.components.ZibeSnackType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ZibeSnackEvent(
    val uiText: UiText,
    val type: ZibeSnackType
)

@Singleton
class SnackBarManager @Inject constructor() {

    private val _events = Channel<ZibeSnackEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun show(
        uiText: UiText,
        type: ZibeSnackType
    ) {
        _events.trySend(
            ZibeSnackEvent(
                uiText = uiText,
                type = type
            )
        )
    }
}
