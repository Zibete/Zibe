//package com.zibete.proyecto1.adapters
//
//import android.content.Context
//import android.graphics.PorterDuff
//import android.graphics.Typeface
//import android.view.ContextMenu
//import android.view.LayoutInflater
//import android.view.View
//import android.view.View.OnCreateContextMenuListener
//import android.view.ViewGroup
//import android.widget.Filter
//import android.widget.Filterable
//import androidx.core.content.ContextCompat
//import androidx.core.view.isVisible
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.ValueEventListener
//import com.zibete.proyecto1.R
//import com.zibete.proyecto1.databinding.RowChatlistaBinding
//import com.zibete.proyecto1.model.Conversation
//import com.zibete.proyecto1.ui.constants.Constants
//import com.zibete.proyecto1.utils.FirebaseRefs.refDatos
//import java.text.SimpleDateFormat
//import java.util.Calendar
//
//class AdapterChatGroupsList(
//    private val chatList: MutableList<Conversation>,
//    private val context: Context,
//    // --- ACCIONES (Callbacks) ---
//    private val onChatClicked: (Conversation) -> Unit,
//    private val onChatSeen: (Conversation) -> Unit,       // Para marcar wVisto = 2
//    private val onMarkAsRead: (Conversation) -> Unit      // Para la lógica de doble check (setMyDoubleCheck)
//) : RecyclerView.Adapter<AdapterChatGroupsList.ChatGroupViewHolder>(),
//    Filterable,
//    OnCreateContextMenuListener {
//
//    private val fullChatList: MutableList<Conversation> = mutableListOf()
//
//    // Variables para el menú contextual
//    private var menu1: String? = null
//    private var menu2: String? = null
//    private var contextMenuPosition: Int = 0
//
//    var user = FirebaseAuth.getInstance().currentUser!!
//
//    // -------- ViewHolder --------
//    class ChatGroupViewHolder(val binding: RowChatlistaBinding) : RecyclerView.ViewHolder(binding.root) {
//        init {
//            binding.cardview.isVisible = false
//            binding.iconConnected.isVisible = false
//            binding.iconDisconnected.isVisible = false
//            binding.tvStatus.isVisible = false
//            binding.notifOff.isVisible = false
//            binding.nuevoMsg.isVisible = false
//            binding.relativeLayout.isVisible = false
//            binding.checked.isVisible = false
//            binding.checked2.isVisible = false
//        }
//    }
//
//    // -------- onCreateViewHolder --------
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatGroupViewHolder {
//        val binding = RowChatlistaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        binding.root.setOnCreateContextMenuListener(this)
//        return ChatGroupViewHolder(binding)
//    }
//
//    // -------- onBindViewHolder --------
//    override fun onBindViewHolder(holder: ChatGroupViewHolder, position: Int, payloads: MutableList<Any>) {
//        val chat = chatList[position]
//        // Guardamos posición para el ContextMenu
//        holder.itemView.setOnLongClickListener {
//            setPosition(holder.bindingAdapterPosition)
//            prepareContextMenuLabels(holder.binding)
//            false // return false permite que se abra el menú
//        }
//
//        bindFull(holder, chat)
//    }
//
//    override fun onBindViewHolder(holder: ChatGroupViewHolder, position: Int) =
//        onBindViewHolder(holder, position, mutableListOf())
//
//    // -------- Bind Completo --------
//    private fun bindFull(holder: ChatGroupViewHolder, chat: Conversation) {
//        val binding = holder.binding
//
//        // 1. UI Básica
//        applyCardState(binding, chat)
//        binding.tvUsuario1.text = chat.otherName
//        try {
//            Glide.with(context.applicationContext).load(chat.otherPhotoUrl).into(binding.imageUser1)
//        } catch (_: Exception) {}
//
//        // 2. Estado Online (Visual)
////        UserRepository.stateUser(context, chat.userId, binding.iconConectado, binding.iconDesconectado, binding.tvEstado, Constants.CHATWITHUNKNOWN)
//
//        // 3. Listener: Estado "Visto" (Visual)
//        refDatos.child(chat.otherId).child(Constants.NODE_GROUP_DM).child(user.uid)
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (!snapshot.exists()) {
//                        hideChecks(binding)
//                        return
//                    }
//                    val sender = snapshot.child("wEnvia").getValue(String::class.java)
//                    val visto = snapshot.child("wVisto").getValue(Int::class.java)
//
//                    if (sender == user.uid && visto != null) {
//                        binding.relativeLayout.isVisible = true
//                        updateCheckIcons(binding, visto)
//                    } else {
//                        hideChecks(binding)
//                    }
//                }
//                override fun onCancelled(error: DatabaseError) {}
//            })
//
//        // 4. Listener: No Vistos (Counter Visual)
//        refDatos.child(user.uid).child(Constants.NODE_GROUP_DM).child(chat.otherId).child("noVisto")
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val noVistos = snapshot.getValue(Int::class.java) ?: 0
//                    binding.nuevoMsg.isVisible = noVistos > 0
//                    if (noVistos > 0) binding.nuevoMsg.text = noVistos.toString()
//                }
//                override fun onCancelled(error: DatabaseError) {}
//            })
//
//        // 5. Listener: Último Mensaje (Visual + Triggers)
//        refDatos.child(user.uid).child(Constants.NODE_GROUP_DM).child(chat.otherId)
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (!snapshot.exists()) return
//
//                    if (binding.cardview.isVisible) {
//                        binding.ultMsg.text = chat.lastContent
//                        applyTextStyle(binding, chat.lastContent)
//                        setLastMsgTime(binding, chat)
//
//                        // TRIGGER: Delegamos la lógica de marcar como leídos al Fragmento
//                        onMarkAsRead(chat)
//                    }
//
//                    // TRIGGER: Avisamos que el chat fue visto (wVisto = 2)
//                    onChatSeen(chat)
//                }
//                override fun onCancelled(error: DatabaseError) {}
//            })
//
//        // 6. Click Listener (Navegación delegada)
//        binding.cardview.setOnClickListener {
//            onChatClicked(chat)
//        }
//    }
//
//    // -------- Helpers UI --------
//
//    private fun prepareContextMenuLabels(binding: RowChatlistaBinding) {
//        menu2 = if (binding.notifOff.isVisible) context.getString(R.string.menu_notifications_on) else context.getString(R.string.menu_notifications_off)
//        menu1 = if (binding.nuevoMsg.isVisible) context.getString(R.string.leido) else context.getString(R.string.noleido)
//    }
//
//    private fun applyCardState(binding: RowChatlistaBinding, chat: Conversation) {
//        when (chat.state) {
//            Constants.NODE_GROUP_DM -> {
//                binding.cardview.isVisible = true
//                binding.notifOff.isVisible = false
//            }
//            "silent" -> {
//                binding.cardview.isVisible = true
//                binding.notifOff.isVisible = true
//            }
//            "bloq", "delete" -> binding.cardview.isVisible = false
//            else -> binding.cardview.isVisible = chat.otherPhotoUrl != Constants.EMPTY
//        }
//    }
//
//    private fun updateCheckIcons(binding: RowChatlistaBinding, visto: Int) {
//        when (visto) {
//            1 -> {
//                binding.checked.isVisible = true
//                binding.checked2.isVisible = false
//                tintChecks(binding, R.color.blanco)
//            }
//            2 -> {
//                binding.checked.isVisible = true
//                binding.checked2.isVisible = true
//                tintChecks(binding, R.color.blanco)
//            }
//            3 -> {
//                binding.checked.isVisible = true
//                binding.checked2.isVisible = true
//                tintChecks(binding, R.color.visto)
//            }
//            else -> hideChecks(binding)
//        }
//    }
//
//    private fun hideChecks(binding: RowChatlistaBinding) {
//        binding.checked.isVisible = false
//        binding.checked2.isVisible = false
//        binding.relativeLayout.isVisible = false
//    }
//
//    private fun tintChecks(binding: RowChatlistaBinding, colorRes: Int) {
//        val color = ContextCompat.getColor(context, colorRes)
//        binding.checked.setColorFilter(color, PorterDuff.Mode.SRC_IN)
//        binding.checked2.setColorFilter(color, PorterDuff.Mode.SRC_IN)
//    }
//
//    private fun applyTextStyle(binding: RowChatlistaBinding, msg: String) {
//        val italicMsgs = listOf(
//            context.getString(R.string.photo_send),
//            context.getString(R.string.photo_received),
//            context.getString(R.string.audio_send),
//            context.getString(R.string.audio_received)
//        )
//        if (msg in italicMsgs) binding.ultMsg.setTypeface(null, Typeface.ITALIC)
//        else binding.ultMsg.setTypeface(null, Typeface.NORMAL)
//    }
//
//    private fun setLastMsgTime(binding: RowChatlistaBinding, chat: Conversation) {
//        val c = Calendar.getInstance()
//        val dateFormat = SimpleDateFormat("dd/MM/yyyy")
//        val date = chat.lastDate.safeSub(0, 10)
//
//        binding.horaUltMsg.text = when {
//            date == dateFormat.format(c.time) -> chat.lastDate.safeSub(11, 16)
//            date == dateFormat.format(Calendar.getInstance().apply { add(Calendar.DATE, -1) }.time) -> context.getString(R.string.yesterday)
//            else -> date
//        }
//    }
//
//    private fun String.safeSub(start: Int, end: Int): String {
//        return try { this.substring(start, end.coerceAtMost(this.length)) } catch (_: Exception) { "" }
//    }
//
//    // -------- Filter --------
//    private val filterChats = object : Filter() {
//        override fun performFiltering(constraint: CharSequence?): FilterResults {
//            val query = constraint?.toString()?.lowercase()?.trim().orEmpty()
//            val filtered: MutableList<Conversation> = if (query.isEmpty()) {
//                fullChatList.toMutableList()
//            } else {
//                fullChatList.filter { it.otherName.lowercase().contains(query) }.toMutableList()
//            }
//            filtered.sort()
//            return FilterResults().apply { values = filtered }
//        }
//
//        @Suppress("UNCHECKED_CAST")
//        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
//            val newList = results?.values as? MutableList<Conversation> ?: mutableListOf()
//            updateData(newList)
//        }
//    }
//
//    override fun getFilter(): Filter = filterChats
//
//    // -------- Context menu --------
//    override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
//        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 1, contextMenuPosition, menu1)
//        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 2, contextMenuPosition, menu2)
//        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 3, contextMenuPosition, R.string.menu_block)
//        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 4, contextMenuPosition, R.string.ocultar)
//        menu.add(Constants.FRAGMENT_ID_CHATGROUPLIST, 5, contextMenuPosition, R.string.eliminar)
//    }
//
//    fun setPosition(position: Int) {
//        contextMenuPosition = position
//    }
//
//    // -------- Data Methods --------
//    override fun getItemCount(): Int = chatList.size
//
//    fun addChats(chat: Conversation) {
//        chatList.add(chat)
//        fullChatList.add(chat)
//        notifyItemInserted(chatList.size - 1)
//    }
//
//    fun deleteChat(chat: Conversation) {
//        val index = chatList.indexOf(chat)
//        if (index != -1) {
//            chatList.removeAt(index)
//            fullChatList.remove(chat)
//            notifyItemRemoved(index)
//        }
//    }
//
//    fun updateData(newList: List<Conversation>) {
//        val diffCallback = ChatDiffCallback(oldList = chatList, newList = newList)
//        val diffResult = DiffUtil.calculateDiff(diffCallback)
//        chatList.clear()
//        chatList.addAll(newList)
//        fullChatList.clear()
//        fullChatList.addAll(newList)
//        diffResult.dispatchUpdatesTo(this)
//    }
//}