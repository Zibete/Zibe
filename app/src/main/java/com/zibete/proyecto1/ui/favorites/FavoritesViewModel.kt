package com.zibete.proyecto1.ui.favorites

import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.utils.FirebaseRefs
import com.zibete.proyecto1.utils.Utils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState

    private val currentUser get() = userRepository.user

    fun loadFavorites() {
        _uiState.update { it.copy(isLoading = true, error = null, isEmpty = false) }

        FirebaseRefs.refDatos.child(currentUser.uid).child("FavoriteList")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                favorites = emptyList(),
                                isEmpty = true
                            )
                        }
                        return
                    }

                    val pending = snapshot.childrenCount
                    var processed = 0L
                    val result = mutableListOf<FavoriteUserUi>()

                    fun checkComplete() {
                        if (processed < pending) return

                        val sorted = result.sortedBy { f -> f.name.lowercase() }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                favorites = sorted,
                                isEmpty = sorted.isEmpty()
                            )
                        }
                    }

                    snapshot.children.forEach { favChild ->
                        val favUserId = favChild.getValue(String::class.java)

                        if (favUserId.isNullOrEmpty()) {
                            processed++
                            checkComplete()
                            return@forEach
                        }

                        // Verificamos que el usuario siga existiendo
                        FirebaseRefs.refCuentas.child(favUserId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnap: DataSnapshot) {
                                    if (!userSnap.exists()) {
                                        // Limpieza: si ya no existe el user, borramos de favoritos
                                        favChild.ref.removeValue()
                                        processed++
                                        checkComplete()
                                        return
                                    }

                                    val u = userSnap.getValue(Users::class.java)
                                    if (u == null) {
                                        processed++
                                        checkComplete()
                                        return
                                    }

                                    val age = Utils.calcAge(u.birthDay)

                                    // Estado online/offline
                                    FirebaseRefs.refDatos.child(favUserId).child("Estado")
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(stateSnap: DataSnapshot) {
                                                val estado = stateSnap.child("estado")
                                                    .getValue(String::class.java)
                                                val online = estado == "Online" // o R.string.online, si querés, pero acá mantenemos literal

                                                result.add(
                                                    FavoriteUserUi(
                                                        id = favUserId,
                                                        name = u.name ?: "",
                                                        age = age,
                                                        profilePhoto = u.profilePhoto,
                                                        isOnline = online
                                                    )
                                                )

                                                processed++
                                                checkComplete()
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                processed++
                                                checkComplete()
                                            }
                                        })
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    processed++
                                    checkComplete()
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message,
                            favorites = emptyList()
                        )
                    }
                }
            })
    }

    fun refreshFavorites() {
        loadFavorites()
    }
}
