package com.zibete.proyecto1.ui.chat.session

import android.content.Context
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.UserMessageUtils
import com.zibete.proyecto1.ui.components.ZibeSnackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object ChatSessionUiHandler {

    fun handle(
        context: Context,
        event: ChatSessionUiEvent,
        scope: CoroutineScope,
        snackBarManager: SnackBarManager
    ) {
        when (event) {
            is ChatSessionUiEvent.ShowToggleBlockSuccess -> {
                snackBarManager.show(
                    uiText = UiText.StringRes(
                        if (event.isBlocked) R.string.chat_block_success
                        else R.string.chat_unblock_success,
                        listOf(event.name)
                    ),
                    type = ZibeSnackType.INFO
                )
            }

            is ChatSessionUiEvent.ConfirmToggleBlockAction -> {
                UserMessageUtils.confirm(
                    context = context,
                    title = if (event.isBlockedByMe)
                        context.getString(R.string.chat_confirm_unblock_title)
                    else context.getString(R.string.chat_confirm_block_title),
                    message = if (event.isBlockedByMe)
                        context.getString(R.string.chat_confirm_unblock_message, event.name)
                    else context.getString(R.string.chat_confirm_block_message, event.name),
                    onConfirm = { scope.launch { event.onConfirm() } }
                )
            }

            is ChatSessionUiEvent.ConfirmHideChat -> {
                UserMessageUtils.confirm(
                    context = context,
                    title = context.getString(R.string.chat_confirm_hide_title),
                    message = context.getString(R.string.chat_confirm_hide_message, event.name),
                    onConfirm = { scope.launch { event.onConfirm() } }
                )
            }

            is ChatSessionUiEvent.ShowChatHiddenSuccess -> {
                snackBarManager.show(
                    uiText = UiText.StringRes(R.string.chat_hidden_success),
                    type = ZibeSnackType.SUCCESS
                )
            }

            is ChatSessionUiEvent.ConfirmDeleteChat -> {
                var deleteMessages = false

                val choices = arrayOf(
                    context.getString(R.string.chat_choice_hide_chat),
                    context.resources.getQuantityString(
                        R.plurals.chat_choice_delete_messages,
                        event.countMessages,
                        event.countMessages
                    )
                )

                UserMessageUtils.confirm(
                    context = context,
                    title = context.getString(R.string.chat_confirm_delete_title),
                    message = context.getString(R.string.chat_confirm_delete_message, event.name),
                    choices = choices,
                    selectedIndex = 0,
                    onChoiceSelected = { index -> deleteMessages = (index == 1) },
                    onConfirm = { scope.launch { event.onConfirm(deleteMessages) } }
                )
            }

            is ChatSessionUiEvent.ShowDeleteMessagesSuccess -> {
                val message = UiText.Dynamic(
                    context.resources.getQuantityString(
                        R.plurals.chat_delete_messages_success,
                        event.count,
                        event.count
                    )
                )
                snackBarManager.show(
                    uiText = message,
                    type = ZibeSnackType.INFO
                )
            }

            is ChatSessionUiEvent.ShowToggleNotificationSuccess -> {
                snackBarManager.show(
                    uiText = UiText.StringRes(
                        if (event.isNotificationsSilenced) R.string.chat_notifications_enabled
                        else R.string.chat_notifications_disabled,
                        listOf(event.name)
                    ),
                    type = ZibeSnackType.INFO
                )
            }

            is ChatSessionUiEvent.ShowToggleFavoriteSuccess -> {
                snackBarManager.show(
                    uiText = UiText.StringRes(
                        if (event.newFavoriteState) R.string.favorite_added
                        else R.string.favorite_removed, listOf(event.name)
                    ),
                    type = ZibeSnackType.INFO
                )
            }

            is ChatSessionUiEvent.OtherUserNoLongerAvailable -> {
                UserMessageUtils.dialog(
                    context = context,
                    message = context.getString(
                        R.string.chat_other_user_no_longer_available,
                        event.userName
                    ),
                    onConfirm = { scope.launch { event.onConfirm() } }
                )
            }

            is ChatSessionUiEvent.ShowBlockedByOther -> {
                UserMessageUtils.dialog(
                    context = context,
                    message = context.getString(R.string.chat_blocked_by_other_user, event.userName)
                )
            }

            is ChatSessionUiEvent.ShowErrorDialog -> {
                UserMessageUtils.dialog(
                    context = context,
                    title = context.getString(R.string.dialog_error_title),
                    message = event.uiText.asString(context)
                )
            }

            else -> {}
        }
    }
}
