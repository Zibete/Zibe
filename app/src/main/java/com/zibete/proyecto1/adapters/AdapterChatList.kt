package com.zibete.proyecto1.adapters

import android.graphics.Typeface
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.adapters.ChatListDiffCallback.PayloadConversation
import com.zibete.proyecto1.core.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.core.constants.Constants.FRAGMENT_ID_CHATLIST
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.core.utils.TimeUtils
import com.zibete.proyecto1.data.profile.ProfileRepositoryProvider
import com.zibete.proyecto1.databinding.RowChatListBinding
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.extensions.bindChecks
import com.zibete.proyecto1.ui.extensions.bindStatusIndicator
import com.zibete.proyecto1.ui.extensions.loadAvatar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdapterChatList(
    private val lifecycleScope: CoroutineScope,
    private val myUid: String,
    private val profileRepositoryProvider: ProfileRepositoryProvider,
    private val onChatClicked: (Conversation) -> Unit
) : ListAdapter<Conversation, AdapterChatList.ChatListViewHolder>(ChatListDiffCallback),
    OnCreateContextMenuListener {


    private var contextMenuChatId: String? = null
    fun consumeContextMenuChatId(): String? =
        contextMenuChatId.also { contextMenuChatId = null }

    private var menuReadTitle: CharSequence? = null
    private var menuNotifTitle: CharSequence? = null

//    private var contextMenuPosition: Int = 0


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
        bindFull(
            holder = holder,
            chat = getItem(position)
        )
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

        Glide.with(holder.binding.root).clear(holder.binding.chatListAvatarImage)
        holder.binding.chatListAvatarImage.setImageDrawable(null)

        super.onViewRecycled(holder)
    }

    private fun bindPayload(
        holder: ChatListViewHolder,
        chat: Conversation,
        payload: Any
    ) {
        val changes = payload as? Set<*> ?: run {
            bindFull(holder, chat)
            return
        }

        val b = holder.binding
        val ctx = b.root.context

        if (PayloadConversation.USER_NAME in changes) b.userName.text =
            chat.otherName.takeIf { it.isNotBlank() }
                ?: ctx.getString(R.string.deleted_profile_fallback)

        if (PayloadConversation.MESSAGE in changes) {
            b.lastMessage.text = chat.lastContent
            applyLastMsgStyle(b)
        }

        if (PayloadConversation.STATE in changes) b.offNotifications.isVisible =
            chat.state == CHAT_STATE_SILENT

        if (PayloadConversation.PHOTO_URL in changes) loadAvatar(b, chat.otherPhotoUrl)
        if (PayloadConversation.CREATED_AT in changes) setLastMsgTime(b, chat)
        if (PayloadConversation.UNREAD in changes) bindBadgeUnreadMessage(b, chat)
        if (PayloadConversation.CHECKS in changes) bindChecks(b, chat)

        bindClicks(holder, chat)
    }

    private fun resetEphemeralViews(b: RowChatListBinding) {
        b.offNotifications.isVisible = false
        b.imageViewChecks.isVisible = false
        b.badgeUnReadMessage.isVisible = false
        b.badgeUnReadMessage.text = ""
    }

    private fun bindFull(
        holder: ChatListViewHolder,
        chat: Conversation
    ) {
        val b = holder.binding
        val ctx = b.root.context

        resetEphemeralViews(b)

        b.offNotifications.isVisible = chat.state == CHAT_STATE_SILENT

        b.userName.text = chat.otherName.takeIf { it.isNotBlank() }
            ?: ctx.getString(R.string.deleted_profile_fallback)

        loadAvatar(b, chat.otherPhotoUrl)

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
            contextMenuChatId = chat.otherId.takeIf { it.isNotBlank() }

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
        val unreadCount = chat.unreadCount
        b.badgeUnReadMessage.isVisible = unreadCount > 0
        b.badgeUnReadMessage.text = if (unreadCount > 0) unreadCount.toString() else ""
    }

    private fun bindChecks(b: RowChatListBinding, chat: Conversation) {
        val isMine = chat.userId == myUid
        b.imageViewChecks.bindChecks(isMine, chat.seen)
    }

    private fun bindUserStatus(b: RowChatListBinding, status: UserStatus) {
        val ctx = b.root.context
        b.statusIndicator.bindStatusIndicator(ctx, status)
    }

    private fun loadAvatar(b: RowChatListBinding, photoUrl: String) {
        val url = photoUrl.takeIf { it.isNotBlank() }
        b.chatListAvatarImage.loadAvatar(url)
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

    override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        val ctx = v?.context ?: return
        val titleRead = menuReadTitle ?: ctx.getString(R.string.leido)
        val titleNotif = menuNotifTitle ?: ctx.getString(R.string.menu_user_notifications_off)
        
        menu.add(FRAGMENT_ID_CHATLIST, 1, 0, titleRead)
        menu.add(FRAGMENT_ID_CHATLIST, 2, 0, titleNotif)
        menu.add(FRAGMENT_ID_CHATLIST, 3, 0, R.string.menu_user_block)
        menu.add(FRAGMENT_ID_CHATLIST, 4, 0, R.string.ocultar)
        menu.add(FRAGMENT_ID_CHATLIST, 5, 0, R.string.delete)
    }
}
