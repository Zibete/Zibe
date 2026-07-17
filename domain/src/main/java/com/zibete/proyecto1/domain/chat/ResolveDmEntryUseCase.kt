package com.zibete.proyecto1.domain.chat

import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.ChatRepositoryContract
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

sealed interface DmEntryDecision {
    data object OpenExisting : DmEntryDecision
    data object RequireFirstContactConfirmation : DmEntryDecision
}

interface ResolveDmEntryUseCase {
    suspend operator fun invoke(otherUid: String): ZibeResult<DmEntryDecision>
}

class DefaultResolveDmEntryUseCase @Inject constructor(
    private val chatRepository: ChatRepositoryContract
) : ResolveDmEntryUseCase {

    override suspend fun invoke(otherUid: String): ZibeResult<DmEntryDecision> =
        when (val result = chatRepository.hasConversation(otherUid, NODE_DM)) {
            is ZibeResult.Success -> when (result.data) {
                true -> ZibeResult.Success(DmEntryDecision.OpenExisting)
                false -> ZibeResult.Success(DmEntryDecision.RequireFirstContactConfirmation)
                null -> ZibeResult.Failure(
                    IllegalStateException("DM conversation lookup returned no decision")
                )
            }

            is ZibeResult.Failure -> {
                if (result.exception is CancellationException) throw result.exception
                result
            }
        }
}
