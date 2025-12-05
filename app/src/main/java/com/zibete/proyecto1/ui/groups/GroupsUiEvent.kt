package com.zibete.proyecto1.ui.groups

sealed class GroupsUiEvent {
    data class NickInUse(val nick: String) : GroupsUiEvent()

    data class GroupNameInUse(val name: String) : GroupsUiEvent()

    data class JoinGroup(
        val groupName: String,
        val nick: String,
        val type: Int
    ) : GroupsUiEvent()

    object NavigateToGroupPager : GroupsUiEvent()

}