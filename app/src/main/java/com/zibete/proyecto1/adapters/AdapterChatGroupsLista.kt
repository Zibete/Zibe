package com.zibete.proyecto1.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.View.OnCreateContextMenuListener
import android.widget.Filter
import android.widget.Filterable
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.ChatActivity
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.FragmentChatListBinding
import com.zibete.proyecto1.databinding.RowChatlistaBinding
import com.zibete.proyecto1.model.ChatWith
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.utils.FirebaseRefs.refChatUnknown
import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
import com.zibete.proyecto1.utils.FirebaseRefs.refGroupUsers
import com.zibete.proyecto1.utils.FirebaseRefs.currentUser
import com.zibete.proyecto1.utils.UserRepository
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections

class AdapterChatGroupsLista(
    private val chatList: MutableList<ChatWith>,
    private val context: Context
) : RecyclerView.Adapter<AdapterChatGroupsLista.ChatGroupViewHolder>(),
    Filterable,
    OnCreateContextMenuListener {

    private val fullChatList: MutableList<ChatWith> = mutableListOf()
    private var menu1: String? = null
    private var menu2: String? = null
    private var contextMenuPosition: Int = 0
    private val user get() = currentUser!!

    // -------- ViewHolder --------

    class ChatGroupViewHolder(val binding: RowChatlistaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Estado inicial controlado por código
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

    // -------- onCreateViewHolder --------

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatGroupViewHolder {
        val binding = RowChatlistaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.root.setOnCreateContextMenuListener(this)
        return ChatGroupViewHolder(binding)
    }

    // -------- onBindViewHolder con payloads --------

    override fun onBindViewHolder(
        holder: ChatGroupViewHolder,
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

    override fun onBindViewHolder(holder: ChatGroupViewHolder, position: Int) {
        // Usamos siempre la variante con payloads
    }

    // -------- Bind principal --------

    private fun bindFull(holder: ChatGroupViewHolder, chat: ChatWith) {

        val binding = holder.binding

        // Mostrar/ocultar card según estado
        applyCardState(binding, chat)

        // Nombre (en grupos unknown viene del modelo)
        binding.tvUsuario1.text = chat.userName

        // Foto
        Glide.with(context.applicationContext)
            .load(chat.userPhoto)
            .into(binding.imageUser1)

        // Estado online/offline
        UserRepository.stateUser(
            context,
            chat.userId,
            binding.iconConectado,
            binding.iconDesconectado,
            binding.tvEstado,
            Constants.CHATWITHUNKNOWN
        )

        // Checked (visto) para unknown
        refDatos.child(chat.userId)
            .child(Constants.CHATWITHUNKNOWN)
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
                                tintChecks(binding, R.color.blanco)
                            }
                            2 -> {
                                binding.checked.isVisible = true
                                binding.checked2.isVisible = true
                                tintChecks(binding, R.color.blanco)
                            }
                            3 -> {
                                binding.checked.isVisible = true
                                binding.checked2.isVisible = true
                                tintChecks(binding, R.color.visto)
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
            .child(Constants.CHATWITHUNKNOWN)
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
            .child(Constants.CHATWITHUNKNOWN)
            .child(chat.userId)
            .addValueEventListener(object : ValueEventListener {
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
                        .child(Constants.CHATWITHUNKNOWN)
                        .child(chat.userId)
                        .child("wVisto")
                        .setValue(2)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        // Click: abrir chat only if sigue en el grupo
        binding.cardview.setOnClickListener { v ->
            refGroupUsers.child(UsuariosFragment.groupName)
                .child(chat.userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val intent = Intent(context, ChatActivity::class.java).apply {
                                putExtra("unknownName", chat.userName)
                                putExtra("idUserUnknown", chat.userId)
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(
                                context,
                                "Lo sentimos, ${chat.userName} ya no está disponible",
                                Toast.LENGTH_SHORT
                            ).show()

                            refDatos.child(user.uid)
                                .child(Constants.CHATWITHUNKNOWN)
                                .child(chat.userId)
                                .removeValue()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
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

    // -------- Helpers UI --------

    private fun applyCardState(binding: RowChatlistaBinding, chat: ChatWith) {
        val state = chat.state
        val photo = chat.userPhoto

        when (state) {
            Constants.CHATWITHUNKNOWN -> {
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

    private fun tintChecks(binding: RowChatlistaBinding, colorRes: Int) {
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

    // -------- Double check unknown --------

    private fun setMyDoubleCheck(chat: ChatWith) {

        refDatos.child(user.uid)
            .child(Constants.CHATWITHUNKNOWN)
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

                    refChatUnknown.child("${user.uid} <---> ${chat.userId}")
                        .child("Mensajes")
                        .limitToLast(noVistos)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(ds: DataSnapshot) {
                                if (ds.exists()) {
                                    markSeen(ds)
                                } else {
                                    refChatUnknown.child("${chat.userId} <---> ${user.uid}")
                                        .child("Mensajes")
                                        .limitToLast(noVistos)
                                        .addListenerForSingleValueEvent(object :
                                            ValueEventListener {
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

    // -------- Filter --------

    private val filterChats = object : Filter() {

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.lowercase()?.trim().orEmpty()
            val filtered: MutableList<ChatWith> = if (query.isEmpty()) {
                fullChatList.toMutableList()
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

    // -------- Context menu --------

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 1, contextMenuPosition, menu1)
        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 2, contextMenuPosition, menu2)
        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 3, contextMenuPosition, R.string.bloquear)
        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 4, contextMenuPosition, R.string.ocultar)
        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 5, contextMenuPosition, R.string.eliminar)
    }

    fun setPosition(position: Int) {
        contextMenuPosition = position
    }

    // -------- Data helpers --------

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