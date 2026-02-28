package com.zibete.proyecto1.model

sealed class UserStatus {
    object Online : UserStatus()
    data class TypingOrRecording(val text: String) : UserStatus()
    data class LastSeen(val text: String) : UserStatus()
    object Offline : UserStatus()
}