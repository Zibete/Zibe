package com.zibete.proyecto1.adapters

import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.ui.favorites.FavoriteUserUi

object FavoritesDiffCallback : DiffUtil.ItemCallback<FavoriteUserUi>() {

    override fun areItemsTheSame(oldItem: FavoriteUserUi, newItem: FavoriteUserUi): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: FavoriteUserUi, newItem: FavoriteUserUi): Boolean =
        oldItem == newItem

    override fun getChangePayload(oldItem: FavoriteUserUi, newItem: FavoriteUserUi): Any? {
        val changed = mutableSetOf<String>()

        if (oldItem.isOnline != newItem.isOnline) changed += PayloadFavoriteUser.IS_ONLINE
        if (oldItem.profilePhoto != newItem.profilePhoto) changed += PayloadFavoriteUser.PHOTO_URL
        if (oldItem.name != newItem.name) changed += PayloadFavoriteUser.NAME
        if (oldItem.age != newItem.age) changed += PayloadFavoriteUser.AGE

        return changed.takeIf { it.isNotEmpty() }
    }

    object PayloadFavoriteUser {
        const val IS_ONLINE = "payload_favorite_user_is_online"
        const val PHOTO_URL = "payload_favorite_user_photo_url"
        const val NAME = "payload_favorite_user_name"
        const val AGE = "payload_favorite_user_age"
    }
}



