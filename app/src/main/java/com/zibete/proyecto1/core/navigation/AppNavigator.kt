package com.zibete.proyecto1.core.navigation

import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.domain.session.SessionConflictNavigator
import com.zibete.proyecto1.ui.components.ZibeSnackType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class AppNavigator @Inject constructor() : SessionConflictNavigator {
    private val _events = MutableSharedFlow<NavAppEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    fun finishFlowNavigateToSplash(
        uiText: UiText? = null,
        snackType: ZibeSnackType? = null,
        deleteAccount: Boolean = false,
        sessionConflict: Boolean = false
    ) = _events.tryEmit(
        NavAppEvent.FinishFlowNavigateToSplash(
            snackMessage = uiText,
            snackType = snackType,
            deleteAccount = deleteAccount,
            sessionConflict = sessionConflict
        )
    )

    override fun onSessionConflict() {
        finishFlowNavigateToSplash(sessionConflict = true)
    }
}
