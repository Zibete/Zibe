package com.zibete.proyecto1.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_BLOCKED
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.core.utils.onFailure
import com.zibete.proyecto1.core.utils.onFinally
import com.zibete.proyecto1.core.utils.onSuccess
import com.zibete.proyecto1.core.utils.runCatchingPreservingCancellation
import com.zibete.proyecto1.data.ChatRepositoryContract
import com.zibete.proyecto1.data.LocalRepositoryProvider
import com.zibete.proyecto1.data.LocationRepositoryProvider
import com.zibete.proyecto1.data.UserDirectoryProvider
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userPreferencesProvider: UserPreferencesProvider,
    private val userPreferencesActions: UserPreferencesActions,
    private val locationRepository: LocationRepositoryProvider,
    private val localRepositoryProvider: LocalRepositoryProvider,
    private val userDirectoryProvider: UserDirectoryProvider,
    private val chatRepository: ChatRepositoryContract,
    private val profileRepositoryProvider: ProfileRepositoryProvider
) : ViewModel() {
    private data class UsersFilters(
        val applyAgeFilter: Boolean = false,
        val applyOnlineFilter: Boolean = false,
        val minAge: Int = MIN_AGE,
        val maxAge: Int = MAX_AGE
    )

    private val _uiState = MutableStateFlow(UsersUiState())
    val uiState: StateFlow<UsersUiState> = _uiState.asStateFlow()

    private val _events = Channel<UsersUiEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var allUsers: List<UsersRowUiModel> = emptyList()
    private var currentFilters = UsersFilters()
    private var searchQuery = ""
    private var loadJob: Job? = null
    private var metaJob: Job? = null
    private val statusJobs = mutableMapOf<String, Job>()
    private val hasBlockedMeCache = mutableMapOf<String, Boolean>()
    private val hasBlockedMeInFlight = mutableSetOf<String>()
    private val hasBlockedMeSemaphore = Semaphore(4)
    private var usersGeneration = 0

    fun loadUsers() {
        loadJob?.cancel()
        metaJob?.cancel()
        usersGeneration += 1
        hasBlockedMeCache.clear()
        hasBlockedMeInFlight.clear()
        loadJob = viewModelScope.launch {
            val hasContent = allUsers.isNotEmpty()
            _uiState.update {
                it.copy(
                    isLoading = !hasContent,
                    isRefreshing = hasContent,
                    error = null
                )
            }

            currentFilters = readFiltersFromPrefs()
            syncFilterState()
            val myUid = localRepositoryProvider.myUid

            runCatchingPreservingCancellation { fetchUsersBase(myUid) }
                .onSuccess { users ->
                    allUsers = users
                    updateVisibleUsers(isLoading = false, isRefreshing = false, error = null)
                    if (users.isNotEmpty()) {
                        metaJob = viewModelScope.launch {
                            runCatchingPreservingCancellation { enrichUsersMeta(users, myUid) }
                                .onFailure { emitSnack(it.toUsersError(), ZibeSnackType.ERROR) }
                        }
                    }
                }
                .onFailure { error ->
                    val uiText = error.toUsersError()
                    updateVisibleUsers(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (hasContent) null else uiText
                    )
                    emitSnack(uiText, ZibeSnackType.ERROR)
                }
        }
    }

    private suspend fun fetchUsersBase(myUid: String): List<UsersRowUiModel> {
        val accounts = userDirectoryProvider.getAllAccounts()
        val latitude = locationRepository.latitude
        val longitude = locationRepository.longitude
        return withContext(Dispatchers.Default) {
            accounts
                .asSequence()
                .filter { it.id.isNotBlank() && it.id != myUid }
                .map { it.toRow(latitude, longitude) }
                .sortedBy { it.distanceMeters }
                .toList()
        }
    }

    private fun Users.toRow(latitude: Double, longitude: Double) = UsersRowUiModel(
        id = id,
        name = name,
        age = ageCalculator(birthDate),
        isOnline = online,
        distanceMeters = locationRepository.getDistanceMeters(
            latitude,
            longitude,
            this.latitude,
            this.longitude
        ),
        photoUrl = photoUrl,
        description = description
    )

    private suspend fun enrichUsersMeta(baseUsers: List<UsersRowUiModel>, myUid: String) {
        val (favorites, chatStates) = withContext(Dispatchers.IO) {
            coroutineScope {
                val favoritesDeferred = async { userDirectoryProvider.getFavoriteUserIds(myUid) }
                val statesDeferred = async { userDirectoryProvider.getConversationStates(myUid) }
                favoritesDeferred.await() to statesDeferred.await()
            }
        }
        val enrichedById = baseUsers.associate { user ->
            val chatState = chatStates[user.id].orEmpty()
            user.id to user.copy(
                isFavorite = favorites.contains(user.id),
                isBlockedByMe = chatState == CHAT_STATE_BLOCKED,
                hasBlockedMe = hasBlockedMeCache[user.id] ?: false,
                isNotificationsSilenced = chatState == CHAT_STATE_SILENT
            )
        }
        allUsers = allUsers.map { current -> enrichedById[current.id] ?: current }
        updateVisibleUsers()
    }

    fun onSearchQueryChanged(query: String?) {
        searchQuery = query.orEmpty()
        updateVisibleUsers()
    }

    fun onOnlineFilterChanged(enabled: Boolean) {
        applyFilters(
            applyAgeFilter = currentFilters.applyAgeFilter,
            applyOnlineFilter = enabled,
            minAge = currentFilters.minAge,
            maxAge = currentFilters.maxAge
        )
    }

    fun applyFilters(
        applyAgeFilter: Boolean,
        applyOnlineFilter: Boolean,
        minAge: Int,
        maxAge: Int
    ) {
        val safeMin = minAge.coerceIn(MIN_AGE, MAX_AGE)
        val safeMax = maxAge.coerceIn(safeMin, MAX_AGE)
        currentFilters = UsersFilters(
            applyAgeFilter = applyAgeFilter,
            applyOnlineFilter = applyOnlineFilter,
            minAge = safeMin,
            maxAge = safeMax
        )
        syncFilterState()
        updateVisibleUsers()

        viewModelScope.launch {
            runCatchingPreservingCancellation {
                userPreferencesActions.setApplyAgeFilter(applyAgeFilter)
                userPreferencesActions.setApplyOnlineFilter(applyOnlineFilter)
                userPreferencesActions.setMinAge(if (applyAgeFilter) safeMin else 0)
                userPreferencesActions.setMaxAge(if (applyAgeFilter) safeMax else 0)
                userPreferencesActions.setFilterSwitch(applyAgeFilter || applyOnlineFilter)
            }.onFailure { emitSnack(it.toUsersError(), ZibeSnackType.ERROR) }
        }
    }

    fun clearFilters() {
        applyFilters(
            applyAgeFilter = false,
            applyOnlineFilter = false,
            minAge = MIN_AGE,
            maxAge = MAX_AGE
        )
    }

    fun clearAllCriteria() {
        searchQuery = ""
        clearFilters()
    }

    fun onFilterRequested() {
        _uiState.update { it.copy(isFilterSheetOpen = true) }
    }

    fun onFilterDismissed() {
        _uiState.update { it.copy(isFilterSheetOpen = false) }
    }

    fun onUserChatClick(userId: String) {
        if (_uiState.value.chatCheckUserId != null) return
        val user = allUsers.firstOrNull { it.id == userId } ?: return
        _uiState.update { it.copy(chatCheckUserId = userId) }
        viewModelScope.launch {
            chatRepository.hasConversation(userId, NODE_DM)
                .onSuccess { hasConversation ->
                    if (hasConversation == true) emit(UsersUiEvent.NavigateToChat(userId))
                    else _uiState.update { it.copy(pendingFirstContact = user) }
                }
                .onFailure { error ->
                    emitSnack(
                        error.message.toUiText(
                            R.string.discover_chat_check_error,
                            R.string.discover_chat_check_error
                        ),
                        ZibeSnackType.ERROR
                    )
                }
                .onFinally { _uiState.update { it.copy(chatCheckUserId = null) } }
        }
    }

    fun confirmFirstContact() {
        val userId = _uiState.value.pendingFirstContact?.id ?: return
        _uiState.update { it.copy(pendingFirstContact = null) }
        emit(UsersUiEvent.NavigateToChat(userId))
    }

    fun cancelFirstContact() {
        _uiState.update { it.copy(pendingFirstContact = null) }
    }

    fun onUserProfileClick(userId: String) {
        val visibleUsers = _uiState.value.users
        val position = visibleUsers.indexOfFirst { it.id == userId }.coerceAtLeast(0)
        emit(
            UsersUiEvent.NavigateToProfile(
                userIds = ArrayList(visibleUsers.map { it.id }),
                startIndex = position
            )
        )
    }

    fun formatDistance(meters: Double): String = locationRepository.formatDistance(meters)

    fun onVisibleUsersChanged(userIds: List<String>) {
        val visibleIds = userIds.filter(String::isNotBlank).toSet()
        statusJobs.keys.filterNot(visibleIds::contains).forEach { userId ->
            statusJobs.remove(userId)?.cancel()
        }
        visibleIds.filterNot(statusJobs::containsKey).forEach { userId ->
            statusJobs[userId] = viewModelScope.launch {
                profileRepositoryProvider.observeUserStatus(userId, NODE_DM)
                    .collectLatest { status -> applyUserStatus(userId, status) }
            }
        }
        prefetchHasBlockedMe(visibleIds.toList())
    }

    private fun prefetchHasBlockedMe(userIds: List<String>) {
        val generation = usersGeneration
        val toFetch = userIds.filter {
            it !in hasBlockedMeCache && it !in hasBlockedMeInFlight
        }
        if (toFetch.isEmpty()) return
        hasBlockedMeInFlight += toFetch
        viewModelScope.launch {
            runCatchingPreservingCancellation {
                coroutineScope {
                    toFetch.associateWith { userId ->
                        async(Dispatchers.IO) {
                            hasBlockedMeSemaphore.withPermit {
                                userDirectoryProvider.hasBlockedUser(userId, localRepositoryProvider.myUid)
                            }
                        }
                    }.mapValues { it.value.await() }
                }
            }.onSuccess { results ->
                if (generation != usersGeneration) return@onSuccess
                hasBlockedMeCache += results
                allUsers = allUsers.map { user ->
                    results[user.id]?.let { user.copy(hasBlockedMe = it) } ?: user
                }
                updateVisibleUsers()
            }.onFailure { emitSnack(it.toUsersError(), ZibeSnackType.ERROR) }
            hasBlockedMeInFlight -= toFetch.toSet()
        }
    }

    private fun applyUserStatus(userId: String, status: UserStatus) {
        val isOnline = status is UserStatus.Online || status is UserStatus.TypingOrRecording
        val index = allUsers.indexOfFirst { it.id == userId }
        if (index == -1 || allUsers[index].isOnline == isOnline) return
        allUsers = allUsers.toMutableList().apply {
            this[index] = this[index].copy(isOnline = isOnline)
        }
        updateVisibleUsers()
    }

    private suspend fun readFiltersFromPrefs(): UsersFilters {
        val applyAgeFilter = userPreferencesProvider.applyAgeFilterFlow.first()
        val storedMin = userPreferencesProvider.minAgeFlow.first()
        val storedMax = userPreferencesProvider.maxAgeFlow.first()
        return UsersFilters(
            applyAgeFilter = applyAgeFilter,
            applyOnlineFilter = userPreferencesProvider.applyOnlineFilterFlow.first(),
            minAge = storedMin.takeIf { it in MIN_AGE..MAX_AGE } ?: MIN_AGE,
            maxAge = storedMax.takeIf { it in MIN_AGE..MAX_AGE } ?: MAX_AGE
        )
    }

    private fun syncFilterState() {
        _uiState.update {
            it.copy(
                applyAgeFilter = currentFilters.applyAgeFilter,
                applyOnlineFilter = currentFilters.applyOnlineFilter,
                minAge = currentFilters.minAge,
                maxAge = currentFilters.maxAge
            )
        }
    }

    private fun updateVisibleUsers(
        isLoading: Boolean? = null,
        isRefreshing: Boolean? = null,
        error: UiText? = _uiState.value.error
    ) {
        val query = searchQuery.trim().lowercase()
        val filters = currentFilters
        val filtered = allUsers.filter { user ->
            (!filters.applyOnlineFilter || user.isOnline) &&
                (!filters.applyAgeFilter || user.age in filters.minAge..filters.maxAge) &&
                (query.isBlank() || user.name.lowercase().contains(query))
        }

        _uiState.update { state ->
            state.copy(
                isLoading = isLoading ?: state.isLoading,
                isRefreshing = isRefreshing ?: state.isRefreshing,
                users = filtered,
                searchQuery = searchQuery,
                error = error
            )
        }
    }

    private fun Throwable.toUsersError(): UiText = message.toUiText(
        R.string.err_zibe_prefix,
        R.string.err_zibe
    )

    private fun emitSnack(uiText: UiText, snackType: ZibeSnackType) {
        emit(UsersUiEvent.ShowSnack(uiText, snackType))
    }

    private fun emit(event: UsersUiEvent) {
        _events.trySend(event)
    }

    private companion object {
        const val MIN_AGE = 18
        const val MAX_AGE = 99
    }
}
