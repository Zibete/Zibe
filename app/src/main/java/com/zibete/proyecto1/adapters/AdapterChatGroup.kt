package com.zibete.proyecto1.adapters

import android.content.Context
import android.content.Intent
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
import androidx.core.content.ContextCompat
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.zibete.proyecto1.ChatActivity
import com.zibete.proyecto1.utils.Constants
import com.zibete.proyecto1.PerfilActivity
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.databinding.RowGroupMsgLeftBinding
import com.zibete.proyecto1.databinding.RowGroupMsgRightBinding
import com.zibete.proyecto1.databinding.RowNotifBinding
import com.zibete.proyecto1.model.ChatsGroup
import com.zibete.proyecto1.ui.UsuariosFragment
import com.zibete.proyecto1.utils.FirebaseRefs

class AdapterChatGroup(
    msgList: ArrayList<ChatsGroup>,
    private val maxSize: Int,
    private val context: Context
) : ListAdapter<ChatsGroup, RecyclerView.ViewHolder>(DIFF),
    View.OnCreateContextMenuListener {

    private val mutable = msgList.toMutableList()
    private val photoList = arrayListOf<String?>()
    private var positionForContext = 0

    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        msgList.forEach { if (it.typeMsg.isPhoto()) photoList.add(it.message) }
        submitList(mutable.toList())
        setHasStableIds(false)
    }

    // ==== Public API (compat con tu código original) ====

    fun addChat(chats: ChatsGroup) {
        if (mutable.size > maxSize) {
            mutable.removeAt(0)
        }
        mutable.add(chats)

        if (chats.typeMsg.isPhoto()) {
            if (photoList.size > maxSize) photoList.removeAt(0)
            photoList.add(chats.message)
        }
        submitList(mutable.toList())
    }

    fun setPosition(position: Int) {
        positionForContext = position
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.typeMsg == 0 -> MSG_TYPE_MID
            item.id == userId -> MSG_TYPE_RIGHT
            else -> MSG_TYPE_LEFT
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        //
    }

    // ==== Adapter ====

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
        val chats = getItem(position)
        when (holder) {
            is MidVH   -> holder.bind(chats)  // notificaciones (typeMsg == 0)
            is RightVH -> {
                holder.bind(chats)
                attachGestures(holder, chats)
            }
            is LeftVH  -> {
                holder.bind(chats)
                attachGestures(holder, chats)
            }
        }
    }

    // ==== Gestos (single y double tap) ====
    private fun attachGestures(holder: BaseMsgVH, chats: ChatsGroup) {
        if (chats.typeMsg == 0) return

        val gd = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (chats.id == userId) return true

                FirebaseRefs.refGroupUsers.child(UsuariosFragment.groupName)
                    .child(chats.id)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (!dataSnapshot.exists()) {
                                noDisponible(holder, chats, wasRemovedFromGroup = true)
                                return
                            }

                            val thisname = dataSnapshot.child("user_name").getValue(String::class.java)
                            val type = dataSnapshot.child("type").getValue(Int::class.java) ?: 0

                            val allowed = if (type == 0) {
                                // Usuario desconocido (incógnito), debe coincidir el nombre actual
                                thisname == chats.name
                            } else {
                                // Usuario normal, depende del typeUser del mensaje
                                chats.typeUser == 1
                            }

                            if (allowed) {
                                val intent = Intent(context, ChatActivity::class.java)
                                    .putExtra("unknownName", chats.name)
                                    .putExtra("idUserUnknown", chats.id)
                                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                                context.startActivity(intent)
                            } else {
                                noDisponible(holder, chats, wasRemovedFromGroup = false)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                return true
            }

            override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
                if (chats.typeUser == 0) {
                    showSnack(holder, context.getString(R.string.perfil_incognito))
                } else if (chats.id != userId) {
                    val intent = Intent(context, PerfilActivity::class.java)
                        .putExtra("id_user", chats.id)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                }
                return false
            }
        })

        holder.binding.linearCardMsg.setOnTouchListener { _, event ->
            val handled = gd.onTouchEvent(event)
            if (handled) {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) setPosition(pos)
            }
            handled
        }

        holder.binding.linearCardMsg.setOnLongClickListener {
            // placeholder (tu original tenía comentario)
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) setPosition(pos)
            false
        }
    }

    private fun noDisponible(holder: BaseMsgVH, chats: ChatsGroup, wasRemovedFromGroup: Boolean) {
        val text = if (chats.typeUser == 0) {
            context.getString(R.string.user_not_available_fmt, chats.name)
        } else {
            if (wasRemovedFromGroup)
                context.getString(R.string.user_not_in_chat_anymore_fmt, chats.name)
            else
                context.getString(R.string.user_not_available_fmt, chats.name)
        }

        showSnack(holder, text)

        // Limpia del "chat con desconocido"
        userId?.let { uid ->
            FirebaseRefs.refDatos.child(uid)
                .child(Constants.CHATWITHUNKNOWN)
                .child(chats.id)
                .removeValue()
        }
    }

    private fun showSnack(holder: BaseMsgVH, message: String) {
        val snack = Snackbar.make(holder.binding.linearMensajeMsg ?: holder.binding.linearCardMsg, message, Snackbar.LENGTH_SHORT)
        val bg = ContextCompat.getColor(context, R.color.colorC)
        snack.setBackgroundTint(bg)
        val tv = snack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        snack.show()
    }

    // ==== ViewHolders ====

    private class MidVH(private val b: RowNotifBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(model: ChatsGroup) {
            // Mensaje de "notificación" en medio (typeMsg == 0)
            b.tvMsg.text = model.message
            b.nameUser.text = model.name
            b.horaMsg.text = model.dateTime.substring(11, 16)
            // Si tenés otros campos/estilos en row_notif, aplicalos acá
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

    private inner class RightVH(private val b: RowGroupMsgRightBinding) : BaseMsgVH(b.root) {
        override val binding = CommonMsgBinding(
            linearCardMsg = b.linearCardMsg,
            linearMensajeMsg = b.linearMensajeMsg,
            linearMensajePic = b.linearMensajePic,
            imgPic = b.imgPic,
            loadingPhoto = b.loadingPhoto,
            tvMsg = b.tvMsg,
            horaMsg = b.horaMsg,
            imgUser = b.imgUser,
            nameUser = b.nameUser
        )

        fun bind(model: ChatsGroup) = bindCommon(binding, model, isMe = true)
    }

    private inner class LeftVH(private val b: RowGroupMsgLeftBinding) : BaseMsgVH(b.root) {
        override val binding = CommonMsgBinding(
            linearCardMsg = b.linearCardMsg,
            linearMensajeMsg = b.linearMensajeMsg,
            linearMensajePic = b.linearMensajePic,
            imgPic = b.imgPic,
            loadingPhoto = b.loadingPhoto,
            tvMsg = b.tvMsg,
            horaMsg = b.horaMsg,
            imgUser = b.imgUser,
            nameUser = b.nameUser
        )

        fun bind(model: ChatsGroup) = bindCommon(binding, model, isMe = false)
    }

    // ==== Bind común para Left/Right ====

    private fun bindCommon(b: CommonMsgBinding, chats: ChatsGroup, isMe: Boolean) {
        b.horaMsg.text = chats.dateTime.safeSub(11, 16)

        when {
            chats.typeMsg.isPhoto() -> {
                b.linearMensajePic?.visibility = View.VISIBLE
                b.linearMensajeMsg?.visibility = View.GONE

                b.loadingPhoto?.visibility = View.VISIBLE
                Glide.with(context)
                    .load(chats.message)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(35)))
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean
                        ): Boolean {
                            b.loadingPhoto?.visibility = View.GONE
                            return false
                        }
                        override fun onResourceReady(
                            resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean
                        ): Boolean {
                            b.loadingPhoto?.visibility = View.GONE
                            return false
                        }
                    })
                    .into(b.imgPic!!)

                b.imgPic.setOnClickListener { v ->
                    val i = Intent(context, SlidePhotoActivity::class.java)
                        .putExtra("photoList", photoList)
                        .putExtra("position", photoList.indexOf(chats.message))
                        .putExtra("rotation", 0)
                    v.context.startActivity(i)
                }
            }

            chats.typeMsg.isText() -> {
                b.linearMensajePic?.visibility = View.GONE
                b.linearMensajeMsg?.visibility = View.VISIBLE
                b.tvMsg.text = chats.message ?: ""
            }

            chats.typeMsg == 0 -> {
                // Notificación (ya la maneja MidVH, pero por si acaso)
                b.tvMsg.text = chats.message ?: ""
            }
        }

        // Nombre y foto de usuario
        if (chats.typeUser == 0) {
            b.nameUser.text = if (chats.typeMsg == 0) chats.name else "${chats.name}:"
            Glide.with(context).load(context.getString(R.string.URL_PHOTO_DEF)).into(b.imgUser)
        } else {
            FirebaseRefs.refCuentas.child(chats.id).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(ds: DataSnapshot) {
                    if (ds.exists()) {
                        val name = ds.child("nombre").getValue(String::class.java)
                        val foto = ds.child("foto").getValue(String::class.java)
                        b.nameUser.text = if (chats.typeMsg == 0) name ?: chats.name else "${name ?: chats.name}:"
                        Glide.with(context).load(foto).into(b.imgUser)
                    } else {
                        b.nameUser.text = "${chats.name}:"
                        Glide.with(context).load(context.getString(R.string.URL_PHOTO_DEF)).into(b.imgUser)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    // ==== Utils ====

    private fun Int?.isPhoto(): Boolean = when (this) {
        Constants.PHOTO, Constants.PHOTO_RECEIVER_DLT, Constants.PHOTO_SENDER_DLT -> true
        else -> false
    }

    private fun Int?.isText(): Boolean = when (this) {
        Constants.MSG, Constants.MSG_RECEIVER_DLT, Constants.MSG_SENDER_DLT -> true
        else -> false
    }

    private fun String?.safeSub(start: Int, end: Int): String {
        val s = this ?: return ""
        if (start < 0 || end <= start || start >= s.length) return ""
        val e = end.coerceAtMost(s.length)
        return try { s.substring(start, e) } catch (_: Exception) { "" }
    }

    // ==== Diff ====

    companion object {
        const val MSG_TYPE_LEFT  = 0
        const val MSG_TYPE_RIGHT = 1
        const val MSG_TYPE_MID   = 2

        val DIFF = object : DiffUtil.ItemCallback<ChatsGroup>() {
            override fun areItemsTheSame(oldItem: ChatsGroup, newItem: ChatsGroup): Boolean {
                // Ajustá si tenés ID único; acá usamos combinación estable
                return oldItem.dateTime == newItem.dateTime &&
                        oldItem.id == newItem.id &&
                        oldItem.typeMsg == newItem.typeMsg &&
                        oldItem.message == newItem.message
            }

            override fun areContentsTheSame(oldItem: ChatsGroup, newItem: ChatsGroup): Boolean =
                oldItem == newItem
        }
    }
}
