package com.zibete.proyecto1.adapters

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.FRAGMENT_ID_CHATLIST
import com.zibete.proyecto1.core.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.core.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.utils.TimeUtils
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.databinding.RowChatListBinding
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdapterChatList(
    private val lifecycleScope: CoroutineScope,
    private val userRepository: UserRepository,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val onChatClicked: (Conversation) -> Unit
) : ListAdapter<Conversation, AdapterChatList.ChatListViewHolder>(
    ChatListDiffCallback
), OnCreateContextMenuListener {

    private var contextMenuPosition: Int = 0
    private var menuReadTitle: CharSequence? = null
    private var menuNotifTitle: CharSequence? = null

    class ChatListViewHolder(val binding: RowChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var statusJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = RowChatListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.root.setOnCreateContextMenuListener(this)
        return ChatListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        bindFull(holder, getItem(position))
    }

    override fun onBindViewHolder(
        holder: ChatListViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val item = getItem(position)
        val payload = payloads.firstOrNull()

        if (payload == null) {
            bindFull(holder, item)
            return
        }

        bindPayload(holder, item, payload)
    }

    override fun onViewRecycled(holder: ChatListViewHolder) {
        holder.statusJob?.cancel()
        holder.statusJob = null
        super.onViewRecycled(holder)
    }

    private fun bindPayload(holder: ChatListViewHolder, chat: Conversation, payload: Any) {
        val changes = payload as? Set<*> ?: run {
            bindFull(holder, chat)
            return
        }

        val b = holder.binding
        val ctx = b.root.context

        if ("state" in changes) b.offNotifications.isVisible = chat.state == CHAT_STATE_SILENT
        if ("name" in changes) b.userName.text = chat.otherName.takeIf { it.isNotBlank() }
            ?: ctx.getString(R.string.deleted_profile_fallback)
        if ("photo" in changes) Glide.with(ctx).load(chat.otherPhotoUrl).into(b.circleImageView)
        if ("msg" in changes) {
            b.lastMessage.text = chat.lastContent.orEmpty()
            applyLastMsgStyle(b)
        }
        if ("time" in changes) setLastMsgTime(b, chat)
        if ("unread" in changes) bindBadgeUnreadMessage(b, chat)
        if ("checks" in changes) bindChecks(b, chat)

        // click/longclick dependen de state/categorías → los re-aplicamos siempre
        bindClicks(holder, chat)
    }

    private fun bindFull(holder: ChatListViewHolder, chat: Conversation) {
        val b = holder.binding
        val ctx = b.root.context

        b.offNotifications.isVisible = chat.state == CHAT_STATE_SILENT

        b.userName.text = chat.otherName.takeIf { it.isNotBlank() }
            ?: ctx.getString(R.string.deleted_profile_fallback)

        Glide.with(ctx).load(chat.otherPhotoUrl).into(b.circleImageView)

        holder.statusJob?.cancel()
        holder.statusJob = lifecycleScope.launch {
            profileRepositoryProvider.observeUserStatus(chat.otherId, NODE_DM)
                .collectLatest { status ->
                    bindUserStatus(b, status)
                }
        }

        b.lastMessage.text = chat.lastContent
        applyLastMsgStyle(b)
        setLastMsgTime(b, chat)
        bindBadgeUnreadMessage(b, chat)
        bindChecks(b, chat)
        bindClicks(holder, chat)
    }

    private fun bindClicks(holder: ChatListViewHolder, chat: Conversation) {
        val b = holder.binding
        val ctx = b.root.context

        b.root.setOnClickListener { onChatClicked(chat) }

        b.root.setOnLongClickListener {
            contextMenuPosition =
                holder.bindingAdapterPosition.coerceAtLeast(0)

            menuNotifTitle =
                if (b.offNotifications.isVisible) ctx.getString(R.string.menu_user_notifications_on)
                else ctx.getString(R.string.menu_user_notifications_off)

            menuReadTitle =
                if (b.badgeUnReadMessage.isVisible) ctx.getString(R.string.leido)
                else ctx.getString(R.string.noleido)

            false
        }
    }

    private fun bindBadgeUnreadMessage(b: RowChatListBinding, chat: Conversation) {
        val noSeen = chat.unreadCount
        if (noSeen > 0) {
            b.badgeUnReadMessage.isVisible = true
            b.badgeUnReadMessage.text = noSeen.toString()
        } else b.badgeUnReadMessage.isVisible = false
    }

    private fun bindChecks(b: RowChatListBinding, chat: Conversation) {
        val myUid = userRepository.myUid
        val senderUid = chat.userId
        val seen = chat.seen

        if (senderUid == myUid) {
            b.imageViewChecks.isVisible = true
            val iconRes = when (seen) {
                MSG_DELIVERED -> R.drawable.ic_check_24
                else -> R.drawable.ic_double_check_24
            }

            b.imageViewChecks.setImageResource(iconRes)
            val tintRes = if (seen == MSG_SEEN) R.color.check_seen else R.color.check_not_seen
            val tintColor = ContextCompat.getColor(b.root.context, tintRes)
            b.imageViewChecks.imageTintList = ColorStateList.valueOf(tintColor)
        } else {
            b.imageViewChecks.isVisible = false
        }
    }

    private fun bindUserStatus(b: RowChatListBinding, status: UserStatus) {
        val ctx = b.root.context
        val colorRes = when (status) {
            is UserStatus.Online,
            is UserStatus.TypingOrRecording -> R.color.status_online

            is UserStatus.LastSeen,
            is UserStatus.Offline -> R.color.status_offline
        }
        val circleColor = ContextCompat.getColor(ctx, colorRes)
        val strokeColor = ContextCompat.getColor(ctx, R.color.status_stroke)

        b.statusIndicator.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(circleColor)
            setStroke(4, strokeColor)
        }
    }

    private fun applyLastMsgStyle(binding: RowChatListBinding) {
        val ctx = binding.root.context
        val m = binding.lastMessage.text?.toString().orEmpty()

        val isMedia =
            m == ctx.getString(R.string.photo_send) ||
                    m == ctx.getString(R.string.photo_received) ||
                    m == ctx.getString(R.string.audio_send) ||
                    m == ctx.getString(R.string.audio_received)

        binding.lastMessage.setTypeface(null, if (isMedia) Typeface.ITALIC else Typeface.NORMAL)
    }

    private fun setLastMsgTime(
        binding: RowChatListBinding,
        chat: Conversation
    ) {
        binding.hourLastMessage.text =
            TimeUtils.formatConversationTimestamp(
                ms = chat.lastMessageAt,
                context = binding.root.context
            )
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val ctx = v?.context ?: return
        val titleRead = menuReadTitle ?: ctx.getString(R.string.leido)
        val titleNotif = menuNotifTitle ?: ctx.getString(R.string.menu_user_notifications_off)

        menu.add(FRAGMENT_ID_CHATLIST, 1, contextMenuPosition, titleRead)
        menu.add(FRAGMENT_ID_CHATLIST, 2, contextMenuPosition, titleNotif)
        menu.add(FRAGMENT_ID_CHATLIST, 3, contextMenuPosition, R.string.menu_user_block)
        menu.add(FRAGMENT_ID_CHATLIST, 4, contextMenuPosition, R.string.ocultar)
        menu.add(FRAGMENT_ID_CHATLIST, 5, contextMenuPosition, R.string.delete)
    }
}
