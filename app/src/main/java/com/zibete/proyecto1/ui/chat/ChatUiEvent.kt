//package com.zibete.proyecto1.ui.chat
//
//import android.content.Intent
//
//sealed class ChatUiEvent {
//    data class ShowSnackbar(val message: String) : ChatUiEvent()
//    data class ShowToast(val message: String) : ChatUiEvent()
//    data class ShowGenericDialog(val title: String, val message: String, val positiveAction: (() -> Unit)?) : ChatUiEvent()
//    data class ExecuteIntent(val intent: Intent) : ChatUiEvent()
//    data class NotifyUiUpdate(val type: String) : ChatUiEvent()
//    data class AudioUploadSuccess(val duration: String) : ChatUiEvent()
//}