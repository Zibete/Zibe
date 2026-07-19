package com.zibete.proyecto1.ui.groups

import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.domain.rooms.CreateRoomUseCase
import com.zibete.proyecto1.domain.rooms.JoinRoomUseCase
import com.zibete.proyecto1.domain.rooms.RoomFailureReason
import com.zibete.proyecto1.domain.rooms.RoomOperationException
import com.zibete.proyecto1.domain.rooms.RoomOperationResult
import com.zibete.proyecto1.domain.rooms.RoomValidationError
import com.zibete.proyecto1.domain.rooms.RoomValidationField
import com.zibete.proyecto1.domain.rooms.RoomValidationIssue
import com.zibete.proyecto1.domain.rooms.SwitchRoomUseCase
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.RoomIdentityType
import com.zibete.proyecto1.model.RoomSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `load renders real rooms and search matches name or description`() = runTest {
        val harness = harness()
        val rooms = listOf(
            room("music", "Música", "Para escuchar discos"),
            room("android", "Android", "Arquitectura móvil")
        )
        coEvery { harness.repository.loadRooms() } returns ZibeResult.Success(rooms)

        harness.vm.loadRooms()
        runCurrent()

        assertEquals(rooms, harness.vm.uiState.value.visibleRooms)
        assertFalse(harness.vm.uiState.value.isLoading)
        assertNull(harness.vm.uiState.value.error)

        harness.vm.onSearchQueryChanged("  movil  ")
        assertEquals(listOf("android"), harness.vm.uiState.value.visibleRooms.map { it.roomKey })

        harness.vm.onSearchQueryChanged("MUSICA")
        assertEquals(listOf("music"), harness.vm.uiState.value.visibleRooms.map { it.roomKey })
    }

    @Test
    fun `refresh preserves current content until replacement succeeds`() = runTest {
        val harness = harness()
        val initial = room("initial", "Inicial", "Descripción")
        coEvery { harness.repository.loadRooms() } returns ZibeResult.Success(listOf(initial))
        harness.vm.loadRooms()
        runCurrent()

        val refresh = CompletableDeferred<ZibeResult<List<Groups>>>()
        coEvery { harness.repository.loadRooms() } coAnswers { refresh.await() }
        harness.vm.refreshRooms()
        runCurrent()

        assertTrue(harness.vm.uiState.value.isRefreshing)
        assertEquals(listOf(initial), harness.vm.uiState.value.visibleRooms)

        val updated = room("updated", "Actualizada", "Otra descripción")
        refresh.complete(ZibeResult.Success(listOf(updated)))
        runCurrent()

        assertFalse(harness.vm.uiState.value.isRefreshing)
        assertEquals(listOf(updated), harness.vm.uiState.value.visibleRooms)
    }

    @Test
    fun `initial load failure exposes retry state without fake empty content`() = runTest {
        val harness = harness()
        coEvery { harness.repository.loadRooms() } returns
            ZibeResult.Failure(IllegalStateException("offline"))

        harness.vm.loadRooms()
        runCurrent()

        assertTrue(harness.vm.uiState.value.rooms.isEmpty())
        assertEquals(
            UiText.StringRes(R.string.rooms_error_message),
            harness.vm.uiState.value.error
        )
    }

    @Test
    fun `public join navigates only after use case success`() = runTest {
        val harness = harness()
        val room = room("target", "Target", "Descripción")
        val session = RoomSession("target", "Target", MY_NAME, PUBLIC_USER)
        coEvery { harness.joinRoom.execute(any()) } returns
            ZibeResult.Success(RoomOperationResult.Joined(session))
        harness.vm.onRoomSelected(room)
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.submitSheet("joined", "left")

        assertEquals(GroupsUiEvent.NavigateToGroupHost, event.await())
        assertNull(harness.vm.uiState.value.sheet)
        coVerify(exactly = 1) {
            harness.joinRoom.execute(
                match {
                    it.roomKey == "target" &&
                        it.identity.type == RoomIdentityType.PUBLIC &&
                        it.identity.displayName == MY_NAME
                }
            )
        }
    }

    @Test
    fun `anonymous alias conflict stays in join sheet with inline error`() = runTest {
        val harness = harness()
        harness.vm.onRoomSelected(room("target", "Target", "Descripción"))
        harness.vm.onIdentitySelected(RoomIdentityType.ANONYMOUS)
        harness.vm.onAliasChanged("Quiet Fox")
        coEvery { harness.joinRoom.execute(any()) } returns
            ZibeResult.Success(RoomOperationResult.AliasInUse("Quiet Fox"))

        harness.vm.submitSheet("joined", "left")
        runCurrent()

        assertNotNull(harness.vm.uiState.value.sheet)
        assertEquals(
            UiText.StringRes(R.string.group_nick_in_use, listOf("Quiet Fox")),
            harness.vm.uiState.value.identityError
        )
        assertFalse(harness.vm.uiState.value.isSubmitting)
    }

    @Test
    fun `switch confirmation preserves private history policy and delegates atomic switch`() =
        runTest {
            val current = RoomSession("old", "Anterior", MY_NAME, PUBLIC_USER)
            val harness = harness(current)
            val target = room("target", "Destino", "Descripción")
            coEvery { harness.joinRoom.execute(any()) } returns ZibeResult.Success(
                RoomOperationResult.SwitchRequired(current, "target")
            )
            val joined = RoomSession("target", "Destino", "Quiet Fox", ANONYMOUS_USER)
            coEvery { harness.switchRoom.execute(any()) } returns ZibeResult.Success(
                RoomOperationResult.Switched("old", joined)
            )
            runCurrent()
            harness.vm.onRoomSelected(target)
            harness.vm.onIdentitySelected(RoomIdentityType.ANONYMOUS)
            harness.vm.onAliasChanged("Quiet Fox")

            harness.vm.submitSheet("joined", "left")
            runCurrent()

            assertNotNull(harness.vm.uiState.value.pendingSwitch)
            val event = async { awaitEvent(harness.vm) }
            runCurrent()
            harness.vm.confirmSwitch("left")

            assertEquals(GroupsUiEvent.NavigateToGroupHost, event.await())
            coVerify(exactly = 1) {
                harness.switchRoom.execute(
                    match {
                        it.previousSession == current &&
                            it.target.roomKey == "target" &&
                            it.leaveEventContent == "left"
                    }
                )
            }
        }

    @Test
    fun `create validation maps domain issues to form fields`() = runTest {
        val harness = harness()
        harness.vm.onCreateRoomRequested()
        harness.vm.onRoomNameChanged("x")
        harness.vm.onRoomDescriptionChanged("Description")
        coEvery { harness.createRoom.execute(any()) } returns ZibeResult.Success(
            RoomOperationResult.ValidationFailed(
                listOf(
                    issue(RoomValidationField.ROOM_NAME),
                    issue(RoomValidationField.PUBLIC_IDENTITY)
                )
            )
        )

        harness.vm.submitSheet("joined", "left")
        runCurrent()

        assertEquals(
            UiText.StringRes(R.string.rooms_invalid_name),
            harness.vm.uiState.value.roomNameError
        )
        assertEquals(
            UiText.StringRes(R.string.rooms_public_identity_missing),
            harness.vm.uiState.value.identityError
        )
        assertFalse(harness.vm.uiState.value.isSubmitting)
    }

    @Test
    fun `create always resets and submits public identity without stale alias`() = runTest {
        val harness = harness()
        harness.vm.onRoomSelected(room("target", "Target", "Descripción"))
        harness.vm.onIdentitySelected(RoomIdentityType.ANONYMOUS)
        harness.vm.onAliasChanged("Quiet Fox")

        harness.vm.onCreateRoomRequested()

        assertEquals(RoomIdentityType.PUBLIC, harness.vm.uiState.value.identityType)
        assertEquals("", harness.vm.uiState.value.alias)
        harness.vm.onRoomNameChanged("Nueva sala")
        harness.vm.onRoomDescriptionChanged("Descripción")
        coEvery { harness.createRoom.execute(any()) } returns ZibeResult.Success(
            RoomOperationResult.Created(
                RoomSession("new-room", "Nueva sala", MY_NAME, PUBLIC_USER)
            )
        )

        harness.vm.submitSheet("joined", "left")
        runCurrent()

        coVerify(exactly = 1) {
            harness.createRoom.execute(match {
                it.identity.type == RoomIdentityType.PUBLIC &&
                    it.identity.displayName == MY_NAME
            })
        }
    }

    @Test
    fun `permission failure maps to configuration-safe message`() = runTest {
        val harness = harness()
        harness.vm.onRoomSelected(room("target", "Target", "Descripción"))
        coEvery { harness.joinRoom.execute(any()) } returns ZibeResult.Failure(
            RoomOperationException(RoomFailureReason.PERMISSION)
        )
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.submitSheet("joined", "left")

        val snack = event.await() as GroupsUiEvent.ShowSnack
        assertEquals(UiText.StringRes(R.string.rooms_error_permission), snack.uiText)
    }

    @Test
    fun `operation failure releases submit and emits safe error`() = runTest {
        val harness = harness()
        harness.vm.onRoomSelected(room("target", "Target", "Descripción"))
        coEvery { harness.joinRoom.execute(any()) } returns
            ZibeResult.Failure(IllegalStateException("provider detail"))
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.submitSheet("joined", "left")

        val snack = event.await() as GroupsUiEvent.ShowSnack
        assertEquals(UiText.StringRes(R.string.rooms_action_error), snack.uiText)
        assertFalse(harness.vm.uiState.value.isSubmitting)
        assertNotNull(harness.vm.uiState.value.sheet)
    }

    @Test
    fun `operation cancellation is rethrown by domain boundary and releases submit`() = runTest {
        val harness = harness()
        harness.vm.onRoomSelected(room("target", "Target", "Descripción"))
        coEvery { harness.joinRoom.execute(any()) } throws CancellationException("cancelled")

        harness.vm.submitSheet("joined", "left")
        runCurrent()

        assertFalse(harness.vm.uiState.value.isSubmitting)
        assertNotNull(harness.vm.uiState.value.sheet)
    }

    @Test
    fun `selecting active room opens host without a redundant join`() = runTest {
        val current = RoomSession("active", "Activa", MY_NAME, PUBLIC_USER)
        val harness = harness(current)
        runCurrent()
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.onRoomSelected(room("active", "Activa", "Descripción"))

        assertEquals(GroupsUiEvent.NavigateToGroupHost, event.await())
        assertNull(harness.vm.uiState.value.sheet)
        coVerify(exactly = 0) { harness.joinRoom.execute(any()) }
    }

    private fun harness(activeSession: RoomSession? = null): Harness {
        val repository = mockk<GroupRepositoryProvider>()
        val local = mockk<LocalRepositoryProvider>()
        val preferences = mockk<UserPreferencesProvider>()
        val createRoom = mockk<CreateRoomUseCase>()
        val joinRoom = mockk<JoinRoomUseCase>()
        val switchRoom = mockk<SwitchRoomUseCase>()
        val context = MutableStateFlow(activeSession?.toContext())
        every { local.myUserName } returns MY_NAME
        every { local.myProfilePhotoUrl } returns "https://example.test/profile.jpg"
        every { preferences.groupContextFlow } returns context
        val vm = GroupsViewModel(
            groupRepository = repository,
            localRepositoryProvider = local,
            userPreferencesProvider = preferences,
            createRoomUseCase = createRoom,
            joinRoomUseCase = joinRoom,
            switchRoomUseCase = switchRoom
        )
        return Harness(vm, repository, createRoom, joinRoom, switchRoom)
    }

    private suspend fun awaitEvent(vm: GroupsViewModel): GroupsUiEvent =
        withTimeout(3_000) { vm.events.first() }

    private fun room(key: String, name: String, description: String) = Groups(
        name = name,
        description = description,
        users = 3,
        roomId = key,
        roomKey = key,
        displayName = name,
        lastActivityAt = 123L
    )

    private fun RoomSession.toContext() = GroupContext(
        inGroup = true,
        groupName = roomKey,
        userName = userName,
        userType = userType,
        roomKey = roomKey,
        displayName = displayName
    )

    private fun issue(field: RoomValidationField) =
        RoomValidationIssue(field, RoomValidationError.INVALID_VALUE)

    private data class Harness(
        val vm: GroupsViewModel,
        val repository: GroupRepositoryProvider,
        val createRoom: CreateRoomUseCase,
        val joinRoom: JoinRoomUseCase,
        val switchRoom: SwitchRoomUseCase
    )

    private companion object {
        const val MY_NAME = "Public User"
    }
}
