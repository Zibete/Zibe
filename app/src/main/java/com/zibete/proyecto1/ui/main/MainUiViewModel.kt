package com.zibete.proyecto1.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainUiViewModel : ViewModel() {

    // Visibilidad de toolbar
    private val _toolbarVisible = MutableStateFlow(true)
    val toolbarVisible: StateFlow<Boolean> = _toolbarVisible

    // Visibilidad de layoutSettings
    private val _layoutSettingsVisible = MutableStateFlow(false)
    val layoutSettingsVisible: StateFlow<Boolean> = _layoutSettingsVisible

    // Si querés, más adelante:
    // currentScreen, bottomNavVisible, etc.

    fun showToolbar() { _toolbarVisible.value = true }
    fun hideToolbar() { _toolbarVisible.value = false }

    fun showLayoutSettings() { _layoutSettingsVisible.value = true }
    fun hideLayoutSettings() { _layoutSettingsVisible.value = false }
}
