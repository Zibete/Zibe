package com.zibete.proyecto1.ui.users

import com.zibete.proyecto1.MainDispatcherRule
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.LocationRepositoryProvider
import com.zibete.proyecto1.data.UserDirectoryProvider
import com.zibete.proyecto1.data.profile.ProfileRepositoryActions
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.fakes.FakeUserPreferencesActions
import com.zibete.proyecto1.fakes.FakeUserPreferencesProvider
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.testing.TestScenario
import com.zibete.proyecto1.ui.components.ZibeSnackType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UsersViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `load shows progress then sorts valid users and enriches metadata`() = runTest {
        val loadGate = CompletableDeferred<List<Users>>()
        val harness = harness()
        coEvery { harness.directory.getAllAccounts() } coAnswers { loadGate.await() }
        coEvery { harness.directory.getFavoriteUserIds(MY_UID) } returns setOf(NEAR_UID)
        coEvery { harness.directory.getConversationStates(MY_UID) } returns mapOf(
            NEAR_UID to CHAT_STATE_SILENT,
            FAR_UID to CHAT_STATE_BLOCKED
        )

        harness.vm.loadUsers()
        runCurrent()

        assertTrue(harness.vm.uiState.value.isLoading)
        loadGate.complete(
            listOf(
                user(MY_UID, "Yo", 30, distance = 1.0),
                user(FAR_UID, "Lejos", 29, distance = 900.0),
                user("", "Inválido", 28, distance = 5.0),
                user(NEAR_UID, "Cerca", 27, distance = 100.0)
            )
        )

        val state = awaitState(harness.vm) {
            it.users.size == 2 && it.users.first().isFavorite
        }

        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(listOf(NEAR_UID, FAR_UID), state.users.map { it.id })
        assertTrue(state.users.first().isNotificationsSilenced)
        assertTrue(state.users.last().isBlockedByMe)
    }

    @Test
    fun `empty directory produces an empty non loading state`() = runTest {
        val harness = harness(users = emptyList())

        harness.vm.loadUsers()
        runCurrent()

        val state = harness.vm.uiState.value
        assertTrue(state.users.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.error)
        coVerify(exactly = 1) { harness.directory.getAllAccounts() }
    }

    @Test
    fun `directory failure exposes error and emits error snack`() = runTest {
        val harness = harness()
        val failure = IllegalStateException("sin conexión")
        coEvery { harness.directory.getAllAccounts() } throws failure
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.loadUsers()

        val state = awaitState(harness.vm) { !it.isLoading && it.error != null }
        val snack = event.await() as UsersUiEvent.ShowSnack
        assertNotNull(state.error)
        assertEquals(ZibeSnackType.ERROR, snack.snackType)
    }

    @Test
    fun `search is trimmed case insensitive and can be cleared`() = runTest {
        val harness = loadedHarness(
            user(NEAR_UID, "Ámbar", 25),
            user(FAR_UID, "Bruno", 26)
        )

        harness.vm.onSearchQueryChanged("  BRU  ")

        assertEquals(listOf(FAR_UID), harness.vm.uiState.value.users.map { it.id })
        assertEquals("  BRU  ", harness.vm.uiState.value.searchQuery)

        harness.vm.onSearchQueryChanged(null)

        assertEquals(listOf(NEAR_UID, FAR_UID), harness.vm.uiState.value.users.map { it.id })
        assertEquals("", harness.vm.uiState.value.searchQuery)
    }

    @Test
    fun `online filter keeps online users and persists preference`() = runTest {
        val harness = loadedHarness(
            user(NEAR_UID, "Online", 25, online = true),
            user(FAR_UID, "Offline", 25, online = false)
        )

        harness.vm.onOnlineFilterChanged(true)
        runCurrent()

        assertEquals(listOf(NEAR_UID), harness.vm.uiState.value.users.map { it.id })
        assertTrue(harness.vm.uiState.value.applyOnlineFilter)
        assertTrue(harness.scenario.applyOnlineFilter)
        assertTrue(harness.scenario.filterSwitch)
    }

    @Test
    fun `age filter applies inclusive range and persists normalized bounds`() = runTest {
        val harness = loadedHarness(
            user(NEAR_UID, "Joven", 22),
            user(FAR_UID, "Mayor", 41)
        )

        harness.vm.applyFilters(
            applyAgeFilter = true,
            applyOnlineFilter = false,
            minAge = 20,
            maxAge = 30
        )
        runCurrent()

        val state = harness.vm.uiState.value
        assertEquals(listOf(NEAR_UID), state.users.map { it.id })
        assertTrue(state.applyAgeFilter)
        assertEquals(20, state.minAge)
        assertEquals(30, state.maxAge)
        assertEquals(20, harness.scenario.minAge)
        assertEquals(30, harness.scenario.maxAge)
    }

    @Test
    fun `clear filters restores all users and resets persisted switch`() = runTest {
        val harness = loadedHarness(
            user(NEAR_UID, "Online joven", 22, online = true),
            user(FAR_UID, "Offline mayor", 41, online = false)
        )
        harness.vm.applyFilters(true, true, 18, 25)
        runCurrent()

        harness.vm.clearFilters()
        runCurrent()

        val state = harness.vm.uiState.value
        assertEquals(listOf(NEAR_UID, FAR_UID), state.users.map { it.id })
        assertFalse(state.hasActiveFilters)
        assertEquals(18, state.minAge)
        assertEquals(99, state.maxAge)
        assertFalse(harness.scenario.filterSwitch)
        assertEquals(0, harness.scenario.minAge)
        assertEquals(0, harness.scenario.maxAge)
    }

    @Test
    fun `refresh preserves content while loading and replaces it on success`() = runTest {
        val refreshGate = CompletableDeferred<List<Users>>()
        val harness = harness(users = listOf(user(NEAR_UID, "Inicial", 25)))
        harness.vm.loadUsers()
        awaitState(harness.vm) { !it.isLoading && it.users.size == 1 }
        coEvery { harness.directory.getAllAccounts() } coAnswers { refreshGate.await() }
        coEvery { harness.directory.getConversationStates(MY_UID) } returns mapOf(
            FAR_UID to CHAT_STATE_SILENT
        )

        harness.vm.loadUsers()
        runCurrent()

        val refreshing = harness.vm.uiState.value
        assertTrue(refreshing.isRefreshing)
        assertEquals(listOf(NEAR_UID), refreshing.users.map { it.id })

        refreshGate.complete(listOf(user(FAR_UID, "Actualizado", 27)))
        val refreshed = awaitState(harness.vm) {
            !it.isRefreshing &&
                it.users.singleOrNull()?.id == FAR_UID &&
                it.users.single().isNotificationsSilenced
        }

        assertFalse(refreshed.isLoading)
        assertNull(refreshed.error)
    }

    @Test
    fun `existing conversation navigates directly to chat`() = runTest {
        val harness = loadedHarness(user(NEAR_UID, "Cerca", 25))
        coEvery { harness.chat.hasConversation(NEAR_UID, NODE_DM) } returns
            ZibeResult.Success(true)
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.onUserChatClick(NEAR_UID)

        assertEquals(UsersUiEvent.NavigateToChat(NEAR_UID), event.await())
        awaitState(harness.vm) { it.chatCheckUserId == null }
        assertNull(harness.vm.uiState.value.pendingFirstContact)
    }

    @Test
    fun `new conversation requires confirmation before navigation`() = runTest {
        val harness = loadedHarness(user(NEAR_UID, "Cerca", 25))
        coEvery { harness.chat.hasConversation(NEAR_UID, NODE_DM) } returns
            ZibeResult.Success(false)

        harness.vm.onUserChatClick(NEAR_UID)

        val pending = awaitState(harness.vm) { it.pendingFirstContact != null }
        assertEquals(NEAR_UID, pending.pendingFirstContact?.id)
        assertNull(pending.chatCheckUserId)

        val event = async { awaitEvent(harness.vm) }
        runCurrent()
        harness.vm.confirmFirstContact()

        assertEquals(UsersUiEvent.NavigateToChat(NEAR_UID), event.await())
        assertNull(harness.vm.uiState.value.pendingFirstContact)
    }

    @Test
    fun `first contact can be cancelled without navigation`() = runTest {
        val harness = loadedHarness(user(NEAR_UID, "Cerca", 25))
        coEvery { harness.chat.hasConversation(NEAR_UID, NODE_DM) } returns
            ZibeResult.Success(false)
        harness.vm.onUserChatClick(NEAR_UID)
        awaitState(harness.vm) { it.pendingFirstContact != null }

        harness.vm.cancelFirstContact()

        assertNull(harness.vm.uiState.value.pendingFirstContact)
    }

    @Test
    fun `conversation check failure emits error and releases pending action`() = runTest {
        val harness = loadedHarness(user(NEAR_UID, "Cerca", 25))
        coEvery { harness.chat.hasConversation(NEAR_UID, NODE_DM) } returns
            ZibeResult.Failure(IllegalStateException("falló chat"))
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.onUserChatClick(NEAR_UID)

        val snack = event.await() as UsersUiEvent.ShowSnack
        assertEquals(ZibeSnackType.ERROR, snack.snackType)
        awaitState(harness.vm) { it.chatCheckUserId == null }
        assertNull(harness.vm.uiState.value.pendingFirstContact)
    }

    @Test
    fun `profile navigation uses visible user order and selected index`() = runTest {
        val harness = loadedHarness(
            user(NEAR_UID, "Ámbar", 25),
            user(FAR_UID, "Bruno", 26)
        )
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.onUserProfileClick(FAR_UID)

        val navigation = event.await() as UsersUiEvent.NavigateToProfile
        assertEquals(arrayListOf(NEAR_UID, FAR_UID), navigation.userIds)
        assertEquals(1, navigation.startIndex)
    }

    @Test
    fun `favorite success updates row emits success and releases action`() = runTest {
        val harness = loadedHarness(user(NEAR_UID, "Cerca", 25))
        coEvery { harness.profile.toggleFavoriteUser(NEAR_UID) } returns
            ZibeResult.Success(true)
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.onFavoriteClick(NEAR_UID)

        val snack = event.await() as UsersUiEvent.ShowSnack
        assertEquals(ZibeSnackType.SUCCESS, snack.snackType)
        val state = awaitState(harness.vm) { it.favoriteActionUserId == null }
        assertTrue(state.users.single().isFavorite)
        coVerify(exactly = 1) { harness.profile.toggleFavoriteUser(NEAR_UID) }
    }

    @Test
    fun `favorite failure preserves row emits error and releases action`() = runTest {
        val harness = loadedHarness(user(NEAR_UID, "Cerca", 25))
        coEvery { harness.profile.toggleFavoriteUser(NEAR_UID) } returns
            ZibeResult.Failure(IllegalStateException("falló favorito"))
        val event = async { awaitEvent(harness.vm) }
        runCurrent()

        harness.vm.onFavoriteClick(NEAR_UID)

        val snack = event.await() as UsersUiEvent.ShowSnack
        assertEquals(ZibeSnackType.ERROR, snack.snackType)
        val state = awaitState(harness.vm) { it.favoriteActionUserId == null }
        assertFalse(state.users.single().isFavorite)
    }

    private suspend fun loadedHarness(vararg users: Users): Harness {
        val harness = harness(users = users.toList())
        harness.vm.loadUsers()
        awaitState(harness.vm) {
            !it.isLoading &&
                it.users.size == users.size &&
                it.users.all(UsersRowUiModel::isNotificationsSilenced)
        }
        return harness
    }

    private fun harness(users: List<Users> = emptyList()): Harness {
        val scenario = TestScenario()
        val directory = mockk<UserDirectoryProvider>()
        val location = mockk<LocationRepositoryProvider>()
        val local = mockk<LocalRepositoryProvider>()
        val chat = mockk<ChatRepositoryContract>()
        val profile = mockk<ProfileRepositoryActions>()

        coEvery { directory.getAllAccounts() } returns users
        coEvery { directory.getFavoriteUserIds(MY_UID) } returns emptySet()
        coEvery { directory.getConversationStates(MY_UID) } returns users
            .filter { it.id.isNotBlank() && it.id != MY_UID }
            .associate { it.id to CHAT_STATE_SILENT }
        every { local.myUid } returns MY_UID
        every { location.latitude } returns 0.0
        every { location.longitude } returns 0.0
        every { location.getDistanceMeters(any(), any(), any(), any()) } answers {
            arg<Double>(2)
        }
        every { location.formatDistance(any()) } answers { "${firstArg<Double>()} m" }

        val vm = UsersViewModel(
            userPreferencesProvider = FakeUserPreferencesProvider { scenario },
            userPreferencesActions = FakeUserPreferencesActions { scenario },
            locationRepository = location,
            localRepositoryProvider = local,
            userDirectoryProvider = directory,
            chatRepository = chat,
            profileRepositoryActions = profile,
            profileRepositoryProvider = mockk<ProfileRepositoryProvider>(relaxed = true)
        )
        return Harness(vm, scenario, directory, chat, profile)
    }

    private suspend fun awaitState(
        vm: UsersViewModel,
        predicate: (UsersUiState) -> Boolean
    ): UsersUiState = withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(3_000) { vm.uiState.first(predicate) }
    }

    private suspend fun awaitEvent(vm: UsersViewModel): UsersUiEvent =
        withTimeout(3_000) { vm.events.first() }

    private fun user(
        id: String,
        name: String,
        age: Long,
        online: Boolean = false,
        distance: Double = if (id == NEAR_UID) 100.0 else 900.0
    ) = Users(
        id = id,
        name = name,
        birthDate = LocalDate.now().minusYears(age).toString(),
        online = online,
        photoUrl = "https://example.test/$id.jpg",
        description = "Descripción de $name",
        latitude = distance,
        longitude = 0.0
    )

    private data class Harness(
        val vm: UsersViewModel,
        val scenario: TestScenario,
        val directory: UserDirectoryProvider,
        val chat: ChatRepositoryContract,
        val profile: ProfileRepositoryActions
    )

    private companion object {
        const val MY_UID = "my-uid"
        const val NEAR_UID = "near-uid"
        const val FAR_UID = "far-uid"
    }
}
