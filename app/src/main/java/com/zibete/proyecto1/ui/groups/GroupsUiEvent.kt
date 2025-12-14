package com.zibete.proyecto1.ui.groups

sealed interface GroupsUiEvent {
    data class NickInUse(val nick: String) : GroupsUiEvent

    data class GroupNameInUse(val name: String) : GroupsUiEvent

    object NavigateToGroupPager : GroupsUiEvent

    data class ShowMessage(val message: String?) : GroupsUiEvent

}