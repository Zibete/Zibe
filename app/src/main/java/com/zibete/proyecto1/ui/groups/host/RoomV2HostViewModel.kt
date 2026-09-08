package com.zibete.proyecto1.ui.groups.host

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.utils.ZibeResult
import com.zibete.proyecto1.domain.roomsv2.RoomV2ErrorCode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Exception
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2Message
import com.zibete.proyecto1.domain.roomsv2.RoomV2Role
import com.zibete.proyecto1.domain.roomsv2.RoomV2Thread
import com.zibete.proyecto1.domain.roomsv2.RoomsV2ChatRepository
import com.zibete.proyecto1.domain.roomsv2.RoomsV2ModerationRepository
import com.zibete.proyecto1.domain.roomsv2.RoomsV2Repository
import com.zibete.proyecto1.domain.roomsv2.SendRoomV2TextUseCase
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RoomV2HostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomsRepository: RoomsV2Repository,
    private val chatRepository: RoomsV2ChatRepository,
    private val moderationRepository: RoomsV2ModerationRepository,
    private val sendText: SendRoomV2TextUseCase,
) : ViewModel() {

    private val roomId = savedStateHandle.get<String>(ROOM_V2_ID_ARG).orEmpty().trim()
    private val selectedConversationId = MutableStateFlow<String?>(null)
    private var screenVisible = false

    private val _uiState = MutableStateFlow(RoomV2HostUiState(roomId = roomId))
    val uiState: StateFlow<RoomV2HostUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RoomV2HostEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<RoomV2HostEvent> = _events.asSharedFlow()

    init {
        if (roomId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = UiText.StringRes(R.string.rooms_v2_error_not_found),
                )
            }
            emit(RoomV2HostEvent.NavigateBack)
        } else {
            observeRoom()
            observeMembership()
            observeParticipants()
            observeConversations()
            observeMessages()
            observeReports()
        }
    }

    private fun observeRoom() {
        viewModelScope.launch {
            roomsRepository.observeRoom(roomId)
                .catch { failure -> handleStreamFailure(failure) }
                .collect { room ->
                    _uiState.update {
                        it.copy(
                            room = room,
                            isLoading = false,
                            error = if (room == null) {
                                UiText.StringRes(R.string.rooms_v2_error_not_found)
                            } else {
                                null
                            },
                        )
                    }
                }
        }
    }

    private fun observeMembership() {
        viewModelScope.launch {
            roomsRepository.observeMemberships()
                .map { memberships -> memberships.firstOrNull { it.roomId == roomId } }
                .distinctUntilChanged()
                .catch { failure -> handleStreamFailure(failure) }
                .collect { membership ->
                    _uiState.update { it.copy(membership = membership) }
                }
        }
    }

    private fun observeParticipants() {
        viewModelScope.launch {
            chatRepository.observeParticipants(roomId)
                .catch { failure -> handleStreamFailure(failure) }
                .collect { participants ->
                    _uiState.update { it.copy(participants = participants) }
                }
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepository.observeConversations(roomId)
                .catch { failure -> handleStreamFailure(failure) }
                .collect { conversations ->
                    _uiState.update { it.copy(conversations = conversations) }
                    val selected = selectedConversationId.value
                    if (selected != null && conversations.none { it.conversationId == selected }) {
                        switchContext(RoomV2HostTab.PRIVATES, null)
                    }
                }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            selectedConversationId
                .flatMapLatest { conversationId ->
                    chatRepository.observeMessages(
                        RoomV2Thread(roomId = roomId, conversationId = conversationId),
                        limit = 100,
                    )
                }
                .catch { failure -> handleStreamFailure(failure) }
                .collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                    val latestSeq = messages.lastOrNull()?.seq ?: 0L
                    if (latestSeq > 0L && screenVisible && isThreadVisible(_uiState.value)) {
                        markRead(_uiState.value.currentThread, latestSeq)
                    }
                }
        }
    }

    private fun observeReports() {
        viewModelScope.launch {
            roomsRepository.observeMemberships()
                .map { memberships -> memberships.firstOrNull { it.roomId == roomId } }
                .distinctUntilChanged()
                .flatMapLatest { membership ->
                    when (membership?.identity?.role) {
                        RoomV2Role.OWNER,
                        RoomV2Role.MODERATOR -> moderationRepository.observeReports(roomId)
                        RoomV2Role.MEMBER,
                        null -> flowOf(emptyList())
                    }
                }
                .catch { failure -> handleStreamFailure(failure) }
                .collect { reports ->
                    _uiState.update { it.copy(reports = reports) }
                }
        }
    }

    fun onScreenVisible(visible: Boolean) {
        if (screenVisible == visible) return
        screenVisible = visible
        val state = _uiState.value
        if (isThreadVisible(state)) {
            setVisibleThread(state.currentThread, visible)
            if (visible) {
                val latestSeq = state.messages.lastOrNull()?.seq ?: 0L
                if (latestSeq > 0L) markRead(state.currentThread, latestSeq)
            }
        }
    }

    fun onTabSelected(tab: RoomV2HostTab) {
        if (tab == _uiState.value.selectedTab) return
        val conversationId = if (tab == RoomV2HostTab.PRIVATES) {
            _uiState.value.selectedConversationId
        } else {
            null
        }
        switchContext(tab, conversationId)
    }

    fun onDraftChanged(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun sendCurrentText() {
        val state = _uiState.value
        if (state.isSubmitting || !isThreadVisible(state)) return
        val text = state.draft.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (
                val result = sendText(
                    state.currentThread,
                    text,
                    UUID.randomUUID().toString(),
                )
            ) {
                is ZibeResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, draft = "") }
                }

                is ZibeResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showFailure(result.exception)
                }
            }
        }
    }

    fun openPrivate(target: RoomV2Identity) {
        val state = _uiState.value
        if (target.identityId == state.myIdentityId || state.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = chatRepository.openPrivate(roomId, target.identityId)) {
                is ZibeResult.Success -> {
                    val conversationId = result.data
                    _uiState.update { it.copy(isSubmitting = false) }
                    if (conversationId.isNullOrBlank()) {
                        showFailure(RoomV2Exception(RoomV2ErrorCode.INTERNAL))
                    } else {
                        switchContext(RoomV2HostTab.PRIVATES, conversationId)
                    }
                }

                is ZibeResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showFailure(result.exception)
                }
            }
        }
    }

    fun selectConversation(conversationId: String) {
        if (conversationId.isBlank()) return
        switchContext(RoomV2HostTab.PRIVATES, conversationId)
    }

    fun backToPrivateList() {
        if (_uiState.value.selectedConversationId == null) return
        switchContext(RoomV2HostTab.PRIVATES, null)
    }

    fun onBackRequested() {
        if (!tryHandleBack()) {
            emit(RoomV2HostEvent.NavigateBack)
        }
    }

    fun tryHandleBack(): Boolean {
        val state = _uiState.value
        if (
            state.selectedTab == RoomV2HostTab.PRIVATES &&
            state.selectedConversationId != null
        ) {
            backToPrivateList()
            return true
        }
        if (state.selectedTab != RoomV2HostTab.CHAT) {
            switchContext(RoomV2HostTab.CHAT, null)
            return true
        }
        return false
    }

    fun onLeaveRequested() {
        _uiState.update { it.copy(showLeaveConfirm = true) }
    }

    fun dismissLeave() {
        _uiState.update { it.copy(showLeaveConfirm = false) }
    }

    fun confirmLeave() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, showLeaveConfirm = false) }
            when (val result = roomsRepository.leaveRoom(roomId)) {
                is ZibeResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    emit(RoomV2HostEvent.NavigateBack)
                }

                is ZibeResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showFailure(result.exception)
                }
            }
        }
    }

    fun setNotifications(enabled: Boolean) {
        launchUnit { roomsRepository.setNotifications(roomId, enabled) }
    }

    fun blockCurrentPrivate(blocked: Boolean) {
        val thread = _uiState.value.currentThread
        if (thread.conversationId == null) return
        launchUnit { chatRepository.blockPrivate(thread, blocked) }
    }

    fun closeRoom() {
        launchUnit(success = UiText.StringRes(R.string.rooms_v2_closed)) {
            moderationRepository.closeRoom(roomId)
        }
    }

    fun transferOwnership(targetIdentityId: String) {
        launchUnit { moderationRepository.transferOwnership(roomId, targetIdentityId) }
    }

    fun setModerator(targetIdentityId: String, enabled: Boolean) {
        launchUnit { moderationRepository.setModerator(roomId, targetIdentityId, enabled) }
    }

    fun removeMember(targetIdentityId: String, ban: Boolean) {
        launchUnit { moderationRepository.removeMember(roomId, targetIdentityId, ban) }
    }

    fun removeMessage(messageId: String) {
        if (_uiState.value.selectedConversationId != null) return
        launchUnit { moderationRepository.removeMessage(roomId, messageId) }
    }

    fun reportMessage(message: RoomV2Message, reason: String) {
        val cleanReason = reason.trim()
        if (cleanReason.length < 3) {
            showFailure(RoomV2Exception(RoomV2ErrorCode.INVALID_INPUT))
            return
        }
        launchUnit(success = UiText.StringRes(R.string.rooms_v2_report_sent)) {
            moderationRepository.reportMessage(
                RoomV2Thread(roomId, message.conversationId ?: _uiState.value.selectedConversationId),
                message.messageId,
                cleanReason,
            )
        }
    }

    fun resolveReport(reportId: String, resolution: String) {
        if (resolution.isBlank()) {
            showFailure(RoomV2Exception(RoomV2ErrorCode.INVALID_INPUT))
            return
        }
        launchUnit { moderationRepository.resolveReport(roomId, reportId, resolution.trim()) }
    }

    private fun launchUnit(
        success: UiText? = null,
        block: suspend () -> ZibeResult<Unit>,
    ) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            when (val result = block()) {
                is ZibeResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    if (success != null) emit(RoomV2HostEvent.ShowSnack(success, ZibeSnackType.SUCCESS))
                }

                is ZibeResult.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showFailure(result.exception)
                }
            }
        }
    }

    private fun switchContext(tab: RoomV2HostTab, conversationId: String?) {
        val before = _uiState.value
        val beforeThread = before.currentThread
        val beforeVisible = screenVisible && isThreadVisible(before)

        selectedConversationId.value = conversationId
        _uiState.update {
            it.copy(
                selectedTab = tab,
                selectedConversationId = conversationId,
                messages = emptyList(),
                draft = "",
            )
        }

        val after = _uiState.value
        val afterVisible = screenVisible && isThreadVisible(after)
        if (beforeVisible) setVisibleThread(beforeThread, false)
        if (afterVisible) setVisibleThread(after.currentThread, true)
    }

    private fun isThreadVisible(state: RoomV2HostUiState): Boolean = when (state.selectedTab) {
        RoomV2HostTab.CHAT -> true
        RoomV2HostTab.PRIVATES -> state.selectedConversationId != null
        RoomV2HostTab.PEOPLE,
        RoomV2HostTab.REPORTS -> false
    }

    private fun setVisibleThread(thread: RoomV2Thread, visible: Boolean) {
        viewModelScope.launch {
            when (val result = chatRepository.setVisibleThread(thread, visible)) {
                is ZibeResult.Success -> Unit
                is ZibeResult.Failure -> if (visible) showFailure(result.exception)
            }
        }
    }

    private fun markRead(thread: RoomV2Thread, seq: Long) {
        viewModelScope.launch {
            when (val result = chatRepository.markRead(thread, seq)) {
                is ZibeResult.Success -> Unit
                is ZibeResult.Failure -> showFailure(result.exception)
            }
        }
    }

    private fun handleStreamFailure(failure: Throwable) {
        if (failure is CancellationException) throw failure
        _uiState.update {
            it.copy(
                isLoading = false,
                error = if (it.room == null) failure.toRoomText() else it.error,
            )
        }
        if (_uiState.value.room != null) showFailure(failure)
    }

    private fun showFailure(failure: Throwable) {
        emit(RoomV2HostEvent.ShowSnack(failure.toRoomText(), ZibeSnackType.ERROR))
    }

    private fun Throwable.toRoomText(): UiText {
        val resource = when ((this as? RoomV2Exception)?.code) {
            RoomV2ErrorCode.UNAUTHENTICATED,
            RoomV2ErrorCode.PERMISSION_DENIED -> R.string.rooms_v2_error_permission
            RoomV2ErrorCode.ALIAS_TAKEN -> R.string.rooms_v2_error_alias_taken
            RoomV2ErrorCode.ROOM_NAME_TAKEN -> R.string.rooms_v2_error_room_name_taken
            RoomV2ErrorCode.ROOM_CLOSED -> R.string.rooms_v2_error_closed
            RoomV2ErrorCode.IDENTITY_CHANGE_REQUIRED -> R.string.rooms_v2_error_identity_change
            RoomV2ErrorCode.OWNER_ACTION_REQUIRED -> R.string.rooms_v2_error_owner_action
            RoomV2ErrorCode.NOT_FOUND -> R.string.rooms_v2_error_not_found
            RoomV2ErrorCode.INVALID_INPUT -> R.string.rooms_v2_error_invalid_input
            RoomV2ErrorCode.OFFLINE -> R.string.rooms_v2_error_offline
            RoomV2ErrorCode.CONFLICT -> R.string.rooms_v2_error_conflict
            RoomV2ErrorCode.INTERNAL,
            null -> R.string.rooms_v2_error_unexpected
        }
        return UiText.StringRes(resource)
    }

    private fun emit(event: RoomV2HostEvent) {
        viewModelScope.launch { _events.emit(event) }
    }
}
