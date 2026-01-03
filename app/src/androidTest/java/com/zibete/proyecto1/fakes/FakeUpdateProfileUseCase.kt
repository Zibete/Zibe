package com.zibete.proyecto1.fakes

import android.net.Uri
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.profile.UpdateProfileUseCase
import javax.inject.Inject

class FakeUpdateProfileUseCase @Inject constructor(
) : UpdateProfileUseCase {

    override suspend fun execute(
        newName: String,
        newBirthDate: String,
        newDescription: String,
        age: Int,
        originalPhotoUrl: String?,
        photoPreviewUri: Uri?,
        shouldDeletePhoto: Boolean
    ): ZibeResult<Unit> = ZibeResult.Success(Unit)
}