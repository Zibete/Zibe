package com.zibete.proyecto1.adapters

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.view.ContextMenu
import android.view.View
import android.view.ViewGroup
import android.view.View.OnCreateContextMenuListener
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.data.UserRepository
import com.zibete.proyecto1.data.UserSessionManager
import com.zibete.proyecto1.databinding.RowChatlistaBinding
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_BLOQ
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_CHATWITH
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_HIDE
import com.zibete.proyecto1.ui.constants.Constants.CHAT_STATE_SILENT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

class AdapterChatList(
    private val context: Context,
    private val lifecycleScope: CoroutineScope,
    private val userRepository: UserRepository,
    private val userSessionManager: UserSessionManager
) : ListAdapter<ChatWith, AdapterChatList.ChatListViewHolder>(
    ChatDiffCallback()
), OnCreateContextMenuListener {

    private val user = userRepository.user
    private var menu1: String? = null
    private var menu2: String? = null
    private var contextMenuPosition: Int = 0

    // ---------- DiffUtil ----------

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatWith>() {
        override fun areItemsTheSame(oldItem: ChatWith, newItem: ChatWith): Boolean {
            // Ajustá si tu ChatWith tiene otro identificador único
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: ChatWith, newItem: ChatWith): Boolean {
            return oldItem == newItem
        }
    }

    // ---------- ViewHolder ----------

    class ChatListViewHolder(val binding: RowChatlistaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var statusJob: Job? = null

        init {
            binding.cardview.isVisible = false
            binding.iconConnected.isVisible = false
            binding.iconDisconnected.isVisible = false
            binding.tvStatus.isVisible = false
            binding.notifOff.isVisible = false
            binding.nuevoMsg.isVisible = false
            binding.relativeLayout.isVisible = false
            binding.checked.isVisible = false
            binding.checked2.isVisible = false
        }
    }

    // ---------- Creación ViewHolder ----------

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListViewHolder {
        val binding = RowChatlistaBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.root.setOnCreateContextMenuListener(this)
        return ChatListViewHolder(binding)
    }

    // ---------- Bind con payloads ----------

    override fun onBindViewHolder(
        holder: ChatListViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val chat = getItem(position)

        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            bindFull(holder, chat)
        } else {
            val bundle = payloads.firstOrNull() as? Bundle
            if (bundle != null && bundle.keySet().contains("id")) {
                bindFull(holder, chat)
            } else {
                bindFull(holder, chat)
            }
        }
    }

    override fun onBindViewHolder(holder: ChatListViewHolder, position: Int) {
        bindFull(holder, getItem(position))
    }

    // ---------- Lógica principal de bind ----------

    private fun bindFull(holder: ChatListViewHolder, chat: ChatWith) {

        val binding = holder.binding

        // Card según estado / visibilidad lógica
        applyCardState(binding, chat)

        // Nombre (desde el modelo)
        binding.tvUsuario1.text =
            chat.userName.ifBlank { context.getString(R.string.deleted_profile_fallback) }

        // Foto (desde el modelo)
        Glide.with(context.applicationContext)
            .load(chat.userPhoto)
            .into(binding.imageUser1)

        // Estado online / offline (Flow)
        holder.statusJob?.cancel()
        holder.statusJob = lifecycleScope.launch {
            userRepository.observeUserStatus(chat.userId, Constants.CHAT_STATE_CHATWITH)
                .collectLatest { status ->
                    bindUserStatus(binding, status)
                }
        }

        // Checks (double check, leído, etc. desde el modelo)
        val senderId = chat.senderId
        val seen = chat.seen

        if (senderId == user.uid && seen != 0) {
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

        // No vistos (desde el modelo)
        val noSeen = chat.noSeen
        if (noSeen > 0) {
            binding.nuevoMsg.isVisible = true
            binding.nuevoMsg.text = noSeen.toString()
        } else {
            binding.nuevoMsg.isVisible = false
        }

        // Último mensaje + hora (desde el modelo)
        if (binding.cardview.isVisible) {

            // Texto del último mensaje
            binding.ultMsg.text = chat.msg

            // Formato especial si es foto/audio
            if (chat.msg == context.getString(R.string.photo_send) ||
                chat.msg == context.getString(R.string.photo_received) ||
                chat.msg == context.getString(R.string.audio_send) ||
                chat.msg == context.getString(R.string.audio_received)
            ) {
                binding.ultMsg.setTypeface(null, Typeface.ITALIC)
            } else {
                binding.ultMsg.setTypeface(null, Typeface.NORMAL)
            }

            // Hora del último mensaje
            setLastMsgTime(binding, chat)

            // Marcar mensajes como vistos (en repo)
            setMyDoubleCheck(chat)
        }

        // Click: ir al chat
        binding.cardview.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java).apply {
                putExtra("id_user", chat.userId)
            }
            context.startActivity(intent)
        }

        // Long click: menú contextual
        binding.cardview.setOnLongClickListener {
            setPosition(holder.bindingAdapterPosition)

            menu2 = if (binding.notifOff.isVisible) {
                context.getString(R.string.mostrar_notif)
            } else {
                context.getString(R.string.silenciar)
            }

            menu1 = if (binding.nuevoMsg.isVisible) {
                context.getString(R.string.leido)
            } else {
                context.getString(R.string.noleido)
            }

            false
        }
    }

    // ---------- Helpers de UI ----------

    private fun tintCheck(binding: RowChatlistaBinding, colorRes: Int) {
        val color = ContextCompat.getColor(context, colorRes)
        binding.checked.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        binding.checked2.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun setLastMsgTime(binding: RowChatlistaBinding, chat: ChatWith) {
        val c = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
        val date = chat.dateTime.substring(0, 10)

        binding.horaUltMsg.text = when {
            date == dateFormat.format(c.time) -> {
                chat.dateTime.substring(11, 16)
            }
            date == dateFormat.format(
                Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time
            ) -> {
                context.getString(R.string.yesterday)
            }
            else -> {
                chat.dateTime.substring(0, 10)
            }
        }
    }

    private fun applyCardState(binding: RowChatlistaBinding, chat: ChatWith) {
        val state = chat.state
        val photo = chat.userPhoto

        when (state) {
            CHAT_STATE_CHATWITH -> {
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

    private fun bindUserStatus(
        binding: RowChatlistaBinding,
        status: UserStatus
    ) {
        when (status) {
            is UserStatus.Online -> {
                binding.iconConnected.isVisible = true
                binding.iconDisconnected.isVisible = false
                binding.tvStatus.isVisible = true
                binding.tvStatus.text = binding.root.context.getString(R.string.online)
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
                binding.tvStatus.text = binding.root.context.getString(R.string.offline)
            }
        }
    }

    // ---------- Double check / marcar mensajes vistos ----------

    private fun setMyDoubleCheck(chat: ChatWith) {
        val noSeen = chat.noSeen
        if (noSeen <= 0) return

        lifecycleScope.launch {
            userRepository.markMessagesAsSeen(
                otherUserId = chat.userId,
                chatType = CHAT_STATE_CHATWITH,
                noSeen = noSeen
            )
        }
    }

    // ---------- Context menu ----------

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menu.add(Constants.FRAGMENT_ID_CHATLIST, 1, contextMenuPosition, menu1)
        menu.add(Constants.FRAGMENT_ID_CHATLIST, 2, contextMenuPosition, menu2)
        menu.add(Constants.FRAGMENT_ID_CHATLIST, 3, contextMenuPosition, R.string.bloquear)
        menu.add(Constants.FRAGMENT_ID_CHATLIST, 4, contextMenuPosition, R.string.ocultar)
        menu.add(Constants.FRAGMENT_ID_CHATLIST, 5, contextMenuPosition, R.string.eliminar)
    }

    fun setPosition(position: Int) {
        contextMenuPosition = position
    }
}
