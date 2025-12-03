package com.zibete.proyecto1.ui.profile

data class UserProfile(
    val id: String,
    val name: String,
    val description: String?,
    val birthDay: String?,
    val photoUrl: String?,
    val lat: Double,
    val long: Double
)
