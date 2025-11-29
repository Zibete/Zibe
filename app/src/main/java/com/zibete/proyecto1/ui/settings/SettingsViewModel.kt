package com.zibete.proyecto1.ui.settings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

// ESTADO INTERNO (Propiedades que antes eran 'lateinit' en la Activity)
// Ahora son privadas y gestionadas por el VM.
private const val CHAT_TYPE_INDIVIDUAL = "CHAT"
private const val CHAT_TYPE_UNKNOWN = "CHATWITHUNKNOWN"
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userSessionManager: UserSessionManager,
    private val userRepository: UserRepository
) : ViewModel() {

    fun setUserOnline() = viewModelScope.launch { userRepository.setUserOnline() }
    fun setUserOffline() = viewModelScope.launch { userRepository.setUserLastSeen() }

}