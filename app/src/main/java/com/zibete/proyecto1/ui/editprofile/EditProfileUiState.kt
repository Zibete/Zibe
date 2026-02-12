package com.zibete.proyecto1.ui.editprofile

import android.net.Uri
import com.zibete.proyecto1.core.ui.UiText

enum class PendingNav { BACK, SETTINGS }

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isValidating: Boolean = false,

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

    val hasPendingChanges: Boolean = false,

    val birthDateError: UiText? = null,
    val nameError: UiText? = null,
    val showDiscardDialog: Boolean = false,
    val showWelcomeSheet: Boolean = false,
    val showSkipButton: Boolean = false,
    val isFirstLoginDone: Boolean = true,
    val pendingNav: PendingNav? = null,
)
