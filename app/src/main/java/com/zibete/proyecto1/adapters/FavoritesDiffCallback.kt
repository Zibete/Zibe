package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_ONLINE
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_PHOTO_URL
import com.zibete.proyecto1.ui.favorites.FavoriteUserUi

object FavoritesDiffCallback : DiffUtil.ItemCallback<FavoriteUserUi>() {

    override fun areItemsTheSame(oldItem: FavoriteUserUi, newItem: FavoriteUserUi): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: FavoriteUserUi, newItem: FavoriteUserUi): Boolean =
        oldItem == newItem

    override fun getChangePayload(oldItem: FavoriteUserUi, newItem: FavoriteUserUi): Any? {
        val changed = mutableSetOf<String>()

        if (oldItem.isOnline != newItem.isOnline) changed += PAYLOAD_ONLINE
        if (oldItem.profilePhoto != newItem.profilePhoto) changed += PAYLOAD_PHOTO_URL

        return changed.takeIf { it.isNotEmpty() }
    }
}




