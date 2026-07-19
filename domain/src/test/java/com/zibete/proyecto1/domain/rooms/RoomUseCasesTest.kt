package com.zibete.proyecto1.domain.rooms

import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.model.RoomIdentity
import com.zibete.proyecto1.model.RoomIdentityType
import com.zibete.proyecto1.model.RoomSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class RoomUseCasesTest {
    private lateinit var repository: GroupRepositoryProvider
    private lateinit var preferencesProvider: UserPreferencesProvider
    private lateinit var preferencesActions: UserPreferencesActions

    @Before
    fun setUp() {
        repository = mockk()
        preferencesProvider = mockk()
        preferencesActions = mockk(relaxed = true)
        every { preferencesProvider.groupContextFlow } returns flowOf(null)
    }

    @Test
    fun `create persists session only after atomic remote creation`() = runTest {
        val session = publicSession()
        coEvery { repository.createRoom(any()) } returns
            ZibeResult.Success(RoomOperationResult.Created(session))
        val useCase = DefaultCreateRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val result = useCase.execute(createCommand())

        assertEquals(RoomOperationResult.Created(session), result.successData())
        coVerify(exactly = 1) {
            repository.createRoom(match { it.roomName == "Room Norte" })
        }
        coVerify(exactly = 1) { preferencesActions.setRoomSession(session) }
    }

    @Test
    fun `room name conflict is an explicit outcome and does not persist`() = runTest {
        coEvery { repository.createRoom(any()) } returns
            ZibeResult.Success(RoomOperationResult.NameInUse("Room Norte"))
        val useCase = DefaultCreateRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val result = useCase.execute(createCommand())

        assertEquals(RoomOperationResult.NameInUse("Room Norte"), result.successData())
        coVerify(exactly = 0) { preferencesActions.setRoomSession(any()) }
    }

    @Test
    fun `anonymous creator is rejected locally without remote write`() = runTest {
        val useCase = DefaultCreateRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val result = useCase.execute(
            createCommand(
                identity = RoomIdentity("Quiet Fox", RoomIdentityType.ANONYMOUS)
            )
        )

        val outcome = result.successData() as RoomOperationResult.ValidationFailed
        assertTrue(
            outcome.issues.any {
                it.field == RoomValidationField.PUBLIC_IDENTITY &&
                    it.error == RoomValidationError.INVALID_VALUE
            }
        )
        coVerify(exactly = 0) { repository.createRoom(any()) }
    }

    @Test
    fun `create requires confirmation and then replaces active room atomically`() = runTest {
        val current = publicSession(roomKey = "room-old", displayName = "Old room")
        val created = publicSession(roomKey = "room-new")
        every { preferencesProvider.groupContextFlow } returns flowOf(current.toContext())
        coEvery {
            repository.createRoom(match {
                it.replaceActiveRoom &&
                    it.previousSession == current &&
                    it.leaveEventContent == "left the room"
            })
        } returns ZibeResult.Success(RoomOperationResult.Created(created))
        val useCase = DefaultCreateRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val confirmation = useCase.execute(createCommand())
        val result = useCase.execute(
            createCommand().copy(
                replaceActiveRoom = true,
                leaveEventContent = "left the room"
            )
        )

        assertEquals(
            RoomOperationResult.SwitchRequired(current, "Room Norte"),
            confirmation.successData()
        )
        assertEquals(RoomOperationResult.Created(created), result.successData())
        coVerify(exactly = 1) { preferencesActions.setRoomSession(created) }
    }

    @Test
    fun `public identity joins and persists authoritative membership session`() = runTest {
        val session = publicSession()
        coEvery { repository.joinRoom(any()) } returns
            ZibeResult.Success(RoomOperationResult.Joined(session))
        val useCase = DefaultJoinRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val result = useCase.execute(joinCommand())

        assertEquals(RoomOperationResult.Joined(session), result.successData())
        coVerify(exactly = 1) { preferencesActions.setRoomSession(session) }
    }

    @Test
    fun `anonymous identity joins without exposing authenticated uid in command`() = runTest {
        val session = anonymousSession()
        val command = joinCommand(
            identity = RoomIdentity("Quiet Fox", RoomIdentityType.ANONYMOUS)
        )
        coEvery { repository.joinRoom(command) } returns
            ZibeResult.Success(RoomOperationResult.Joined(session))
        val useCase = DefaultJoinRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val result = useCase.execute(command)

        assertEquals(RoomOperationResult.Joined(session), result.successData())
        coVerify(exactly = 1) { repository.joinRoom(command) }
        coVerify(exactly = 1) { preferencesActions.setRoomSession(session) }
    }

    @Test
    fun `alias in use is explicit and leaves local session untouched`() = runTest {
        val command = joinCommand(
            identity = RoomIdentity("Quiet Fox", RoomIdentityType.ANONYMOUS)
        )
        coEvery { repository.joinRoom(command) } returns
            ZibeResult.Success(RoomOperationResult.AliasInUse("Quiet Fox"))
        val useCase = DefaultJoinRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val result = useCase.execute(command)

        assertEquals(RoomOperationResult.AliasInUse("Quiet Fox"), result.successData())
        coVerify(exactly = 0) { preferencesActions.setRoomSession(any()) }
    }

    @Test
    fun `join requires explicit switch when another room is active`() = runTest {
        val current = publicSession(roomKey = "room-old")
        every { preferencesProvider.groupContextFlow } returns flowOf(current.toContext())
        val useCase = DefaultJoinRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val result = useCase.execute(joinCommand())

        assertEquals(
            RoomOperationResult.SwitchRequired(current, ROOM_KEY),
            result.successData()
        )
        coVerify(exactly = 0) { repository.joinRoom(any()) }
    }

    @Test
    fun `switch uses current preferences as authority and persists after remote success`() =
        runTest {
            val current = publicSession(roomKey = "room-old", displayName = "Old room")
            val stale = current.copy(roomKey = "stale-room")
            val joined = anonymousSession()
            every { preferencesProvider.groupContextFlow } returns flowOf(current.toContext())
            coEvery {
                repository.switchRoom(match { it.previousSession == current })
            } returns ZibeResult.Success(RoomOperationResult.Switched(current.roomKey, joined))
            val useCase = DefaultSwitchRoomUseCase(
                repository,
                preferencesProvider,
                preferencesActions
            )

            val result = useCase.execute(
                SwitchRoomCommand(stale, joinCommand(), "left the room")
            )

            assertEquals(
                RoomOperationResult.Switched(current.roomKey, joined),
                result.successData()
            )
            coVerify(exactly = 1) { preferencesActions.setRoomSession(joined) }
        }

    @Test
    fun `mark read writes only while public chat is visible`() = runTest {
        val useCase = DefaultMarkRoomReadUseCase(repository)

        val hidden = useCase.execute(MarkRoomReadCommand(ROOM_KEY, 123L, false))
        coEvery { repository.markRoomAsRead(ROOM_KEY, 456L) } returns
            ZibeResult.Success(Unit)
        val visible = useCase.execute(MarkRoomReadCommand(ROOM_KEY, 456L, true))

        assertEquals(MarkRoomReadResult.SkippedNotVisible, hidden.successData())
        assertEquals(MarkRoomReadResult.Marked(ROOM_KEY, 456L), visible.successData())
        coVerify(exactly = 1) { repository.markRoomAsRead(ROOM_KEY, 456L) }
    }

    @Test
    fun `resume verifies remote membership before restoring local session`() = runTest {
        val session = anonymousSession()
        coEvery { repository.resolveRoomSession(ROOM_KEY) } returns
            ZibeResult.Success(session)
        val useCase = DefaultResumeRoomSessionUseCase(repository, preferencesActions)

        val result = useCase.execute(ROOM_KEY)

        assertEquals(session, result.successData())
        coVerify(exactly = 1) { preferencesActions.setRoomSession(session) }
    }

    @Test
    fun `resume missing membership fails and does not restore preferences`() = runTest {
        coEvery { repository.resolveRoomSession(ROOM_KEY) } returns ZibeResult.Success(null)
        val useCase = DefaultResumeRoomSessionUseCase(repository, preferencesActions)

        val result = useCase.execute(ROOM_KEY)

        assertTrue((result as ZibeResult.Failure).exception is RoomMembershipNotFoundException)
        coVerify(exactly = 0) { preferencesActions.setRoomSession(any()) }
    }

    @Test
    fun `resume repository failure is preserved without changing preferences`() = runTest {
        val error = IllegalStateException("membership lookup failed")
        coEvery { repository.resolveRoomSession(ROOM_KEY) } returns ZibeResult.Failure(error)
        val useCase = DefaultResumeRoomSessionUseCase(repository, preferencesActions)

        val result = useCase.execute(ROOM_KEY)

        assertSame(error, (result as ZibeResult.Failure).exception)
        coVerify(exactly = 0) { preferencesActions.setRoomSession(any()) }
    }

    @Test
    fun `repository error is preserved and cancellation is rethrown`() = runTest {
        val error = IllegalStateException("remote failed")
        coEvery { repository.createRoom(any()) } returns ZibeResult.Failure(error)
        val create = DefaultCreateRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )

        val failure = create.execute(createCommand())

        assertSame(error, (failure as ZibeResult.Failure).exception)

        val cancellation = CancellationException("cancelled")
        coEvery { repository.joinRoom(any()) } returns ZibeResult.Failure(cancellation)
        val join = DefaultJoinRoomUseCase(
            repository,
            preferencesProvider,
            preferencesActions
        )
        try {
            join.execute(joinCommand())
            fail("CancellationException expected")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    private fun createCommand(
        identity: RoomIdentity = RoomIdentity("Public User", RoomIdentityType.PUBLIC)
    ) = CreateRoomCommand(
        roomName = " Room Norte ",
        description = " A useful room ",
        identity = identity,
        eventContent = "joined the room"
    )

    private fun joinCommand(
        identity: RoomIdentity = RoomIdentity("Public User", RoomIdentityType.PUBLIC)
    ) = JoinRoomCommand(
        roomKey = ROOM_KEY,
        displayName = "Room Norte",
        identity = identity,
        eventContent = "joined the room"
    )

    private fun publicSession(
        roomKey: String = ROOM_KEY,
        displayName: String = "Room Norte"
    ) = RoomSession(roomKey, displayName, "Public User", PUBLIC_USER)

    private fun anonymousSession() =
        RoomSession(ROOM_KEY, "Room Norte", "Quiet Fox", ANONYMOUS_USER)

    private fun RoomSession.toContext() = GroupContext(
        inGroup = true,
        groupName = roomKey,
        userName = userName,
        userType = userType,
        roomKey = roomKey,
        displayName = displayName
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> ZibeResult<T>.successData(): T =
        (this as ZibeResult.Success<T>).data as T

    private companion object {
        const val ROOM_KEY = "room-stable-id"
    }
}
