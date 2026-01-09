package com.zibete.proyecto1.domain.profile

import android.net.Uri
import com.zibete.proyecto1.core.constants.Constants.AccountsKeys
import com.zibete.proyecto1.core.constants.Constants.DEFAULT_PROFILE_PHOTO_URL
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.UserRepositoryActions
import com.zibete.proyecto1.data.UserRepositoryProvider
import com.zibete.proyecto1.data.auth.AuthSessionActions
import javax.inject.Inject

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
    private val authSessionActions: AuthSessionActions
) : UpdateProfileUseCase {

    override suspend fun execute(
        newName: String,
        newBirthDate: String,
        newDescription: String,
        age: Int,
        originalPhotoUrl: String?,
        photoPreviewUri: Uri?,
        shouldDeletePhoto: Boolean
    ): ZibeResult<Unit> =
        zibeCatching {
            var finalPhotoUrl: String? = originalPhotoUrl

            when {
                shouldDeletePhoto -> {
                    userRepositoryActions.deleteProfilePhoto()
                    finalPhotoUrl = DEFAULT_PROFILE_PHOTO_URL
                }
                photoPreviewUri != null -> {
                    userRepositoryActions.putProfilePhotoInStorage(photoPreviewUri)
                    finalPhotoUrl = userRepositoryProvider.getProfilePhotoUrl()
                }
            }

            val updates = mutableMapOf<String, Any?>(
                AccountsKeys.NAME to newName,
                AccountsKeys.BIRTHDATE to newBirthDate,
                AccountsKeys.AGE to age,
                AccountsKeys.DESCRIPTION to newDescription
            )

            if (finalPhotoUrl != null && finalPhotoUrl != originalPhotoUrl) {
                updates[AccountsKeys.PHOTO_URL] = finalPhotoUrl
            }

            userRepositoryActions.updateUserFields(updates)
            authSessionActions.updateAuthProfile(newName, finalPhotoUrl).getOrThrow()
        }
    }