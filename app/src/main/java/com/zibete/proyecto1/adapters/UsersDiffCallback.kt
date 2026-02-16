package com.zibete.proyecto1.adapters

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import com.zibete.proyecto1.ui.users.UsersRowUiModel

object UsersDiffCallback : DiffUtil.ItemCallback<UsersRowUiModel>() {

    override fun areItemsTheSame(oldItem: UsersRowUiModel, newItem: UsersRowUiModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UsersRowUiModel, newItem: UsersRowUiModel): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: UsersRowUiModel, newItem: UsersRowUiModel): Any? {
        val diff = Bundle()

        if (oldItem.distanceMeters != newItem.distanceMeters) diff.putBoolean(
            PayloadUsers.DISTANCE_METERS,
            true
        )
        if (oldItem.name != newItem.name) diff.putBoolean(PayloadUsers.NAME, true)
        if (oldItem.age != newItem.age) diff.putBoolean(PayloadUsers.AGE, true)
        if (oldItem.description != newItem.description) diff.putBoolean(
            PayloadUsers.DESCRIPTION,
            true
        )
        if (oldItem.photoUrl != newItem.photoUrl) diff.putBoolean(PayloadUsers.PHOTO_URL, true)
        if (oldItem.isFavorite != newItem.isFavorite) diff.putBoolean(PayloadUsers.FAVORITE, true)
        if (oldItem.isBlockedByMe != newItem.isBlockedByMe) diff.putBoolean(
            PayloadUsers.BLOCKED_BY_ME,
            true
        )
        if (oldItem.hasBlockedMe != newItem.hasBlockedMe) diff.putBoolean(
            PayloadUsers.HAS_BLOCKED_ME,
            true
        )
        if (oldItem.isNotificationsSilenced != newItem.isNotificationsSilenced) {
            diff.putBoolean(PayloadUsers.NOTIFICATIONS_SILENCED, true)
        }

        return if (diff.isEmpty) null else diff
    }

    object PayloadUsers {
        const val DISTANCE_METERS = "payload_user_distance_meters"
        const val NAME = "payload_user_name"
        const val AGE = "payload_user_age"
        const val DESCRIPTION = "payload_user_description"
        const val PHOTO_URL = "payload_user_photo_url"
        const val FAVORITE = "payload_user_favorite"
        const val BLOCKED_BY_ME = "payload_user_blocked_by_me"
        const val HAS_BLOCKED_ME = "payload_user_blocked_me"
        const val NOTIFICATIONS_SILENCED = "payload_user_notifications_silenced"
    }
}
