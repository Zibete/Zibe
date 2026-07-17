package com.zibete.proyecto1.ui.users

interface DiscoverToolbarHandler {
    val hasActiveFilters: Boolean

    fun onFilterRequested()
}
