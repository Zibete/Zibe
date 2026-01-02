package com.zibete.proyecto1.domain.profile

import android.net.Uri
import com.zibete.proyecto1.core.ZibeResult
import com.zibete.proyecto1.core.constants.Constants.AccountsKeys
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.UserSessionActions
import javax.inject.Inject

// Definimos el contrato
interface UpdateProfileUseCase {
    suspend fun execute(
        newName: String,
        newBirthDate: String,
        newDescription: String,
        age: Int,
        originalPhotoUrl: String?,
        photoPreviewUri: Uri?,
        shouldDeletePhoto: Boolean
    ): ZibeResult<Unit>
}

class DefaultUpdateProfileUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val userRepositoryProvider: UserRepositoryProvider,
    private val userSessionActions: UserSessionActions
) : UpdateProfileUseCase {

    override suspend fun execute(
        newName: String,
        newBirthDate: String,
        newDescription: String,
        age: Int,
        originalPhotoUrl: String?,
        photoPreviewUri: Uri?,
        shouldDeletePhoto: Boolean
    ): ZibeResult<Unit> {
        return try {
            var finalPhotoUrl: String? = originalPhotoUrl

            // 1) Manejo de la foto en Firebase Storage
            when {
                shouldDeletePhoto -> {
                    userRepositoryActions.deleteProfilePhoto()
                    finalPhotoUrl = DEFAULT_PROFILE_PHOTO_URL
                }
                photoPreviewUri != null -> {
                    // Aquí photoPreviewUri es el path local de la imagen elegida
                    userRepositoryActions.putProfilePhotoInStorage(photoPreviewUri)
                    finalPhotoUrl = userRepositoryProvider.getProfilePhotoUrl()
                }
            }

            // 2) Preparar la actualización para Realtime Database
            val updates = mutableMapOf<String, Any?>(
                AccountsKeys.NAME to newName,
                AccountsKeys.BIRTHDATE to newBirthDate,
                AccountsKeys.AGE to age,
                AccountsKeys.DESCRIPTION to newDescription
            )

            // Solo agregamos la URL si cambió
            if (finalPhotoUrl != null && finalPhotoUrl != originalPhotoUrl) {
                updates[AccountsKeys.PHOTO_URL] = finalPhotoUrl
            }

            // 3) Ejecutar actualización en la base de datos
            userRepositoryActions.updateUserFields(updates)

            // 4) Actualizar el perfil en Firebase Auth (Nombre y Foto)
            userSessionActions.updateAuthProfile(newName, finalPhotoUrl)

        } catch (e: Exception) {
            ZibeResult.Failure(e)
        }
    }
}