package com.zibete.proyecto1.utils

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.github.clans.fab.FloatingActionButton
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.AdapterPhotoReceived
import com.zibete.proyecto1.data.GroupRepository
import com.zibete.proyecto1.data.UserPreferencesRepository
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.di.firebase.FirebaseRefsContainer
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.NODE_GROUP_CHAT
import com.zibete.proyecto1.ui.constants.Constants.NODE_CURRENT_CHAT
import com.zibete.proyecto1.ui.constants.Constants.PUBLIC_USER
import com.zibete.proyecto1.utils.Utils.calcAge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileUiBinder @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val firebaseRefsContainer: FirebaseRefsContainer
) {

    private val myUid = userRepository.myUid

    // === UI Perfil (chat normal / chat grupo) ===
    suspend fun setProfile(
        context: Context,
        userId: String,
        subMenuGroupChat: FloatingActionButton,
        subMenuChatWith: FloatingActionButton,
        ageView: TextView
    ) {

        val user = userRepository.getUserProfile(userId) ?: return

        // Edad
        val ageUser = calcAge(user.birthDay)
        ageView.text = ageUser.toString()

        val userGroup = groupRepository.getUserGroup(userId, userPreferencesRepository.groupName)

        // Grupo
        if (userGroup != null && userGroup.type == PUBLIC_USER) {
            subMenuGroupChat.isVisible = true
            subMenuGroupChat.labelText =
                context.getString(R.string.chat_private_in_group, userPreferencesRepository.groupName)

            subMenuGroupChat.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("userId", userId)
                    putExtra("nodeType", NODE_GROUP_CHAT)
                    putExtra("userName", user.name)
                    addFlags(
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                }
                context.startActivity(intent)
            }

        } else {
            subMenuGroupChat.isVisible = false
        }

        // Chat
        subMenuChatWith.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("nodeType", NODE_CURRENT_CHAT)
                putExtra("userName", user.name)
                addFlags(
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            context.startActivity(intent)
        }
    }

    // === Favoritos ===
    suspend fun setFavorite(
        userId: String,
        favOn: ImageView,
        favOff: ImageView
    ) {

        val isFavorite = userRepository.isUserFavorite(userId)

        favOn.isVisible = isFavorite
        favOff.isVisible = !isFavorite

    }

    // === Bloqueo ===
    suspend fun blockState(
        userId: String,
        blockMeIcon: ImageView,
        blockIcon: ImageView
    ) {

        val blockState = userRepository.getBlockStateWith(userId)

        blockIcon.isVisible = blockState.iBlockedUser
        blockMeIcon.isVisible = blockState.userBlockedMe
    }


    // === Fotos recibidas en perfil ===
    fun addPhotoReceived(
        userId: String?,
        adapter: AdapterPhotoReceived,
        linearPhotos: LinearLayout
    ) {
        if (userId.isNullOrEmpty()) return

        // Mis mensajes hacia él
        FirebaseRefs.refChat.child("${myUid} <---> $userId").child("Mensajes")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    addPhoto(snapshot, adapter, linearPhotos)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })

        // Sus mensajes hacia mí
        FirebaseRefs.refChat.child("$userId <---> ${myUid}").child("Mensajes")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    addPhoto(snapshot, adapter, linearPhotos)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addPhoto(
        snapshot: DataSnapshot,
        adapter: AdapterPhotoReceived,
        linearPhotos: LinearLayout
    ) {
        if (!snapshot.exists()) return

        val type = snapshot.child("type").getValue(Int::class.java)
        val sender = snapshot.child("envia").getValue(String::class.java)
        val url = snapshot.child("mensaje").getValue(String::class.java)

        if (sender != myUid && url != null && (type == Constants.MSG_PHOTO || type == Constants.MSG_PHOTO_SENDER_DLT)) {
            adapter.addString(url)
            linearPhotos.visibility = View.VISIBLE
        }
    }


}
