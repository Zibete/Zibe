package com.zibete.proyecto1.domain.session

import com.zibete.proyecto1.core.constants.Constants.ANONYMOUS_USER
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.domain.rooms.RoomOperationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ExitGroupUseCaseTest {
    private lateinit var repository: GroupRepositoryProvider
    private lateinit var preferencesActions: UserPreferencesActions
    private lateinit var preferencesProvider: UserPreferencesProvider
    private lateinit var useCase: ExitGroupUseCase

    @Before
    fun setUp() {
        repository = mockk()
        preferencesActions = mockk(relaxed = true)
        preferencesProvider = mockk()
        every { preferencesProvider.groupContextFlow } returns flowOf(context())
        useCase = DefaultExitGroupUseCase(
            repository,
            preferencesActions,
            preferencesProvider
        )
    }

    @Test
    fun `exit removes only own room membership and preserves private conversations`() = runTest {
        coEvery { repository.leaveRoom(any()) } returns
            ZibeResult.Success(RoomOperationResult.Left(ROOM_KEY))

        val result = useCase.performExitGroupDataCleanup("left the room")

        assertEquals(Unit, (result as ZibeResult.Success).data)
        coVerify(exactly = 1) {
            repository.leaveRoom(
                match {
                    it.roomKey == ROOM_KEY &&
                        it.userName == "Quiet Fox" &&
                        it.eventContent == "left the room"
                }
            )
        }
        coVerify(exactly = 1) { preferencesActions.resetRoomSession() }
    }

    @Test
    fun `remote exit failure preserves local active session`() = runTest {
        val error = IllegalStateException("remote leave failed")
        coEvery { repository.leaveRoom(any()) } returns ZibeResult.Failure(error)

        val result = useCase.performExitGroupDataCleanup("left the room")

        assertSame(error, (result as ZibeResult.Failure).exception)
        coVerify(exactly = 0) { preferencesActions.resetRoomSession() }
    }

    @Test
    fun `exit cancellation is rethrown and does not clear preferences`() = runTest {
        val cancellation = CancellationException("cancelled")
        coEvery { repository.leaveRoom(any()) } returns ZibeResult.Failure(cancellation)

        try {
            useCase.performExitGroupDataCleanup("left the room")
            fail("CancellationException expected")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        coVerify(exactly = 0) { preferencesActions.resetRoomSession() }
    }

    private fun context() = GroupContext(
        inGroup = true,
        groupName = "Legacy display name",
        userName = "Quiet Fox",
        userType = ANONYMOUS_USER,
        roomKey = ROOM_KEY,
        displayName = "Room Norte"
    )

    private companion object {
        const val ROOM_KEY = "room-stable-id"
    }
}
