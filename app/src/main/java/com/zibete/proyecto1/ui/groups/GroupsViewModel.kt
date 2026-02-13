package com.zibete.proyecto1.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.MSG_INFO
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_GROUP
import com.zibete.proyecto1.core.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.core.ui.UiText
import com.zibete.proyecto1.core.ui.toUiText
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesActions
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.ui.components.ZibeSnackType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val userPreferencesActions: UserPreferencesActions,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GroupsUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<GroupsUiEvent> = _events.asSharedFlow()

    private var allGroups: List<Groups> = emptyList()
    private var searchQuery: String = ""

    fun loadGroups() = fetchGroups(showLoading = true)

    fun refreshGroups() = fetchGroups(showLoading = false)

    private fun fetchGroups(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _uiState.update { it.copy(isLoading = true) }

            runCatching { groupRepository.getAllGroups() }
                .onSuccess { groupsList ->
                    allGroups = groupsList.sortedBy { it.name.lowercase() }
                    updateVisibleGroups(isLoading = false)
                }
                .onFailure { e ->
                    onError(
                        e.message.toUiText(
                            R.string.err_zibe_prefix,
                            R.string.err_zibe
                        )
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        viewModelScope.launch {
            searchQuery = query
            updateVisibleGroups()
        }
    }

    fun onError(uiText: UiText) {
        _uiState.update {
            it.copy(
                isLoading = false,
                groups = emptyList(),
                filteredGroups = emptyList()
            )
        }
        viewModelScope.launch {
            _events.emit(
                GroupsUiEvent.ShowSnack(
                    uiText = uiText,
                    snackType = ZibeSnackType.ERROR
                )
            )
        }
    }

    private fun updateVisibleGroups(isLoading: Boolean? = null) {
        val query = searchQuery.trim().lowercase()

        val filtered =
            if (query.isBlank()) allGroups
            else allGroups.filter { it.name.lowercase().contains(query) }

        _uiState.update { state ->
            state.copy(
                isLoading = isLoading ?: state.isLoading,
                groups = allGroups,
                filteredGroups = filtered,
                searchQuery = searchQuery
            )
        }
    }

    private suspend fun joinGroupAndNavigate(
        groupName: String,
        userName: String,
        userType: Int,
        message: String
    ) {
        userPreferencesActions.setGroupSession(groupName, userName, userType)

        viewModelScope.launch {
            groupRepository.saveUserInGroup(groupName, userName, userType)
            groupRepository.sendGroupMessage(groupName, userName, userType, MSG_INFO, message)
            _events.emit(GroupsUiEvent.NavigateToGroupHost)
        }
    }

    fun onJoinGroupRequested(groupName: String, nick: String, type: Int, message: String) {
        viewModelScope.launch {
            val inUse = groupRepository.isNickInUse(groupName, nick)
            if (inUse) {
                _events.emit(GroupsUiEvent.NickInUse(nick))
            } else {
                joinGroupAndNavigate(
                    groupName = groupName,
                    userName = nick,
                    userType = type,
                    message = message
                )
            }
        }
    }

    fun onCreateNewGroupClicked(groupName: String, groupData: String, message: String) {
        viewModelScope.launch {
            if (groupRepository.isGroupNameInUse(groupName)) {
                _events.emit(GroupsUiEvent.GroupNameInUse(groupName))
                return@launch
            }

            groupRepository.createGroup(
                groupName = groupName,
                groupType = PUBLIC_GROUP,
                groupDescription = groupData
            )

            joinGroupAndNavigate(
                groupName = groupName,
                userName = userRepository.myUserName,
                userType = PUBLIC_USER,
                message = message
            )
        }
    }

    fun myDisplayName(): String = userRepository.myUserName
    fun myPhotoUrl(): String = userRepository.myProfilePhotoUrl
}
