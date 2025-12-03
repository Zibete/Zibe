package com.zibete.proyecto1.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.utils.FirebaseRefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState

    // ------------------------------------------------------------------------
    // PUBLIC METHODS
    // ------------------------------------------------------------------------

    fun loadGroups() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            getGroupsFlow().collect { groupsList ->
                val sorted = groupsList.sortedBy { it.name }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groups = sorted,
                        originalGroups = sorted,
                        error = null
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

    // ------------------------------------------------------------------------
    // FIREBASE → FLOW
    // ------------------------------------------------------------------------

    private fun getGroupsFlow() = callbackFlow {

        FirebaseRefs.refGroupData.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (!snapshot.exists()) {
                    trySend(emptyList())
                    return
                }

                val groupsList = ArrayList<Groups>()
                var groupsProcessed = 0
                val totalGroups = snapshot.childrenCount.toInt()

                // Si no hay hijos, enviamos lista vacía
                if (totalGroups == 0) {
                    trySend(emptyList())
                    return
                }

                snapshot.children.forEach { groupSnap ->
                    val group = groupSnap.getValue(Groups::class.java) ?: run {
                        groupsProcessed++
                        if (groupsProcessed == totalGroups) trySend(groupsList)
                        return@forEach
                    }

                    val name = groupSnap.child("name").getValue(String::class.java) ?: ""

                    // Obtener cantidad de usuarios
                    FirebaseRefs.refGroupUsers.child(name)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(usersSnap: DataSnapshot) {

                                group.users = usersSnap.childrenCount.toInt()
                                groupsList.add(group)

                                groupsProcessed++

                                if (groupsProcessed == totalGroups) {
                                    trySend(groupsList)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                groupsProcessed++
                                if (groupsProcessed == totalGroups) {
                                    trySend(groupsList)
                                }
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        awaitClose { /* no-op */ }
    }
}
