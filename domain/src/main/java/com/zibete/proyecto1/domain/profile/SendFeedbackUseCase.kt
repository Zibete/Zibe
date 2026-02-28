package com.zibete.proyecto1.domain.profile

import com.zibete.proyecto1.core.device.DeviceInfoProvider
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.UserRepositoryActions
import jakarta.inject.Inject

interface SendFeedbackUseCase {
    suspend fun execute(
        feedback: String,
        screen: String
    ): ZibeResult<Unit>
}

class DefaultSendFeedbackUseCase @Inject constructor(
    private val userRepositoryActions: UserRepositoryActions,
    private val deviceInfoProvider: DeviceInfoProvider
) : SendFeedbackUseCase {
    override suspend fun execute(
        feedback: String,
        screen: String
    ): ZibeResult<Unit> = zibeCatching {
        userRepositoryActions.sendFeedback(
            feedback = feedback,
            screen = screen,
            model = deviceInfoProvider.getModel(),
            appVersion = deviceInfoProvider.getAppVersion()
        ).getOrThrow()
    }
}