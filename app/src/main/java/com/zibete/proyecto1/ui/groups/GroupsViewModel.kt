package com.zibete.proyecto1.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_GROUP
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.ui.constants.ERR_ZIBE
import com.zibete.proyecto1.utils.Utils.now
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer
) : ViewModel() {

    private val user = userRepository.user

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState

    private val _events = MutableSharedFlow<GroupsUiEvent>()
    val events = _events

    fun loadGroups() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val groupsList = groupRepository.getGroups()
                val sorted = groupsList.sortedBy { it.name }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groups = sorted,
                        originalGroups = sorted,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groups = emptyList(),
                        originalGroups = emptyList(),
                        error = e.message ?: ERR_ZIBE
                    )
                }
            }
        }
    }

    fun refreshGroups() = loadGroups()

    fun filter(query: String) {
        val baseList = _uiState.value.originalGroups

        if (query.isEmpty()) {
            _uiState.update { it.copy(groups = baseList) }
            return
        }

        val filtered = baseList.filter {
            it.name.lowercase().contains(query.lowercase())
        }

        _uiState.update { it.copy(groups = filtered) }
    }

    fun joinGroupAndNavigate(
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
                _events.emit(GroupsUiEvent.JoinGroup(groupName, nick, type))
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
                groupName,
                groupData,
                PUBLIC_GROUP,)

            joinGroupAndNavigate(
                groupName = groupName,
                nick = user.displayName ?: "",
                type = PUBLIC_USER
            )

            // 4) Navegar al pager
            _events.emit(GroupsUiEvent.NavigateToGroupPager)
        }
    }



}
