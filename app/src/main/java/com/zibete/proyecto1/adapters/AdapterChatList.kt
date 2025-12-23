package com.zibete.proyecto1.adapters

import android.graphics.PorterDuff
import android.graphics.Typeface
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
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.databinding.RowChatlistaBinding
import com.zibete.proyecto1.model.Conversation
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import com.zibete.proyecto1.ui.constants.Constants.FRAGMENT_ID_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.NODE_DM
import com.zibete.proyecto1.utils.TimeUtils
import com.zibete.proyecto1.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdapterChatList(
    private val lifecycleScope: CoroutineScope,
    private val userRepository: UserRepository,
    private val onChatClicked: (Conversation) -> Unit
) : ListAdapter<Conversation, AdapterChatList.ChatListViewHolder>(
    ChatListDiffCallback
), OnCreateContextMenuListener {

    private var contextMenuPosition: Int = 0

    private var menuReadTitle: CharSequence? = null
    private var menuNotifTitle: CharSequence? = null

    class ChatListViewHolder(val binding: RowChatlistaBinding) : RecyclerView.ViewHolder(binding.root) {
        var statusJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = RowChatlistaBinding.inflate(
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

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int, payloads: MutableList<Any>) {
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

        if ("state" in changes) applyCardState(b, chat)
        if ("name" in changes) b.tvUsuario1.text = chat.otherName.takeIf { it.isNotBlank() }
            ?: ctx.getString(R.string.deleted_profile_fallback)
        if ("photo" in changes) Glide.with(ctx).load(chat.otherPhotoUrl).into(b.imageUser1)
        if ("msg" in changes) {
            b.ultMsg.text = chat.lastContent.orEmpty()
            applyLastMsgStyle(b)
        }
        if ("time" in changes) setLastMsgTime(b, chat)
        if ("unread" in changes) bindUnreadBadge(b, chat)
        if ("checks" in changes) bindChecks(b, chat)

        // click/longclick dependen de state/categorías → los re-aplicamos siempre
        bindClicks(holder, chat)
    }

    private fun bindFull(holder: ChatListViewHolder, chat: Conversation) {
        val b = holder.binding
        val ctx = b.root.context

        applyCardState(b, chat)

        b.tvUsuario1.text = chat.otherName.takeIf { it.isNotBlank() }
            ?: ctx.getString(R.string.deleted_profile_fallback)

        Glide.with(ctx).load(chat.otherPhotoUrl).into(b.imageUser1)

        holder.statusJob?.cancel()
        holder.statusJob = lifecycleScope.launch {
            userRepository.observeUserStatus(chat.otherId, NODE_DM)
                .collectLatest { status ->
                    bindUserStatus(b, status)
                }
        }

        b.ultMsg.text = chat.lastContent
        applyLastMsgStyle(b)
        setLastMsgTime(b, chat)
        bindUnreadBadge(b, chat)
        bindChecks(b, chat)

        bindClicks(holder, chat)
    }

    private fun bindClicks(holder: ChatListViewHolder, chat: Conversation) {
        val b = holder.binding
        val ctx = b.root.context

        b.cardview.setOnClickListener {
            onChatClicked(chat)
        }

        b.cardview.setOnLongClickListener {
            contextMenuPosition = holder.bindingAdapterPosition.coerceAtLeast(0)

            menuNotifTitle = if (b.notifOff.isVisible) {
                ctx.getString(R.string.menu_notifications_on)
            } else {
                ctx.getString(R.string.menu_notifications_off)
            }

            menuReadTitle = if (b.nuevoMsg.isVisible) {
                ctx.getString(R.string.leido)
            } else {
                ctx.getString(R.string.noleido)
            }

            false
        }
    }

    private fun bindUnreadBadge(binding: RowChatlistaBinding, chat: Conversation) {
        val noSeen = chat.unreadCount
        if (noSeen > 0) {
            binding.nuevoMsg.isVisible = true
            binding.nuevoMsg.text = noSeen.toString()
        } else {
            binding.nuevoMsg.isVisible = false
        }
    }

    private fun applyLastMsgStyle(binding: RowChatlistaBinding) {
        val ctx = binding.root.context
        val m = binding.ultMsg.text?.toString().orEmpty()

        val isMedia =
            m == ctx.getString(R.string.photo_send) ||
                    m == ctx.getString(R.string.photo_received) ||
                    m == ctx.getString(R.string.audio_send) ||
                    m == ctx.getString(R.string.audio_received)

        binding.ultMsg.setTypeface(null, if (isMedia) Typeface.ITALIC else Typeface.NORMAL)
    }

    private fun setLastMsgTime(
        binding: RowChatlistaBinding,
        chat: Conversation
    ) {
        binding.horaUltMsg.text =
            TimeUtils.formatConversationTimestamp(
                ms = chat.lastMessageAt,
                context = binding.root.context
            )
    }


    private fun applyCardState(binding: RowChatlistaBinding, chat: Conversation) {
        val state = chat.state
        val photo = chat.otherPhotoUrl

        when (state) {
            NODE_DM -> {
                binding.cardview.isVisible = true
                binding.notifOff.isVisible = false
            }
            CHAT_STATE_SILENT -> {
                binding.cardview.isVisible = true
                binding.notifOff.isVisible = true
            }
            CHAT_STATE_BLOQ, CHAT_STATE_HIDE -> {
                binding.cardview.isVisible = false
            }
            else -> {
                binding.cardview.isVisible = photo != Constants.EMPTY
            }
        }
    }

    private fun bindChecks(binding: RowChatlistaBinding, chat: Conversation) {
        val myUid = userRepository.myUid
        val senderId = chat.userId
        val seen = chat.seen

        if (senderId == myUid && seen != 0) {
            binding.relativeLayout.isVisible = true

            when (seen) {
                1 -> {
                    binding.checked.isVisible = true
                    binding.checked2.isVisible = false
                    tintCheck(binding, R.color.blanco)
                }
                2 -> {
                    binding.checked.isVisible = true
                    binding.checked2.isVisible = true
                    tintCheck(binding, R.color.blanco)
                }
                3 -> {
                    binding.checked.isVisible = true
                    binding.checked2.isVisible = true
                    tintCheck(binding, R.color.visto)
                }
                else -> {
                    binding.checked.isVisible = false
                    binding.checked2.isVisible = false
                    binding.relativeLayout.isVisible = false
                }
            }
        } else {
            binding.checked.isVisible = false
            binding.checked2.isVisible = false
            binding.relativeLayout.isVisible = false
        }
    }

    private fun tintCheck(binding: RowChatlistaBinding, colorRes: Int) {
        val ctx = binding.root.context
        val color = ContextCompat.getColor(ctx, colorRes)
        binding.checked.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.checked2.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun bindUserStatus(binding: RowChatlistaBinding, status: UserStatus) {
        val ctx = binding.root.context
        when (status) {
            is UserStatus.Online -> {
                binding.iconConnected.isVisible = true
                binding.iconDisconnected.isVisible = false
                binding.tvStatus.isVisible = true
                binding.tvStatus.text = ctx.getString(R.string.online)
            }
            is UserStatus.TypingOrRecording -> {
                binding.iconConnected.isVisible = true
                binding.iconDisconnected.isVisible = false
                binding.tvStatus.isVisible = true
                binding.tvStatus.text = status.text
            }
            is UserStatus.LastSeen -> {
                binding.iconConnected.isVisible = false
                binding.iconDisconnected.isVisible = true
                binding.tvStatus.isVisible = true
                binding.tvStatus.text = status.text
            }
            is UserStatus.Offline -> {
                binding.iconConnected.isVisible = false
                binding.iconDisconnected.isVisible = false
                binding.tvStatus.isVisible = true
                binding.tvStatus.text = ctx.getString(R.string.offline)
            }
        }
    }

    // ---------- Context menu ----------

    override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        val ctx = v?.context ?: return
        val titleRead = menuReadTitle ?: ctx.getString(R.string.leido)
        val titleNotif = menuNotifTitle ?: ctx.getString(R.string.menu_notifications_off)

        menu.add(FRAGMENT_ID_CHATLIST, 1, contextMenuPosition, titleRead)
        menu.add(FRAGMENT_ID_CHATLIST, 2, contextMenuPosition, titleNotif)
        menu.add(FRAGMENT_ID_CHATLIST, 3, contextMenuPosition, R.string.menu_block)
        menu.add(FRAGMENT_ID_CHATLIST, 4, contextMenuPosition, R.string.ocultar)
        menu.add(FRAGMENT_ID_CHATLIST, 5, contextMenuPosition, R.string.eliminar)
    }
}
