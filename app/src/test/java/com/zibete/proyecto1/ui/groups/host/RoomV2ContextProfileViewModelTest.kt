package com.zibete.proyecto1.ui.groups.host

import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.roomsv2.RoomV2ContextProfile
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Role
import com.zibete.proyecto1.domain.roomsv2.RoomsV2ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoomV2ContextProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Test
    fun anonymousIdentityNeverLoadsAProfile() {
        val repository = FakeProfileRepository()
        val viewModel = RoomV2ContextProfileViewModel(repository)

        viewModel.open(
            roomId = "room-1",
            identity = identity(mode = RoomV2IdentityMode.ANONYMOUS),
        )

        assertEquals(0, repository.calls)
        assertNull(viewModel.uiState.value.profile)
        assertNull(viewModel.uiState.value.loadingIdentityId)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun realIdentityLoadsOnlyContextualProfileData() {
        val profile = RoomV2ContextProfile(
            identityId = "identity-1",
            displayName = "Ada",
            age = 31,
            description = "Construyendo cosas.",
        )
        val repository = FakeProfileRepository(ZibeResult.Success(profile))
        val viewModel = RoomV2ContextProfileViewModel(repository)

        viewModel.open(
            roomId = "room-1",
            identity = identity(mode = RoomV2IdentityMode.REAL),
        )

        assertEquals(1, repository.calls)
        assertEquals("room-1", repository.lastRoomId)
        assertEquals("identity-1", repository.lastIdentityId)
        assertEquals(profile, viewModel.uiState.value.profile)
        assertNull(viewModel.uiState.value.loadingIdentityId)
        assertNull(viewModel.uiState.value.error)
    }

    private fun identity(mode: RoomV2IdentityMode) = RoomV2Identity(
        identityId = "identity-1",
        displayName = "Ada",
        mode = mode,
        role = RoomV2Role.MEMBER,
        active = true,
    )

    private class FakeProfileRepository(
        private val result: ZibeResult<RoomV2ContextProfile> = ZibeResult.Success(),
    ) : RoomsV2ProfileRepository {
        var calls: Int = 0
        var lastRoomId: String? = null
        var lastIdentityId: String? = null

        override suspend fun loadContextProfile(
            roomId: String,
            identityId: String,
        ): ZibeResult<RoomV2ContextProfile> {
            calls += 1
            lastRoomId = roomId
            lastIdentityId = identityId
            return result
        }
    }
}
