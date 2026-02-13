package com.zibete.proyecto1.ui.users

data class UsersRowUiModel(
    val id: String,
    val name: String,
    val age: Int,
    val isOnline: Boolean,
    val distanceMeters: Double,
    val photoUrl: String,
    val description: String
)
