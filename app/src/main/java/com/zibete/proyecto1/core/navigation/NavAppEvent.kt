package com.zibete.proyecto1.core.navigation

sealed interface NavAppEvent {
    data object FinishFlowNavigateToSplash : NavAppEvent
}
