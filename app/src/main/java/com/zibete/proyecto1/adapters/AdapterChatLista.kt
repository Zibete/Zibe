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
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.ChatActivity
import com.zibete.proyecto1.utils.Constants
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.RowChatlistaBinding
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.utils.FirebaseRefs.refChat
import com.zibete.proyecto1.utils.FirebaseRefs.refCuentas
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
import com.zibete.proyecto1.utils.UserRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections

class AdapterChatLista(
    private val chatList: MutableList<ChatWith>,
    private val context: Context
) : RecyclerView.Adapter<AdapterChatLista.ChatListViewHolder>(),
    Filterable,
    OnCreateContextMenuListener {

    private val user get() = currentUser!!

    private val fullChatList: MutableList<ChatWith> = mutableListOf()

    private var menu1: String? = null
    private var menu2: String? = null
    private var contextMenuPosition: Int = 0

    private var doubleCheckListener: ValueEventListener? = null

    // ---------- ViewHolder ----------

    class ChatListViewHolder(val binding: RowChatlistaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Estado inicial controlado por código (no por XML)
            binding.cardview.isVisible = false
            binding.iconConectado.isVisible = false
            binding.iconDesconectado.isVisible = false
            binding.tvEstado.isVisible = false
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
        val chat = chatList[position]

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
        // Mantengo override vacío porque usamos la versión con payloads.
        // bindFull se maneja desde el otro override.
    }

    // ---------- Lógica principal de bind ----------

    private fun bindFull(holder: ChatListViewHolder, chat: ChatWith) {

        val binding = holder.binding

        // Card según estado / visibilidad lógica
        applyCardState(binding, chat)

        // Nombre
        refCuentas.child(chat.userId).child("nombre")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.getValue(String::class.java)
                        binding.tvUsuario1.text = name.orEmpty()
                    } else {
                        binding.tvUsuario1.text = "${chat.userName} (Perfil eliminado)"
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Foto
        refCuentas.child(chat.userId).child("foto")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val url = if (snapshot.exists()) {
                        snapshot.getValue(String::class.java)
                    } else {
                        chat.userPhoto
                    }

                    Glide.with(context.applicationContext)
                        .load(url)
                        .into(binding.imageUser1)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Estado online / offline
        UserRepository.stateUser(
            context,
            chat.userId,
            binding.iconConectado,
            binding.iconDesconectado,
            binding.tvEstado,
            Constants.CHATWITH
        )

        // Checks (double check, leído, etc.)
        refDatos.child(chat.userId)
            .child(Constants.CHATWITH)
            .child(user.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        binding.checked.isVisible = false
                        binding.checked2.isVisible = false
                        binding.relativeLayout.isVisible = false
                        return
                    }

                    val sender = snapshot.child("wEnvia").getValue(String::class.java)
                    val visto = snapshot.child("wVisto").getValue(Int::class.java)

                    if (sender == user.uid && visto != null) {
                        binding.relativeLayout.isVisible = true

                        when (visto) {
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

                override fun onCancelled(error: DatabaseError) {}
            })

        // No vistos
        refDatos.child(user.uid)
            .child(Constants.CHATWITH)
            .child(chat.userId)
            .child("noVisto")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val noVistos = snapshot.getValue(Int::class.java) ?: 0
                    if (noVistos > 0) {
                        binding.nuevoMsg.isVisible = true
                        binding.nuevoMsg.text = noVistos.toString()
                    } else {
                        binding.nuevoMsg.isVisible = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Último mensaje + hora
        refDatos.child(user.uid)
            .child(Constants.CHATWITH)
            .child(chat.userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return

                    if (binding.cardview.isVisible) {
                        binding.ultMsg.text = chat.msg

                        if (chat.msg == context.getString(R.string.photo_send) ||
                            chat.msg == context.getString(R.string.photo_received) ||
                            chat.msg == context.getString(R.string.audio_send) ||
                            chat.msg == context.getString(R.string.audio_received)
                        ) {
                            binding.ultMsg.setTypeface(null, Typeface.ITALIC)
                        } else {
                            binding.ultMsg.setTypeface(null, Typeface.NORMAL)
                        }

                        setLastMsgTime(binding, chat)
                        setMyDoubleCheck(chat)
                    }

                    refDatos.child(user.uid)
                        .child(Constants.CHATWITH)
                        .child(chat.userId)
                        .child("wVisto")
                        .setValue(2)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

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
            date == dateFormat.format(Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time) -> {
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
            Constants.CHATWITH -> {
                binding.cardview.isVisible = true
                binding.notifOff.isVisible = false
            }
            "silent" -> {
                binding.cardview.isVisible = true
                binding.notifOff.isVisible = true
            }
            "bloq", "delete" -> {
                binding.cardview.isVisible = false
            }
            else -> {
                binding.cardview.isVisible = photo != Constants.EMPTY
            }
        }
    }

    // ---------- Double check ----------

    private fun setMyDoubleCheck(chat: ChatWith) {

        refDatos.child(user.uid)
            .child(Constants.CHATWITH)
            .child(chat.userId)
            .child("noVisto")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val noVistos = snapshot.getValue(Int::class.java) ?: return
                    if (noVistos <= 0) return

                    fun markSeen(ds: DataSnapshot) {
                        for (msg in ds.children) {
                            msg.ref.child("visto").setValue(2)
                        }
                    }

                    refChat.child("${user.uid} <---> ${chat.userId}")
                        .child("Mensajes")
                        .limitToLast(noVistos)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(ds: DataSnapshot) {
                                if (ds.exists()) {
                                    markSeen(ds)
                                } else {
                                    refChat.child("${chat.userId} <---> ${user.uid}")
                                        .child("Mensajes")
                                        .limitToLast(noVistos)
                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(ds2: DataSnapshot) {
                                                if (ds2.exists()) markSeen(ds2)
                                            }

                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // ---------- Filter ----------

    private val filterChats = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.lowercase()?.trim().orEmpty()
            val filtered = if (query.isEmpty()) {
                fullChatList
            } else {
                fullChatList.filter {
                    it.userName.lowercase().contains(query)
                }.toMutableList()
            }

            filtered.sort()
            return FilterResults().apply { values = filtered }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            val newList = results?.values as? MutableList<ChatWith> ?: mutableListOf()
            updateData(newList)
        }
    }

    override fun getFilter(): Filter = filterChats

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

    // ---------- Data helpers ----------

    override fun getItemCount(): Int = chatList.size

    fun addChats(chat: ChatWith) {
        chatList.add(chat)
        fullChatList.add(chat)
        notifyItemInserted(chatList.size - 1)
    }

    fun deleteChat(chat: ChatWith) {
        val index = chatList.indexOf(chat)
        if (index != -1) {
            chatList.removeAt(index)
            fullChatList.remove(chat)
            notifyItemRemoved(index)
        }
    }

    fun updateData(newList: List<ChatWith>) {
        val diffCallback = ChatDiffCallback(oldList = chatList, newList = newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        chatList.clear()
        chatList.addAll(newList)

        fullChatList.clear()
        fullChatList.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

}
