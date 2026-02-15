package com.zibete.proyecto1.adapters

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.ui.users.UsersRowUiModel
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_AGE
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_BLOCKED_BY_ME
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_DESCRIPTION
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_DISTANCE_METERS
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_FAVORITE
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_HAS_BLOCKED_ME
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_NAME
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_NOTIFICATIONS_SILENCED
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_ONLINE
import com.zibete.proyecto1.core.constants.Constants.PAYLOAD_PHOTO_URL

object UsersDiffCallback : DiffUtil.ItemCallback<UsersRowUiModel>() {

    override fun areItemsTheSame(oldItem: UsersRowUiModel, newItem: UsersRowUiModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UsersRowUiModel, newItem: UsersRowUiModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: UsersRowUiModel, newItem: UsersRowUiModel): Any? {
        val diff = Bundle()

        if (oldItem.distanceMeters != newItem.distanceMeters) diff.putBoolean(PAYLOAD_DISTANCE_METERS, true)
        if (oldItem.name != newItem.name) diff.putBoolean(PAYLOAD_NAME, true)
        if (oldItem.age != newItem.age) diff.putBoolean(PAYLOAD_AGE, true)
        if (oldItem.description != newItem.description) diff.putBoolean(PAYLOAD_DESCRIPTION, true)
        if (oldItem.photoUrl != newItem.photoUrl) diff.putBoolean(PAYLOAD_PHOTO_URL, true)
        if (oldItem.isOnline != newItem.isOnline) diff.putBoolean(PAYLOAD_ONLINE, newItem.isOnline)
        if (oldItem.isFavorite != newItem.isFavorite) diff.putBoolean(PAYLOAD_FAVORITE, true)
        if (oldItem.isBlockedByMe != newItem.isBlockedByMe) diff.putBoolean(PAYLOAD_BLOCKED_BY_ME, true)
        if (oldItem.hasBlockedMe != newItem.hasBlockedMe) diff.putBoolean(PAYLOAD_HAS_BLOCKED_ME, true)
        if (oldItem.isNotificationsSilenced != newItem.isNotificationsSilenced) {
            diff.putBoolean(PAYLOAD_NOTIFICATIONS_SILENCED, true)
        }

        return if (diff.isEmpty) null else diff
    }
}
