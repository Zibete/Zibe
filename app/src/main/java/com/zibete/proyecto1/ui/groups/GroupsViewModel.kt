package com.zibete.proyecto1.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.ui.constants.Constants.MSG_INFO
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_GROUP
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.ui.constants.MSG_USER_JOINED
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState

    private val _events = MutableSharedFlow<GroupsUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<GroupsUiEvent> = _events

    fun loadGroups() = fetchGroups(showLoading = true)

    fun refreshGroups() = fetchGroups(showLoading = false)

    private fun fetchGroups(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val groupsList = groupRepository.getAllGroups()
                val sorted = groupsList.sortedBy { it.name.lowercase() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groups = sorted
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, groups = emptyList()) }
                _events.tryEmit(GroupsUiEvent.ShowMessage(e.message))
            }
        }
    }

    private suspend fun joinGroupAndNavigate(
        groupName: String,
        userName: String,
        userType: Int
    ) {
        userPreferencesRepository.setGroupSession(groupName, userName, userType)

        viewModelScope.launch {
            groupRepository.saveUserInGroup(groupName, userName, userType)
            groupRepository.sendGroupMessage(groupName, userName, userType, MSG_INFO, MSG_USER_JOINED)
            _events.emit(GroupsUiEvent.NavigateToGroupHost)
        }
    }

    fun onJoinGroupRequested(groupName: String, nick: String, type: Int) {
        viewModelScope.launch {
            val inUse = groupRepository.isNickInUse(groupName, nick)
            if (inUse) {
                _events.emit(GroupsUiEvent.NickInUse(nick))
            } else {
                joinGroupAndNavigate(groupName, nick, type)
            }
        }
    }

    fun onCreateNewGroupClicked(groupName: String, groupData: String) {
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
                userType = PUBLIC_USER
            )
        }
    }

    fun myDisplayName(): String = userRepository.myUserName
    fun myPhotoUrl(): String = userRepository.myUserName
}
