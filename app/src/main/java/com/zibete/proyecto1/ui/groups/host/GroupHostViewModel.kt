package com.zibete.proyecto1.ui.groups.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.data.GroupContext
import com.zibete.proyecto1.data.GroupRepositoryProvider
import com.zibete.proyecto1.data.UserPreferencesProvider
import com.zibete.proyecto1.domain.rooms.MarkRoomReadCommand
import com.zibete.proyecto1.domain.rooms.MarkRoomReadResult
import com.zibete.proyecto1.domain.rooms.MarkRoomReadUseCase
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.GroupChatChildEvent
import com.zibete.proyecto1.model.UserGroup
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class GroupHostViewModel @Inject constructor(
    private val groupRepository: GroupRepositoryProvider,
    private val userPreferencesProvider: UserPreferencesProvider,
    private val markRoomRead: MarkRoomReadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupHostUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<GroupHostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var activeLeaseJob: Job? = null
    private var markReadJob: Job? = null
    private var retryObservationJob: Job? = null
    private val activeLeaseMutex = Mutex()
    private var activeLeaseGeneration = 0L
    private var lastMarkedAt = 0L
    private var observationErrorShown = false
    private var leaseErrorShown = false

    init {
        observeActiveSession()
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            userPreferencesProvider.groupContextFlow
                .distinctUntilChangedBy { context ->
                    context?.roomKey?.ifBlank { context.groupName }
                }
                .collectLatest { context ->
                    retryObservationJob?.cancel()
                    if (context == null) {
                        val previousRoomKey = _uiState.value.roomKey
                        _uiState.value = GroupHostUiState()
                        stopActiveLease(previousRoomKey)
                        return@collectLatest
                    }
                    observeRoom(context)
                }
        }
    }

    private suspend fun observeRoom(context: GroupContext) {
        val roomKey = context.roomKey.ifBlank { context.groupName }
        val wasVisible = _uiState.value.isScreenVisible
        _uiState.value = GroupHostUiState(
            groupContext = context,
            currentUid = groupRepository.myUid,
            isScreenVisible = wasVisible
        )
        lastMarkedAt = 0L
        observationErrorShown = false
        leaseErrorShown = false

        if (!loadRoomHeader(context, roomKey)) return
        syncActiveRoomVisibility()

        coroutineScope {
            launch {
                groupRepository.observeGroupUsers(roomKey)
                    .retryRoomObservation()
                    .collect { users ->
                        _uiState.update { state -> state.copy(users = users) }
                    }
            }
            launch {
                groupRepository.observeGroupChatEvents(roomKey)
                    .retryRoomObservation()
                    .collect { event -> onRoomMessageEvent(event) }
            }
            launch {
                groupRepository.observeRoomPrivateConversations(roomKey)
                    .retryRoomObservation()
                    .collect { conversations ->
                        _uiState.update { state ->
                            state.copy(
                                privateConversations = conversations.filterForRoom(roomKey)
                            )
                        }
                    }
            }
            launch {
                groupRepository.observeUnreadGroupChat(roomKey)
                    .retryRoomObservation()
                    .collect { unread ->
                        _uiState.update { state ->
                            state.copy(publicUnread = unread.coerceAtLeast(0))
                        }
                    }
            }
        }
    }

    private suspend fun loadRoomHeader(context: GroupContext, roomKey: String): Boolean {
        val sessionResult = groupRepository.resolveRoomSession(roomKey)
        val session = when (sessionResult) {
            is ZibeResult.Failure -> {
                onInitialLoadFailure(sessionResult.exception)
                return false
            }
            is ZibeResult.Success -> sessionResult.data
        }
        if (session == null) {
            onInitialLoadFailure(IllegalStateException("Room session was empty"))
            return false
        }

        val roomsResult = groupRepository.loadRooms()
        val rooms = when (roomsResult) {
            is ZibeResult.Failure -> {
                onInitialLoadFailure(roomsResult.exception)
                return false
            }
            is ZibeResult.Success -> roomsResult.data.orEmpty()
        }
        val room = rooms.firstOrNull { it.resolvedRoomKey() == roomKey }
        if (room == null) {
            _uiState.update { it.copy(isLoading = false, loadError = true) }
            showSnack(
                UiText.StringRes(R.string.group_not_exists, listOf(context.groupName)),
                ZibeSnackType.WARNING
            )
            return false
        }

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                loadError = false,
                groupContext = context.copy(
                    roomKey = session.roomKey,
                    displayName = session.displayName,
                    userName = session.userName,
                    userType = session.userType
                ),
                roomDescription = room.description,
                creatorUid = room.creatorUid
            )
        }
        return true
    }

    private suspend fun onInitialLoadFailure(throwable: Throwable) {
        throwable.rethrowCancellation()
        _uiState.update { it.copy(isLoading = false, loadError = true) }
        showSnack(UiText.StringRes(R.string.rooms_error_message), ZibeSnackType.ERROR)
    }

    private suspend fun onObservationFailure(throwable: Throwable) {
        throwable.rethrowCancellation()
        if (observationErrorShown) return
        observationErrorShown = true
        showSnack(UiText.StringRes(R.string.rooms_error_message), ZibeSnackType.ERROR)
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.retryRoomObservation() =
        retryWhen { cause, attempt ->
            onObservationFailure(cause)
            delay(
                (OBSERVATION_RETRY_BASE_MS * (attempt.coerceAtMost(5L) + 1L))
                    .coerceAtMost(OBSERVATION_RETRY_MAX_MS)
            )
            true
        }

    fun retryLoad() {
        val context = _uiState.value.groupContext ?: return
        if (_uiState.value.isLoading) return
        retryObservationJob?.cancel()
        retryObservationJob = viewModelScope.launch { observeRoom(context) }
    }

    private fun onRoomMessageEvent(event: GroupChatChildEvent) {
        _uiState.update { state -> state.reduce(event) }
        markLatestMessageReadIfVisible()
    }

    fun onTabSelected(tab: GroupHostTab) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab, selectedMember = null) }
        syncActiveRoomVisibility()
    }

    fun onScreenStarted() {
        if (_uiState.value.isScreenVisible) return
        _uiState.update { it.copy(isScreenVisible = true) }
        syncActiveRoomVisibility()
    }

    fun onScreenStopped() {
        if (!_uiState.value.isScreenVisible) return
        _uiState.update { it.copy(isScreenVisible = false, selectedMember = null) }
        syncActiveRoomVisibility()
    }

    fun tryHandleBack(): Boolean =
        if (_uiState.value.selectedTab != GroupHostTab.GROUP_CHAT) {
            onTabSelected(GroupHostTab.GROUP_CHAT)
            true
        } else {
            false
        }

    private fun syncActiveRoomVisibility() {
        val generation = ++activeLeaseGeneration
        activeLeaseJob?.cancel()
        activeLeaseJob = null
        viewModelScope.launch {
            activeLeaseMutex.withLock {
                if (generation != activeLeaseGeneration) return@withLock
                val state = _uiState.value
                val roomKey = state.roomKey
                if (shouldExposeActiveRoom(state)) {
                    refreshActiveRoomLease(roomKey)
                    if (generation == activeLeaseGeneration) {
                        startActiveRoomRenewal(roomKey, generation)
                    }
                } else if (roomKey.isNotBlank()) {
                    groupRepository.clearActiveRoom(roomKey).rethrowCancellationFailure()
                }
            }
        }
    }

    private fun stopActiveLease(roomKey: String) {
        ++activeLeaseGeneration
        activeLeaseJob?.cancel()
        activeLeaseJob = null
        if (roomKey.isBlank()) return
        viewModelScope.launch {
            activeLeaseMutex.withLock {
                val state = _uiState.value
                if (shouldExposeActiveRoom(state) && state.roomKey == roomKey) return@withLock
                groupRepository.clearActiveRoom(roomKey).rethrowCancellationFailure()
            }
        }
    }

    private suspend fun refreshActiveRoomLease(roomKey: String) {
        when (val result = groupRepository.setActiveRoom(roomKey)) {
            is ZibeResult.Success -> leaseErrorShown = false
            is ZibeResult.Failure -> onLeaseFailure(result.exception)
        }
        markLatestMessageReadIfVisible()
    }

    private fun startActiveRoomRenewal(roomKey: String, generation: Long) {
        activeLeaseJob = viewModelScope.launch {
            delay(ACTIVE_ROOM_REFRESH_MS)
            while (generation == activeLeaseGeneration) {
                activeLeaseMutex.withLock {
                    if (
                        generation != activeLeaseGeneration ||
                        !shouldExposeActiveRoom(_uiState.value)
                    ) return@withLock
                    refreshActiveRoomLease(roomKey)
                }
                delay(ACTIVE_ROOM_REFRESH_MS)
            }
        }
    }

    private suspend fun onLeaseFailure(throwable: Throwable) {
        throwable.rethrowCancellation()
        if (leaseErrorShown) return
        leaseErrorShown = true
        showSnack(UiText.StringRes(R.string.rooms_error_message), ZibeSnackType.ERROR)
    }

    private fun markLatestMessageReadIfVisible() {
        val state = _uiState.value
        val latestMessageAt = state.latestMessageAt
        if (!shouldExposeActiveRoom(state) || latestMessageAt <= lastMarkedAt) return

        markReadJob?.cancel()
        markReadJob = viewModelScope.launch {
            when (
                val result = markRoomRead.execute(
                    MarkRoomReadCommand(
                        roomKey = state.roomKey,
                        lastReadAt = latestMessageAt,
                        isChatVisible = true
                    )
                )
            ) {
                is ZibeResult.Success -> when (result.data) {
                    is MarkRoomReadResult.Marked -> {
                        lastMarkedAt = maxOf(lastMarkedAt, latestMessageAt)
                    }
                    MarkRoomReadResult.SkippedNotVisible,
                    is MarkRoomReadResult.ValidationFailed,
                    null -> Unit
                }
                is ZibeResult.Failure -> onLeaseFailure(result.exception)
            }
        }
    }

    fun onComposerChanged(value: String) {
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(composerText = value, failedSend = null) }
    }

    fun sendCurrentText() {
        val content = _uiState.value.composerText.trim()
        if (content.isBlank()) return
        send(RoomSendDraft.Text(content))
    }

    fun sendPhoto(uri: String) {
        if (uri.isBlank()) return
        send(RoomSendDraft.Photo(uri))
    }

    fun retryFailedSend() {
        _uiState.value.failedSend?.let(::send)
    }

    fun dismissFailedSend() {
        _uiState.update { it.copy(failedSend = null) }
    }

    private fun send(draft: RoomSendDraft) {
        val state = _uiState.value
        val context = state.groupContext ?: return
        if (state.isSending || state.roomKey.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, failedSend = null) }
            val result = when (draft) {
                is RoomSendDraft.Text -> groupRepository.sendRoomMessage(
                    roomKey = state.roomKey,
                    userName = context.userName,
                    userType = context.userType,
                    chatType = MSG_TEXT,
                    content = draft.content
                )
                is RoomSendDraft.Photo -> groupRepository.sendRoomPhotoMessage(
                    roomKey = state.roomKey,
                    userName = context.userName,
                    userType = context.userType,
                    photoUri = draft.uri
                )
            }
            when (result) {
                is ZibeResult.Success -> _uiState.update {
                    it.copy(
                        isSending = false,
                        composerText = if (draft is RoomSendDraft.Text) "" else it.composerText,
                        failedSend = null
                    )
                }
                is ZibeResult.Failure -> {
                    result.exception.rethrowCancellation()
                    _uiState.update { it.copy(isSending = false, failedSend = draft) }
                    showSnack(
                        UiText.StringRes(R.string.rooms_message_failed),
                        ZibeSnackType.ERROR
                    )
                }
            }
        }
    }

    fun onMemberSelected(member: UserGroup) {
        if (_uiState.value.isCurrentUser(member) || member.userId.isBlank()) return
        _uiState.update { it.copy(selectedMember = member) }
    }

    fun dismissMemberActions() {
        _uiState.update { it.copy(selectedMember = null) }
    }

    fun openSelectedMemberProfile() {
        val member = _uiState.value.selectedMember ?: return
        if (member.isAnonymous || member.userId.isBlank()) return
        _uiState.update { it.copy(selectedMember = null) }
        viewModelScope.launch { _events.send(GroupHostEvent.OpenProfile(member.userId)) }
    }

    fun openSelectedMemberPrivateChat() {
        val member = _uiState.value.selectedMember ?: return
        openPrivateChat(member.userId)
    }

    fun openPrivateConversation(conversation: Conversation) {
        val otherUid = conversation.otherId.ifBlank {
            conversation.userId.takeUnless { it == _uiState.value.currentUid }.orEmpty()
        }
        openPrivateChat(otherUid)
    }

    private fun openPrivateChat(otherUid: String) {
        if (otherUid.isBlank() || otherUid == _uiState.value.currentUid) return
        _uiState.update { it.copy(selectedMember = null) }
        viewModelScope.launch { _events.send(GroupHostEvent.OpenPrivateChat(otherUid)) }
    }

    private suspend fun showSnack(uiText: UiText, type: ZibeSnackType) {
        _events.send(GroupHostEvent.ShowSnack(uiText, type))
    }

    private fun List<Conversation>.filterForRoom(roomKey: String): List<Conversation> =
        filter { conversation ->
            conversation.roomKey.isBlank() || conversation.roomKey == roomKey
        }.sorted()

    private fun Throwable.rethrowCancellation() {
        if (this is CancellationException) throw this
    }

    private fun ZibeResult<Unit>.rethrowCancellationFailure() {
        if (this is ZibeResult.Failure) exception.rethrowCancellation()
    }

    private companion object {
        const val ACTIVE_ROOM_REFRESH_MS = 60_000L
        const val OBSERVATION_RETRY_BASE_MS = 1_000L
        const val OBSERVATION_RETRY_MAX_MS = 30_000L
    }
}
