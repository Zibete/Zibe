package com.zibete.proyecto1.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ContextMenu
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.RowGroupMsgLeftBinding
import com.zibete.proyecto1.databinding.RowGroupMsgRightBinding
import com.zibete.proyecto1.databinding.RowNotifBinding
import com.zibete.proyecto1.model.ChatGroup
import com.zibete.proyecto1.ui.constants.Constants
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_MID
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_LEFT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_RIGHT
import com.zibete.proyecto1.utils.FirebaseRefs

class AdapterChatGroup(
    private val context: Context,
    private val maxSize: Int,
    initialList: List<ChatGroup> = emptyList(),
    // --- ACCIONES (CALLBACKS) ---
    private val onImageClicked: (url: String) -> Unit,
    private val onUserSingleTap: (chat: ChatGroup, view: View) -> Unit,
    private val onUserDoubleTap: (chat: ChatGroup, view: View) -> Unit
) : ListAdapter<ChatGroup, RecyclerView.ViewHolder>(DIFF),
    View.OnCreateContextMenuListener {

    private val mutableList = initialList.toMutableList()
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid
    private var positionForContext = 0

    init {
        submitList(mutableList.toList())
    }

    // ==== Public API ====

    fun addChat(chat: ChatGroup) {
        if (mutableList.size > maxSize) {
            mutableList.removeAt(0)
        }
        mutableList.add(chat)
        submitList(mutableList.toList())
    }

    fun getPositionForContext(): Int = positionForContext

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.type == 0 -> MSG_TYPE_MID
            item.senderUid == userId -> MSG_TYPE_RIGHT
            else -> MSG_TYPE_LEFT
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        // La lógica del menú se maneja en el Fragment/Activity usando getPositionForContext()
    }

    // ==== ViewHolders Creation ====

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            MSG_TYPE_MID -> MidVH(RowNotifBinding.inflate(inf, parent, false)).also {
                it.itemView.setOnCreateContextMenuListener(this)
            }
            MSG_TYPE_RIGHT -> RightVH(RowGroupMsgRightBinding.inflate(inf, parent, false)).also {
                it.itemView.setOnCreateContextMenuListener(this)
            }
            else -> LeftVH(RowGroupMsgLeftBinding.inflate(inf, parent, false)).also {
                it.itemView.setOnCreateContextMenuListener(this)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = getItem(position)

        // Guardar posición para ContextMenu al hacer long click
        holder.itemView.setOnLongClickListener {
            positionForContext = holder.bindingAdapterPosition
            false // Retorna false para permitir que se abra el menú contextual
        }

        when (holder) {
            is MidVH -> holder.bind(chat)
            is RightVH -> {
                holder.bind(chat)
                attachGestures(holder, chat)
            }
            is LeftVH -> {
                holder.bind(chat)
                attachGestures(holder, chat)
            }
        }
    }

    // ==== Gestures Logic ====

    private fun attachGestures(holder: BaseMsgVH, chat: ChatGroup) {
        if (chat.type == 0) return

        val gd = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (chat.senderUid == userId) return true
                // Delegamos la lógica compleja al Fragment
                onUserDoubleTap(chat, holder.itemView)
                return true
            }

            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                // Delegamos la navegación al Fragment
                onUserSingleTap(chat, holder.itemView)
                return false
            }
        })

        holder.binding.linearCardMsg.setOnTouchListener { _, event ->
            gd.onTouchEvent(event)
            // No consumimos el evento completamente para permitir scroll y context menu
            false
        }
    }

    // ==== ViewHolders & Binding ====

    private class MidVH(private val b: RowNotifBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(model: ChatGroup) {
            b.tvMsg.text = model.content
            b.nameUser.text = model.nameUser
            b.horaMsg.text = model.date.safeSub(11, 16)
        }
    }

    private abstract class BaseMsgVH(root: View) : RecyclerView.ViewHolder(root) {
        abstract val binding: CommonMsgBinding
    }

    private data class CommonMsgBinding(
        val linearCardMsg: View,
        val linearMensajeMsg: View?,
        val linearMensajePic: View?,
        val imgPic: ImageView,
        val loadingPhoto: View?,
        val tvMsg: TextView,
        val horaMsg: TextView,
        val imgUser: ImageView,
        val nameUser: TextView
    )

    private inner class RightVH(b: RowGroupMsgRightBinding) : BaseMsgVH(b.root) {
        override val binding = CommonMsgBinding(b.linearCardMsg, b.linearMensajeMsg, b.linearMensajePic, b.imgPic, b.loadingPhoto, b.tvMsg, b.horaMsg, b.imgUser, b.nameUser)
        fun bind(model: ChatGroup) = bindCommon(binding, model)
    }

    private inner class LeftVH(b: RowGroupMsgLeftBinding) : BaseMsgVH(b.root) {
        override val binding = CommonMsgBinding(b.linearCardMsg, b.linearMensajeMsg, b.linearMensajePic, b.imgPic, b.loadingPhoto, b.tvMsg, b.horaMsg, b.imgUser, b.nameUser)
        fun bind(model: ChatGroup) = bindCommon(binding, model)
    }

    private fun bindCommon(b: CommonMsgBinding, chat: ChatGroup) {
        b.horaMsg.text = chat.date.safeSub(11, 16)

        // --- FOTO O TEXTO ---
        when {
            chat.type.isPhoto() -> {
                b.linearMensajePic?.visibility = View.VISIBLE
                b.linearMensajeMsg?.visibility = View.GONE
                b.loadingPhoto?.visibility = View.VISIBLE

                Glide.with(context)
                    .load(chat.content)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(35)))
                    .listener(object : RequestListener<Drawable> {

                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable?>,
                            isFirstResource: Boolean
                        ): Boolean {
                            b.loadingPhoto?.visibility = View.GONE
                            return false                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable?>?,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            b.loadingPhoto?.visibility = View.GONE
                            return false
                        }
                    })
                    .into(b.imgPic)

                b.imgPic.setOnClickListener {
                    onImageClicked(chat.content)
                }
            }
            chat.type.isText() -> {
                b.linearMensajePic?.visibility = View.GONE
                b.linearMensajeMsg?.visibility = View.VISIBLE
                b.tvMsg.text = chat.content
            }
        }

        // --- INFO DE USUARIO ---
        // TODO: En el futuro, mover esta carga de datos (FirebaseRefs) a un ViewModel y pasar solo datos listos.
        if (chat.userType == 0) {
            b.nameUser.text = if (chat.type == 0) chat.nameUser else "${chat.nameUser}:"
            Glide.with(context).load(context.getString(R.string.URL_PHOTO_DEF)).into(b.imgUser)
        } else {
            FirebaseRefs.refCuentas.child(chat.senderUid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    if (ds.exists()) {
                        val name = ds.child("nombre").getValue(String::class.java)
                        val foto = ds.child("foto").getValue(String::class.java)
                        b.nameUser.text = if (chat.type == 0) name ?: chat.nameUser else "${name ?: chat.nameUser}:"
                        Glide.with(context).load(foto).into(b.imgUser)
                    } else {
                        b.nameUser.text = "${chat.nameUser}:"
                        Glide.with(context).load(context.getString(R.string.URL_PHOTO_DEF)).into(b.imgUser)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    // ==== Extensions Helpers ====

    private fun Int?.isPhoto(): Boolean =
        this == Constants.MSG_PHOTO || this == Constants.MSG_PHOTO_RECEIVER_DLT || this == Constants.MSG_PHOTO_SENDER_DLT

    private fun Int?.isText(): Boolean =
        this == Constants.MSG_TEXT || this == Constants.MSG_TEXT_RECEIVER_DLT || this == Constants.MSG_TEXT_SENDER_DLT

    companion object {

        private fun String?.safeSub(start: Int, end: Int): String {
            val s = this ?: return ""
            if (start < 0 || end <= start || start >= s.length) return ""
            return try { s.substring(start, end.coerceAtMost(s.length)) } catch (_: Exception) { "" }
        }

        val DIFF = object : DiffUtil.ItemCallback<ChatGroup>() {
            override fun areItemsTheSame(oldItem: ChatGroup, newItem: ChatGroup): Boolean {
                // Usamos ID y tiempo como clave única compuesta
                return oldItem.senderUid == newItem.senderUid && oldItem.date == newItem.date
            }
            override fun areContentsTheSame(oldItem: ChatGroup, newItem: ChatGroup): Boolean = oldItem == newItem
        }
    }
}