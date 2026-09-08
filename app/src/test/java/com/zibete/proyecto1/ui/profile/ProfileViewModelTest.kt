package com.zibete.proyecto1.ui.profile

import androidx.lifecycle.SavedStateHandle
import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.ui.SnackBarManager
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.data.LocationRepositoryProvider
import com.zibete.proyecto1.data.profile.BlockState
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.domain.chat.DmEntryDecision
import com.zibete.proyecto1.domain.chat.ResolveDmEntryUseCase
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.components.ZibeSnackType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `existing conversation opens the exact profile uid`() = runTest {
        val harness = loadedHarness(DmEntryDecision.OpenExisting)
        val event = async { awaitDirectMessage(harness.vm) }
        runCurrent()

        harness.vm.onDmChatRequested()

        assertEquals(ChatSessionUiEvent.OpenDirectMessage(OTHER_UID), event.await())
        awaitState(harness.vm) { !it.isDmEntryLoading }
        assertNull(harness.vm.uiState.value.pendingFirstContactUserId)
    }

    @Test
    fun `new conversation waits for shared first contact confirmation`() = runTest {
        val harness = loadedHarness(DmEntryDecision.RequireFirstContactConfirmation)

        harness.vm.onDmChatRequested()

        val pending = awaitState(harness.vm) {
            !it.isDmEntryLoading && it.pendingFirstContactUserId == OTHER_UID
        }
        assertEquals(OTHER_UID, pending.pendingFirstContactUserId)

        val event = async { awaitDirectMessage(harness.vm) }
        runCurrent()
        harness.vm.confirmFirstContact()

        assertEquals(ChatSessionUiEvent.OpenDirectMessage(OTHER_UID), event.await())
        assertNull(harness.vm.uiState.value.pendingFirstContactUserId)
    }

    @Test
    fun `first contact cancellation clears pending state without navigation`() = runTest {
        val harness = loadedHarness(DmEntryDecision.RequireFirstContactConfirmation)
        val events = mutableListOf<ChatSessionUiEvent>()
        harness.vm.events.onEach(events::add).launchIn(backgroundScope)

        harness.vm.onDmChatRequested()
        awaitState(harness.vm) { it.pendingFirstContactUserId == OTHER_UID }
        harness.vm.cancelFirstContact()
        runCurrent()

        assertNull(harness.vm.uiState.value.pendingFirstContactUserId)
        assertTrue(events.none { it is ChatSessionUiEvent.OpenDirectMessage })
    }

    @Test
    fun `gate failure shows snackbar and releases loading without navigation`() = runTest {
        val harness = loadedHarness(
            ZibeResult.Failure(IllegalStateException("sin conexión"))
        )
        val events = mutableListOf<ChatSessionUiEvent>()
        harness.vm.events.onEach(events::add).launchIn(backgroundScope)
        val snack = async { withTimeout(3_000) { harness.snackBarManager.events.first() } }
        runCurrent()

        harness.vm.onDmChatRequested()

        assertEquals(ZibeSnackType.ERROR, snack.await().type)
        val terminal = awaitState(harness.vm) { !it.isDmEntryLoading }
        assertNull(terminal.pendingFirstContactUserId)
        assertTrue(events.none { it is ChatSessionUiEvent.OpenDirectMessage })
    }

    @Test
    fun `double tap performs one gate lookup and keeps loading until terminal state`() = runTest {
        val harness = loadedHarness(DmEntryDecision.OpenExisting)
        val resolution = CompletableDeferred<ZibeResult<DmEntryDecision>>()
        coEvery { harness.gate(OTHER_UID) } coAnswers { resolution.await() }

        harness.vm.onDmChatRequested()
        harness.vm.onDmChatRequested()
        runCurrent()

        assertTrue(harness.vm.uiState.value.isDmEntryLoading)
        coVerify(exactly = 1) { harness.gate(OTHER_UID) }

        resolution.complete(
            ZibeResult.Success(DmEntryDecision.RequireFirstContactConfirmation)
        )
        val terminal = awaitState(harness.vm) {
            !it.isDmEntryLoading && it.pendingFirstContactUserId == OTHER_UID
        }
        assertEquals(OTHER_UID, terminal.pendingFirstContactUserId)
    }

    @Test
    fun `inactive pager page cancels pending gate and cannot navigate stale uid`() = runTest {
        val harness = loadedHarness(DmEntryDecision.OpenExisting)
        val resolution = CompletableDeferred<ZibeResult<DmEntryDecision>>()
        val events = mutableListOf<ChatSessionUiEvent>()
        harness.vm.events.onEach(events::add).launchIn(backgroundScope)
        coEvery { harness.gate(OTHER_UID) } coAnswers { resolution.await() }

        harness.vm.onDmChatRequested()
        runCurrent()
        assertTrue(harness.vm.uiState.value.isDmEntryLoading)

        harness.vm.onPageActiveChanged(false)
        resolution.complete(ZibeResult.Success(DmEntryDecision.OpenExisting))
        runCurrent()

        val state = harness.vm.uiState.value
        assertFalse(state.isDmEntryLoading)
        assertNull(state.pendingFirstContactUserId)
        assertTrue(events.none { it is ChatSessionUiEvent.OpenDirectMessage })
    }

    @Test
    fun `reactivating pager page does not replay completed navigation`() = runTest {
        val harness = loadedHarness(DmEntryDecision.OpenExisting)
        val events = mutableListOf<ChatSessionUiEvent>()
        harness.vm.events.onEach(events::add).launchIn(backgroundScope)
        runCurrent()

        harness.vm.onDmChatRequested()
        awaitState(harness.vm) { !it.isDmEntryLoading }
        runCurrent()
        assertEquals(1, events.count { it is ChatSessionUiEvent.OpenDirectMessage })

        harness.vm.onPageActiveChanged(false)
        harness.vm.onPageActiveChanged(true)
        runCurrent()

        assertEquals(1, events.count { it is ChatSessionUiEvent.OpenDirectMessage })
    }

    private suspend fun loadedHarness(
        decision: DmEntryDecision
    ): Harness = loadedHarness(ZibeResult.Success(decision))

    private suspend fun loadedHarness(
        resolution: ZibeResult<DmEntryDecision>
    ): Harness {
        val profileProvider = mockk<ProfileRepositoryProvider>()
        val chatRepository = mockk<ChatRepositoryContract>(relaxed = true)
        val locationRepository = mockk<LocationRepositoryProvider>()
        val gate = mockk<ResolveDmEntryUseCase>()
        val snackBarManager = SnackBarManager()

        every { profileProvider.observeUserStatus(OTHER_UID, NODE_DM) } returns
            kotlinx.coroutines.flow.flowOf(UserStatus.Offline)
        coEvery { profileProvider.getOtherAccount(OTHER_UID) } returns ZibeResult.Success(user())
        coEvery { profileProvider.getMyChatState(OTHER_UID) } returns ZibeResult.Success("")
        coEvery { profileProvider.isFavorite(OTHER_UID) } returns ZibeResult.Success(false)
        coEvery { profileProvider.getBlockStateWith(OTHER_UID, NODE_DM) } returns
            ZibeResult.Success(BlockState(isBlockedByMe = false, hasBlockedMe = false))
        coEvery { profileProvider.getDmPhotoList(OTHER_UID, NODE_DM) } returns
            ZibeResult.Success(emptyList())
        coEvery { locationRepository.getDistanceToUser(OTHER_UID) } returns
            ZibeResult.Success(DISTANCE_LABEL)
        coEvery { chatRepository.hasConversation(OTHER_UID, NODE_DM) } returns
            ZibeResult.Success(false)
        coEvery { gate(OTHER_UID) } returns resolution

        val vm = ProfileViewModel(
            savedStateHandle = SavedStateHandle(mapOf(EXTRA_USER_ID to OTHER_UID)),
            chatRepository = chatRepository,
            locationRepository = locationRepository,
            profileRepositoryProvider = profileProvider,
            profileRepositoryActions = mockk<ProfileRepositoryActions>(relaxed = true),
            resolveDmEntry = gate,
            snackBarManager = snackBarManager
        )
        vm.loadProfile()
        awaitState(vm) {
            it.content is ProfileContent.Ready && it.distanceLabel == DISTANCE_LABEL
        }
        return Harness(vm, gate, snackBarManager)
    }

    private suspend fun awaitState(
        vm: ProfileViewModel,
        predicate: (ProfileUiState) -> Boolean
    ): ProfileUiState = withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(3_000) { vm.uiState.first(predicate) }
    }

    private suspend fun awaitDirectMessage(vm: ProfileViewModel): ChatSessionUiEvent =
        withTimeout(3_000) { vm.events.first { it is ChatSessionUiEvent.OpenDirectMessage } }

    private fun user() = Users(
        id = OTHER_UID,
        name = "Ada",
        birthDate = "1997-01-01"
    )

    private data class Harness(
        val vm: ProfileViewModel,
        val gate: ResolveDmEntryUseCase,
        val snackBarManager: SnackBarManager
    )

    private companion object {
        const val DISTANCE_LABEL = "1 km"
        const val OTHER_UID = "profile-uid"
    }
}
