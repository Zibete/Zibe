package com.zibete.proyecto1.ui.base

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.utils.UserMessageUtils
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
                    duration = Snackbar.LENGTH_INDEFINITE,
                    iconRes = R.drawable.ic_info_24
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

                is ChatSessionUiEvent.ShowChatHidden -> {
                UserMessageUtils.showSnack(
                    root = root,
                    message = "Se ha ocultado el chat",
                    duration = Snackbar.LENGTH_INDEFINITE,
                    iconRes = R.drawable.ic_info_24
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
                    duration = Snackbar.LENGTH_INDEFINITE,
                    iconRes = R.drawable.ic_info_24
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

                is ChatSessionUiEvent.ShowDeleteChatSuccess -> {
                UserMessageUtils.showSnack(
                    root = root,
                    message = "Se ha eliminado el chat con ${event.name}",
                    duration = Snackbar.LENGTH_INDEFINITE,
                    iconRes = R.drawable.ic_info_24
                )
            }



            else -> {}

        }
    }
}
