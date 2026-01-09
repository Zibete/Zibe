package com.zibete.proyecto1.ui.chat.session

import android.content.Context
import android.view.View
import com.zibete.proyecto1.ui.components.ZibeSnackType
import com.zibete.proyecto1.core.utils.UserMessageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object ChatSessionUiHandler {

    fun handle(
        context: Context,
        root: View,
        event: ChatSessionUiEvent,
        scope: CoroutineScope

    ) {
        when (event) {

            is ChatSessionUiEvent.ConfirmBlock -> {
                UserMessageUtils.confirm(
                    context = context,
                    title = "Bloquear usuario",
                    message = "¿Bloquear a ${event.name}? No podrán enviarse mensajes.",
                    onConfirm = {
                        scope.launch { event.onConfirm() }
                    }
                )
            }

            is ChatSessionUiEvent.ShowBlockSuccess -> {
                UserMessageUtils.showSnack(
                    root = root,
                    message = "Se ha bloqueado a ${event.name}",
                    type = ZibeSnackType.SUCCESS
                )
            }

            is ChatSessionUiEvent.ConfirmHideChat -> {
                UserMessageUtils.confirm(
                    context = context,
                    title = "Ocultar chat",
                    message = "¿Ocultar chat con ${event.name}?",
                    onConfirm = {
                        scope.launch { event.onConfirm() }
                    }
                )
            }

            is ChatSessionUiEvent.ShowChatHiddenSuccess -> {
                UserMessageUtils.showSnack(
                    root = root,
                    message = "Se ha ocultado el chat",
                    type = ZibeSnackType.SUCCESS
                )
            }

            is ChatSessionUiEvent.ConfirmUnblock -> {
                UserMessageUtils.confirm(
                    context = context,
                    title = "Desbloquear usuario",
                    message = "¿Desbloquear a ${event.name}? Podrá volver a enviarte mensajes.",
                    onConfirm = {
                        scope.launch { event.onConfirm() }
                    }
                )
            }

            is ChatSessionUiEvent.ShowUnblockSuccess -> {
                UserMessageUtils.showSnack(
                    root = root,
                    message = "Se ha desbloqueado a ${event.name}",
                    type = ZibeSnackType.SUCCESS
                )
            }

            is ChatSessionUiEvent.ConfirmDeleteChat -> {
                var deleteMessages = false

                val choices = arrayOf(
                    "Ocultar chat",
                    if (event.countMessages == 1)
                        "Eliminar 1 mensaje"
                    else
                        "Eliminar ${event.countMessages} mensajes"
                )

                UserMessageUtils.confirm(
                    context = context,
                    title = "Eliminar chat",
                    message = "¿Eliminar el chat con ${event.name}? Se eliminarán los mensajes en este dispositivo.",
                    choices = choices,
                    selectedIndex = 0,
                    onChoiceSelected = { index -> deleteMessages = (index == 1) },
                    onConfirm = {
                        scope.launch { event.onConfirm(deleteMessages) }
                    }
                )
            }

            is ChatSessionUiEvent.ShowDeleteMessagesSuccess -> {

                UserMessageUtils.showSnack(
                    root = root,
                    message =   if (event.count > 1) {
                        "${event.count} mensajes eliminados"
                    } else {
                        "Mensaje eliminado"
                    },
                    type = ZibeSnackType.INFO
                )
            }

            is ChatSessionUiEvent.ShowToggleNotificationSuccess -> {
                UserMessageUtils.showSnack(
                    root = root,
                    message =   if (event.enabled) "Notificaciones de ${event.name} activadas"
                    else "Notificaciones de ${event.name} desactivadas",
                    type = ZibeSnackType.INFO
                )
            }

            is ChatSessionUiEvent.OtherUserNoLongerAvailable -> {
                UserMessageUtils.alert(
                    context = context,
                    message = "Lo sentimos, ${event.userName} ya no está disponible",
                    onConfirm = { scope.launch { event.onConfirm() }
                    }
                )
            }

            is ChatSessionUiEvent.ShowBlockedByOther -> {
                UserMessageUtils.alert(
                    context = context,
                    message = "Lo sentimos, ${event.userName} ha bloqueado tus mensajes"
                )
            }

            is ChatSessionUiEvent.ShowErrorDialog -> {
                UserMessageUtils.alert(
                    context = context,
                    title = "ShowErrorDialog",
                    message = event.uiText.asString(context)
                )
            }

            else -> {}
        }
    }
}