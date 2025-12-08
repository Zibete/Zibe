package com.zibete.proyecto1.ui.chat

data class ChatState(
    val showPhotoPicker: Boolean = false,
    val photoReady: Boolean = false,
    val textReady: Boolean = false,
    val pendingFileUrl: String? = null

)
