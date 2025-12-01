package com.zibete.proyecto1.ui.users

import androidx.lifecycle.ViewModel
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.main.CurrentScreen
import com.zibete.proyecto1.ui.main.MainNavEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@HiltViewModel
class UsersViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userSessionManager: UserSessionManager,
    private val firebaseRefsContainer: FirebaseRefsContainer,
    private val userRepository: UserRepository
) : ViewModel() {


    private val myUid = userSessionManager.user.uid







}