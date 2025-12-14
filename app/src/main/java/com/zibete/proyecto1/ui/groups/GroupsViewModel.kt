package com.zibete.proyecto1.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_GROUP
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.utils.Utils.now
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
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val user get() = userRepository.user

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
                val groupsList = groupRepository.getGroups()
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

    private fun joinGroupAndNavigate(
        groupName: String,
        nick: String,
        type: Int
    ) {
        userPreferencesRepository.userNameGroup = nick
        userPreferencesRepository.groupName = groupName
        userPreferencesRepository.inGroup = true
        userPreferencesRepository.userType = type
        userPreferencesRepository.userDate = now()

        viewModelScope.launch {
            groupRepository.sendJoinMessage(groupName, nick, type)
            groupRepository.saveUserInGroup(groupName, nick, type)
            _events.emit(GroupsUiEvent.NavigateToGroupPager)
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
                groupData = groupData,
                groupType = PUBLIC_GROUP
            )

            joinGroupAndNavigate(
                groupName = groupName,
                nick = user.displayName ?: "",
                type = PUBLIC_USER
            )
        }
    }

    fun myDisplayName(): String = user.displayName.orEmpty()
    fun myPhotoUrl(): String? = user.photoUrl?.toString()
}
