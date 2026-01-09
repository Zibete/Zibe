package com.zibete.proyecto1.domain.session

import com.zibete.proyecto1.core.constants.Constants.MSG_INFO
import com.zibete.proyecto1.core.constants.EXIT_GROUP_ERR_EXCEPTION
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.core.utils.getOrThrow
import com.zibete.proyecto1.core.utils.zibeCatching
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface ExitGroupUseCase {
    suspend fun performExitGroupDataCleanup(
        message: String
    ): ZibeResult<Unit>
}

class DefaultExitGroupUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userPreferencesActions: UserPreferencesActions,
    private val userPreferencesProvider: UserPreferencesProvider
) : ExitGroupUseCase {

    override suspend fun performExitGroupDataCleanup(
        message: String
    ): ZibeResult<Unit> = zibeCatching {

        // 1. Obtenemos el contexto actual.
        val groupContext = userPreferencesProvider.groupContextFlow.first()
            ?: throw IllegalStateException(EXIT_GROUP_ERR_EXCEPTION)

        val groupName = groupContext.groupName

        // 2. Ejecución de tareas remotas.
        groupRepository.removeMyGroupChatList().getOrThrow()
        groupRepository.removeMyPrivateGroupChats().getOrThrow()

        groupRepository.sendGroupMessage(
            groupName = groupName,
            userName = groupContext.userName,
            userType = groupContext.userType,
            chatType = MSG_INFO,
            content = message,
        ).getOrThrow()

        groupRepository.removeUserFromGroup(
            groupName = groupName
        ).getOrThrow()

        // 3. Limpieza de estado local
        userPreferencesActions.resetGroupState()
    }
}