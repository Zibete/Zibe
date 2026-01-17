package com.zibete.proyecto1.ui.editprofile

import android.net.Uri

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,

    val name: String = "",
    val description: String = "",
    val birthDate: String = "",
    val age: Int? = null,

    val originalName: String = "",
    val originalDescription: String = "",
    val originalBirthDate: String = "",
    val originalPhotoUrl: String? = null,

    val photoUrl: String? = null,      // foto actual (remota)
    val photoPreviewUri: Uri? = null,  // foto seleccionada local (sin subir todavía)
    val deletePhoto: Boolean = false,  // marcar para volver a default

    val saveEnabled: Boolean = false,
    val hasBirthDate: Boolean = false
)
