package com.zibete.proyecto1.adapters

import UsersDiffCallback
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.databinding.RowUserBinding
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_AGE
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_DESCRIPTION
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_DISTANCE_METERS
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_NAME
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_ONLINE
import com.zibete.proyecto1.ui.constants.Constants.PAYLOAD_PHOTO_URL
import com.zibete.proyecto1.utils.GlassEffect

class AdapterUsers(
    private val onChatClicked: (String) -> Unit,
    private val onProfileClicked: (Users) -> Unit,
    private val formatDistance: (Double) -> String
) : ListAdapter<Users, AdapterUsers.VH>(UsersDiffCallback) {

    /** Para filtro sin romper submitList (fuente “completa”) */
    private var originalList: List<Users> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = RowUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val u = getItem(position)
        holder.bind(u, formatDistance, onChatClicked, onProfileClicked)
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        val bundle = payloads.lastOrNull() as? Bundle
        if (bundle == null) {
            onBindViewHolder(holder, position)
            return
        }

        holder.bindPayload(
            getItem(position),
            bundle,
            formatDistance)
    }

    fun submitUsers(list: List<Users>) {
        originalList = list
        submitList(list)
    }

    fun filterByName(query: String?) {
        val q = query.orEmpty().trim().lowercase()
        if (q.isEmpty()) {
            submitList(originalList)
        } else {
            submitList(originalList.filter { it.name.lowercase().contains(q) })
        }
    }

    class VH(private val b: RowUserBinding) : RecyclerView.ViewHolder(b.root) {

        init {
            GlassEffect.applyGlassEffect(b.blurView, b.root)
            GlassEffect.startGlowIfAny(b.glowBorder)
        }

        fun bind(
            u: Users,
            formatDistance: (Double) -> String,
            onChatClicked: (String) -> Unit,
            onProfileClicked: (Users) -> Unit
        ) {
            // Imagen
            if (u.photoUrl.isNotBlank()) {
                Glide.with(b.root).load(u.photoUrl).into(b.avatarImage)
            } else {
                b.avatarImage.setImageDrawable(null)
            }

            // Texto
            b.userName.text = u.name

            b.onlineIcon.isVisible = u.isOnline
            b.offlineIcon.isVisible = !u.isOnline

            b.userAge.text = u.age.toString()
            b.userDistance.text = formatDistance(u.distanceMeters)

            val hasDesc = u.description.isNotEmpty()
            b.descriptionContainer.isVisible = hasDesc
            b.userDescription.text = u.description

            // Clicks (cada bind, porque cambia el item)
            b.chatButton.setOnClickListener { onChatClicked(u.id) }
            b.userCard.setOnClickListener { onProfileClicked(u) }
        }

        fun bindPayload(
            u: Users,
            payload: Bundle,
            formatDistance: (Double) -> String
        ) {
            if (payload.containsKey(PAYLOAD_NAME)) {
                b.userName.text = u.name
            }
            if (payload.containsKey(PAYLOAD_AGE)) {
                b.userAge.text = u.age.toString()
            }
            if (payload.containsKey(PAYLOAD_DISTANCE_METERS)) {
                b.userDistance.text = formatDistance(u.distanceMeters)
            }
            if (payload.containsKey(PAYLOAD_DESCRIPTION)) {
                val hasDesc = u.description.isNotEmpty()
                b.descriptionContainer.isVisible = hasDesc
                b.userDescription.text = u.description
            }
            if (payload.containsKey(PAYLOAD_PHOTO_URL)) {
                if (u.photoUrl.isNotBlank()) {
                    Glide.with(b.root).load(u.photoUrl).into(b.avatarImage)
                } else {
                    b.avatarImage.setImageDrawable(null)
                }
            }
            if (payload.containsKey(PAYLOAD_ONLINE)) {
                val isOnline = payload.getBoolean(PAYLOAD_ONLINE)
                b.onlineIcon.isVisible = isOnline
                b.offlineIcon.isVisible = !isOnline
            }
        }
    }
}
