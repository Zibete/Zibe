package com.zibete.proyecto1.adapters

import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.UsersDiffCallback.PayloadUsers
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.databinding.RowUserBinding
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.extensions.bindStatusIndicator
import com.zibete.proyecto1.ui.extensions.loadAvatar
import com.zibete.proyecto1.ui.users.UsersRowUiModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdapterUsers(
    private val lifecycleScope: CoroutineScope,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val onChatClicked: (String) -> Unit,
    private val onProfileClicked: (String) -> Unit,
    private val formatDistance: (Double) -> String
) : ListAdapter<UsersRowUiModel, AdapterUsers.UsersListViewHolder>(UsersDiffCallback) {

    class UsersListViewHolder(val binding: RowUserBinding) : RecyclerView.ViewHolder(binding.root) {
        var statusJob: Job? = null
    }

    private var originalList: List<UsersRowUiModel> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersListViewHolder {
        val b = RowUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UsersListViewHolder(b)
    }

    override fun onBindViewHolder(holder: UsersListViewHolder, position: Int) {
        bindFull(
            holder = holder,
            u = getItem(position),
            formatDistance = formatDistance,
            onChatClicked = onChatClicked,
            onProfileClicked = onProfileClicked
        )
    }

    override fun onBindViewHolder(
        holder: UsersListViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val u = getItem(position)
        val b = holder.binding

        val bundle = payloads.lastOrNull() as? Bundle
        if (bundle == null) {
            onBindViewHolder(holder, position)
            return
        }

        bindPayload(
            b = b,
            u = u,
            payload = bundle,
            formatDistance = formatDistance,
            onChatClicked = onChatClicked,
            onProfileClicked = onProfileClicked
        )
    }

    override fun onViewRecycled(holder: UsersListViewHolder) {
        holder.statusJob?.cancel()
        holder.statusJob = null
        clearRecycled(holder.binding)
        super.onViewRecycled(holder)
    }

    fun submitUsers(list: List<UsersRowUiModel>) {
        val safeList = list.toList()
        originalList = safeList
        submitList(safeList)
    }

    fun clearRecycled(b: RowUserBinding) {
        Glide.with(b.root).clear(b.userListAvatarImage)
        b.userListAvatarImage.setImageDrawable(null)
    }

    private fun resetEphemeralViews(b: RowUserBinding) {
        b.userDescription.isVisible = false
        b.userDescription.text = ""
        b.statusIndicator.isVisible = false
        b.tagFavorite.isVisible = false
        b.tagBlockedByMe.isVisible = false
        b.tagHasBlockedMe.isVisible = false
        b.tagNotificationsSilenced.isVisible = false
    }

    fun bindFull(
        holder: UsersListViewHolder,
        u: UsersRowUiModel,
        formatDistance: (Double) -> String,
        onChatClicked: (String) -> Unit,
        onProfileClicked: (String) -> Unit
    ) {
        val b = holder.binding
        val ctx = b.root.context

        resetEphemeralViews(b)

        b.userName.text = u.name.takeIf { it.isNotBlank() }
            ?: ctx.getString(R.string.deleted_profile_fallback)

        loadAvatar(b, u.photoUrl)

        b.userAge.text = u.age.toString()
        b.userDistance.text = formatDistance(u.distanceMeters)

        b.userDescription.isVisible = u.description.isNotBlank()
        b.userDescription.text = u.description

        holder.statusJob?.cancel()
        holder.statusJob = lifecycleScope.launch {
            profileRepositoryProvider.observeUserStatus(u.id, NODE_DM)
                .collectLatest { status ->
                    bindUserStatus(b, status)
                }
        }

        bindBadges(b, u)
        bindClicks(b, u, onChatClicked, onProfileClicked)
    }

    private fun bindBadges(b: RowUserBinding, u: UsersRowUiModel) {
        b.tagFavorite.isVisible = u.isFavorite
        b.tagBlockedByMe.isVisible = u.isBlockedByMe
        b.tagHasBlockedMe.isVisible = u.hasBlockedMe
        b.tagNotificationsSilenced.isVisible = u.isNotificationsSilenced
    }

    fun bindPayload(
        b: RowUserBinding,
        u: UsersRowUiModel,
        payload: Bundle,
        formatDistance: (Double) -> String,
        onChatClicked: (String) -> Unit,
        onProfileClicked: (String) -> Unit
    ) {

        if (payload.containsKey(PayloadUsers.PHOTO_URL)) loadAvatar(b, u.photoUrl)
        if (payload.containsKey(PayloadUsers.NAME)) b.userName.text = u.name
        if (payload.containsKey(PayloadUsers.AGE)) b.userAge.text = u.age.toString()
//        if (payload.containsKey(PAYLOAD_ONLINE)) b.statusIndicator.isVisible = u.isOnline

        if (payload.containsKey(PayloadUsers.FAVORITE)
            || payload.containsKey(PayloadUsers.BLOCKED_BY_ME)
            || payload.containsKey(PayloadUsers.HAS_BLOCKED_ME)
            || payload.containsKey(PayloadUsers.NOTIFICATIONS_SILENCED)
        ) bindBadges(b, u)

        if (payload.containsKey(PayloadUsers.DISTANCE_METERS)) b.userDistance.text =
            formatDistance(u.distanceMeters)

        if (payload.containsKey(PayloadUsers.DESCRIPTION)) {
            val hasDesc = u.description.isNotBlank()
            b.userDescription.isVisible = hasDesc
            b.userDescription.text = u.description
        }

        bindClicks(b, u, onChatClicked, onProfileClicked)
    }

    private fun bindClicks(
        b: RowUserBinding,
        u: UsersRowUiModel,
        onChatClicked: (String) -> Unit,
        onProfileClicked: (String) -> Unit
    ) {
        val gestureDetector =
            GestureDetector(b.root.context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onProfileClicked(u.id)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onChatClicked(u.id)
                    return true
                }
            })
        b.glassContainer.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                }
            }
            true
        }
    }

    private fun bindUserStatus(b: RowUserBinding, status: UserStatus) {
        val ctx = b.root.context
        b.statusIndicator.isVisible = true
        b.statusIndicator.bindStatusIndicator(ctx, status)
    }

    private fun loadAvatar(b: RowUserBinding, photoUrl: String) {
        val url = photoUrl.takeIf { it.isNotBlank() }
        b.userListAvatarImage.loadAvatar(url)
    }

}
