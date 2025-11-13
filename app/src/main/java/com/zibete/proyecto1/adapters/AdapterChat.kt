package com.zibete.proyecto1.adapters

import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.os.Handler
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.zibete.proyecto1.ChatActivity
import com.zibete.proyecto1.utils.Constants
import com.zibete.proyecto1.R
import com.zibete.proyecto1.SlidePhotoActivity
import com.zibete.proyecto1.databinding.RowDateChatBinding
import com.zibete.proyecto1.databinding.RowMsgLeftBinding
import com.zibete.proyecto1.databinding.RowMsgRightBinding
import com.zibete.proyecto1.model.Chats
import com.zibete.proyecto1.utils.ZibeApp.ScreenUtils.heightPx
import com.zibete.proyecto1.utils.ZibeApp.ScreenUtils.widthPx
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdapterChat(
    msgList: ArrayList<Chats>,
    private val maxSize: Int,
    private val context: Context
) : ListAdapter<Chats, RecyclerView.ViewHolder>(DIFF),
    View.OnCreateContextMenuListener {

    private val mutableMsgList = msgList.toMutableList()
    private val photoList = arrayListOf<String?>()
    private var positionForContext = 0
    private var handler: Handler? = null
    private var moveSeekBarThread: Runnable? = null
    private var mediaSelectedMs: Long = 0
    private var chronStateSave: Long = 0
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val userId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        msgList.forEach { if (it.type.isPhoto()) photoList.add(it.message) }
        submitList(mutableMsgList.toList())
        setHasStableIds(false)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.type == Constants.INFO -> TYPE_INFO
            item.sender == userId        -> MSG_TYPE_RIGHT
            else                         -> MSG_TYPE_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_INFO -> InfoVH(RowDateChatBinding.inflate(inf, parent, false)).also {
                it.itemView.setOnCreateContextMenuListener(this)
            }
            MSG_TYPE_RIGHT -> RightVH(RowMsgRightBinding.inflate(inf, parent, false)).also {
                it.itemView.setOnCreateContextMenuListener(this)
            }
            else -> LeftVH(RowMsgLeftBinding.inflate(inf, parent, false)).also {
                it.itemView.setOnCreateContextMenuListener(this)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chats = getItem(position)
        when (holder) {
            is InfoVH  -> holder.bind(chats)
            is RightVH -> holder.bindCommon(chats) { h -> bindInteractive(h, chats) }
            is LeftVH  -> holder.bindCommon(chats) { h -> bindInteractive(h, chats) }
        }
    }

    private fun bindInteractive(h: BaseMsgVH, chats: Chats) {
        val selectedColor = ContextCompat.getColor(context, R.color.accent_transparent)
        val transparent = ContextCompat.getColor(context, R.color.transparent)
        val isSelected = ChatActivity.msgSelected.indexOf(chats) != -1
        h.bindingRoot.selectedItem.setBackgroundColor(if (isSelected) selectedColor else transparent)

        h.bindingRoot.imgPic?.setOnClickListener { v ->
            if (ChatActivity.msgSelected.isEmpty()) {
                val i = Intent(context, SlidePhotoActivity::class.java)
                    .putExtra("photoList", photoList)
                    .putExtra("position", photoList.indexOf(chats.message))
                    .putExtra("rotation", 0)
                v.context.startActivity(i)
            } else {
                onClickToggleSelect(h, chats)
            }
        }

        h.bindingRoot.linearCardMsg.setOnClickListener {
            onClickToggleSelect(h, chats)
        }

        val longClick: (View) -> Boolean = {
            vibrateShort()
            if (ChatActivity.msgSelected.isEmpty()) {
                select(h, chats)
            } else {
                val idx = ChatActivity.msgSelected.indexOf(chats)
                if (idx == -1) select(h, chats) else unselect(h, chats)
            }
            val pos = h.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) setPosition(pos)
            false
        }
        h.bindingRoot.linearCardMsg.setOnLongClickListener(longClick)
        h.bindingRoot.imgPic?.setOnLongClickListener(longClick)

        if (chats.sender == userId) {
            when (chats.seen) {
                1 -> {
                    h.bindingRoot.checked?.visibility = View.VISIBLE
                    h.bindingRoot.checked?.setColorFilter(ContextCompat.getColor(context, R.color.blanco), PorterDuff.Mode.SRC_IN)
                    h.bindingRoot.checked2?.visibility = View.GONE
                }
                2 -> {
                    h.bindingRoot.checked?.visibility = View.VISIBLE
                    h.bindingRoot.checked2?.visibility = View.VISIBLE
                    val c = ContextCompat.getColor(context, R.color.blanco)
                    h.bindingRoot.checked?.setColorFilter(c, PorterDuff.Mode.SRC_IN)
                    h.bindingRoot.checked2?.setColorFilter(c, PorterDuff.Mode.SRC_IN)
                }
                3 -> {
                    h.bindingRoot.checked?.visibility = View.VISIBLE
                    h.bindingRoot.checked2?.visibility = View.VISIBLE
                    val c = ContextCompat.getColor(context, R.color.visto)
                    h.bindingRoot.checked?.setColorFilter(c, PorterDuff.Mode.SRC_IN)
                    h.bindingRoot.checked2?.setColorFilter(c, PorterDuff.Mode.SRC_IN)
                }
                else -> {
                    h.bindingRoot.checked?.visibility = View.GONE
                    h.bindingRoot.checked2?.visibility = View.GONE
                }
            }
        } else {
            h.bindingRoot.checked?.visibility = View.GONE
            h.bindingRoot.checked2?.visibility = View.GONE
        }

        if (chats.type.isAudio()) {
            h.bindingRoot.icPlayPause?.setOnClickListener {
                when (h.stateMediaPlayer) {
                    MediaState.NOT_STARTED -> playAudio(h, chats)
                    MediaState.PLAY        -> pauseAudio(h, chats)
                    MediaState.PAUSE       -> continueAudio(h)
                }
            }

            h.bindingRoot.seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && mediaPlayer != null) {
                        mediaSelectedMs = progress.toLong()
                        mediaPlayer?.seekTo(progress)
                        h.bindingRoot.tvTimer?.base = SystemClock.elapsedRealtime() - progress
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
    }

    private fun onClickToggleSelect(holder: BaseMsgVH, chats: Chats) {
        if (ChatActivity.msgSelected.isEmpty()) return
        vibrateShort()
        val idx = ChatActivity.msgSelected.indexOf(chats)
        if (idx == -1) select(holder, chats) else unselect(holder, chats)
    }

    private fun select(holder: BaseMsgVH, chats: Chats) {
        holder.bindingRoot.selectedItem.setBackgroundColor(
            ContextCompat.getColor(context, R.color.accent_transparent)
        )
        ChatActivity.selectedDeleteMsg(chats)
    }

    private fun unselect(holder: BaseMsgVH, chats: Chats) {
        holder.bindingRoot.selectedItem.setBackgroundColor(
            ContextCompat.getColor(context, R.color.transparent)
        )
        ChatActivity.notSelectedDeleteMsg(chats)
    }

    private fun vibrateShort() {
        vibrator?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ==== API pública compatible ====

    fun addChat(chats: Chats) {
        if (mutableMsgList.size > maxSize) {
            mutableMsgList.removeAt(0)
        }

        // ------- separador de fecha (FIX: "ayer" no muta el calendar de "hoy")
        if (mutableMsgList.isNotEmpty()) {
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

            val last = mutableMsgList.last()
            val thisDate = chats.date.safeSub(0, 10)
            val lastDate = last.date.safeSub(0, 10)

            if (thisDate != lastDate) {
                when (thisDate) {
                    dateFormat.format(today.time) -> mutableMsgList.add(
                        Chats(
                            context.getString(R.string.today),
                            thisDate,
                            "",
                            Constants.INFO,
                            0))
                    dateFormat.format(yesterday.time) -> mutableMsgList.add(
                        Chats(
                            context.getString(R.string.yesterday),
                            thisDate,
                            "",
                            Constants.INFO,
                            0))
                    else -> mutableMsgList.add(
                        Chats(
                            thisDate,
                            thisDate,
                            "",
                            Constants.INFO,
                            0))
                }
            }
        }

        mutableMsgList.add(chats)
        if (chats.type.isPhoto()) photoList.add(chats.message)
        submitList(mutableMsgList.toList())
    }

    fun actualizeMsg(chats: Chats) {
        val idx = mutableMsgList.indexOf(chats)
        if (idx != -1) {
            mutableMsgList[idx] = chats
            val iAmSender = chats.sender == userId
            val deleteType = if (iAmSender)
                listOf(Constants.MSG_SENDER_DLT, Constants.PHOTO_SENDER_DLT, Constants.AUDIO_SENDER_DLT)
            else
                listOf(Constants.MSG_RECEIVER_DLT, Constants.PHOTO_RECEIVER_DLT, Constants.AUDIO_RECEIVER_DLT)

            if (deleteType.contains(chats.type)) {
                mutableMsgList.removeAt(idx)
            }
            submitList(mutableMsgList.toList())
        } else {
            addChat(chats)
        }
    }

    fun deleteMsg(chats: Chats?) {
        chats ?: return
        val idx = mutableMsgList.indexOf(chats)
        if (idx != -1) {
            mutableMsgList.removeAt(idx)
            submitList(mutableMsgList.toList())
        }
    }

    fun getDate(position: Int): String {
        val today = Calendar.getInstance()
        val ayer = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
        val fecha = getItemOrNull(position)?.date?.safeSub(0, 10) ?: ""
        return when (fecha) {
            dateFormat.format(today.time) -> context.getString(R.string.today)
            dateFormat.format(ayer.time)  -> context.getString(R.string.yesterday)
            else                          -> fecha
        }
    }

    fun setPosition(position: Int) {
        positionForContext = position
    }

    override fun getItemCount(): Int = super.getItemCount()

    override fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menu.add(Constants.FRAGMENT_ID_CHATLIST, 1, positionForContext, R.string.eliminar)
    }

    // ==== ViewHolders ====

    private class InfoVH(private val b: RowDateChatBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(model: Chats) {
            b.tvInfo.text = model.message.orEmpty()
        }
    }

    private abstract class BaseMsgVH(root: View) : RecyclerView.ViewHolder(root) {
        abstract val bindingRoot: CommonBindingAccessor
        var stateMediaPlayer: MediaState = MediaState.NOT_STARTED
    }

    private inner class RightVH(val b: RowMsgRightBinding) : BaseMsgVH(b.root) {
        override val bindingRoot: CommonBindingAccessor = CommonBindingAccessor.from(b)
        fun bindCommon(model: Chats, attach: (BaseMsgVH) -> Unit) {
            bindModel(model)
            attach(this)
        }
        private fun bindModel(model: Chats) = bindCommonFields(bCommon = bindingRoot, model = model, isMe = true)
    }

    private inner class LeftVH(val b: RowMsgLeftBinding) : BaseMsgVH(b.root) {
        override val bindingRoot: CommonBindingAccessor = CommonBindingAccessor.from(b)
        fun bindCommon(model: Chats, attach: (BaseMsgVH) -> Unit) {
            bindModel(model)
            attach(this)
        }
        private fun bindModel(model: Chats) = bindCommonFields(bCommon = bindingRoot, model = model, isMe = false)
    }

    private data class CommonBindingAccessor(
        val root: View,
        val linearCardMsg: View,
        val selectedItem: View,
        val imgPic: androidx.appcompat.widget.AppCompatImageView?,
        val loadingPhoto: View?,
        val tvNotFound: View?,
        val tvMsg: TextView?,
        val hora: TextView?,
        val checked: ImageView?,
        val checked2: ImageView?,
        val linearMensajeMsg: View,
        val linearMensajePic: View,
        val linearMensajeAudio: View,
        val linearBubble: View,
        val circleImgAudio: de.hdodenhof.circleimageview.CircleImageView?,
        val icPlayPause: ImageView?,
        val tvTimer: Chronometer?,
        val seekBar: SeekBar?
    ) {
        companion object {
            fun from(b: RowMsgRightBinding): CommonBindingAccessor = CommonBindingAccessor(
                root = b.root,
                linearCardMsg = b.linearCardMsg,
                selectedItem = b.selectedItem,
                imgPic = b.imgPic,
                loadingPhoto = b.loadingPhoto,
                tvNotFound = b.tvNotFound,
                tvMsg = b.tvMsg,
                hora = b.horaMsg,
                checked = b.checked,
                checked2 = b.checked2,
                linearMensajeMsg = b.linearMensajeMsg,
                linearMensajePic = b.linearMensajePic,
                linearMensajeAudio = b.linearMensajeAudio,
                linearBubble = b.linearBubble,
                circleImgAudio = b.circleImgAudio,
                icPlayPause = b.icPlayPause,
                tvTimer = b.tvTimer,
                seekBar = b.seekBar
            )
            fun from(b: RowMsgLeftBinding): CommonBindingAccessor = CommonBindingAccessor(
                root = b.root,
                linearCardMsg = b.linearCardMsg,
                selectedItem = b.selectedItem,
                imgPic = b.imgPic,
                loadingPhoto = b.loadingPhoto,
                tvNotFound = b.tvNotFound,
                tvMsg = b.tvMsg,
                hora = b.horaMsg,
                checked = b.checked,
                checked2 = b.checked2,
                linearMensajeMsg = b.linearMensajeMsg,
                linearMensajePic = b.linearMensajePic,
                linearMensajeAudio = b.linearMensajeAudio,
                linearBubble = b.linearBubble,
                circleImgAudio = b.circleImgAudio,
                icPlayPause = b.icPlayPause,
                tvTimer = b.tvTimer,
                seekBar = b.seekBar
            )
        }
    }

    // Lógica común
    private fun bindCommonFields(bCommon: CommonBindingAccessor, model: Chats, isMe: Boolean) {
        bCommon.hora?.text = model.date.safeSub(11, 16)

        val paramsBubble = bCommon.linearBubble.layoutParams as ViewGroup.MarginLayoutParams

        if (model.type.isPhoto()) {
            if (isMe) paramsBubble.marginStart = (widthPx / 3) else paramsBubble.marginEnd = (widthPx / 3)
        }else{
            if (isMe) paramsBubble.marginStart = (widthPx / 6) else paramsBubble.marginEnd = (widthPx / 6)
        }
        when {
            model.type.isPhoto() -> {
                bCommon.linearMensajePic.visibility = View.VISIBLE
                bCommon.linearMensajeMsg.visibility = View.GONE
                bCommon.linearMensajeAudio.visibility = View.GONE
                bCommon.tvNotFound?.visibility = View.GONE
                bCommon.loadingPhoto?.visibility = View.VISIBLE

                Glide.with(context)
                    .load(model.message)
                    .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(
                            e: com.bumptech.glide.load.engine.GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            bCommon.tvNotFound?.visibility = View.VISIBLE
                            bCommon.loadingPhoto?.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: android.graphics.drawable.Drawable?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                            dataSource: com.bumptech.glide.load.DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            bCommon.loadingPhoto?.visibility = View.GONE
                            return false
                        }
                    })
                    .into(bCommon.imgPic!!)
                bCommon.imgPic.contentDescription = context.getString(R.string.photo_message)
            }

            model.type.isText() -> {
                bCommon.linearMensajePic.visibility = View.GONE
                bCommon.linearMensajeMsg.visibility = View.VISIBLE
                bCommon.linearMensajeAudio.visibility = View.GONE
                bCommon.tvMsg?.text = model.message.orEmpty()
            }

            model.type.isAudio() -> {
                bCommon.linearMensajePic.visibility = View.GONE
                bCommon.linearMensajeMsg.visibility = View.GONE
                bCommon.linearMensajeAudio.visibility = View.VISIBLE

                val url = if (isMe) ChatActivity.myPhoto else ChatActivity.yourPhoto
                Glide.with(context).load(url).into(bCommon.circleImgAudio!!)

                bCommon.tvTimer?.text = model.date.safeSub(23, 28)

                bCommon.seekBar?.progress = 0
                bCommon.seekBar?.max = 0

                bCommon.icPlayPause?.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_baseline_play_arrow_24)
                )
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is BaseMsgVH && holder.stateMediaPlayer != MediaState.NOT_STARTED) {
            stopAndReleaseMedia(holder)
        }
    }

    // ==== Media ====
    private fun playAudio(h: BaseMsgVH, chats: Chats) {
        mediaPlayer?.let { onCompletion(h) }
        h.stateMediaPlayer = MediaState.PLAY
        handler = Handler(context.mainLooper)

        moveSeekBarThread = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    val pos = mp.currentPosition
                    val max = mp.duration
                    h.bindingRoot.seekBar?.max = max
                    h.bindingRoot.seekBar?.progress = pos
                    handler?.postDelayed(this, 16)
                }
            }
        }

        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { onCompletion(h) }
            setOnPreparedListener {
                h.bindingRoot.icPlayPause?.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_baseline_pause_24)
                )
                seekTo(mediaSelectedMs.toInt())
                start()
                h.bindingRoot.tvTimer?.base = SystemClock.elapsedRealtime() - mediaSelectedMs
                h.bindingRoot.tvTimer?.start()
                moveSeekBarThread?.run()
            }
            try {
                setDataSource(chats.message.orEmpty())
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
                onCompletion(h)
            }
        }
    }

    private fun continueAudio(h: BaseMsgVH) {
        h.stateMediaPlayer = MediaState.PLAY
        if (mediaSelectedMs != 0L) {
            h.bindingRoot.tvTimer?.base = SystemClock.elapsedRealtime() - mediaSelectedMs
        } else {
            val intervalOnPause = (SystemClock.elapsedRealtime() - chronStateSave)
            val base = (h.bindingRoot.tvTimer?.base ?: SystemClock.elapsedRealtime())
            h.bindingRoot.tvTimer?.base = base + intervalOnPause - mediaSelectedMs
        }
        h.bindingRoot.tvTimer?.start()
        mediaPlayer?.start()
        h.bindingRoot.icPlayPause?.setImageDrawable(
            ContextCompat.getDrawable(context, R.drawable.ic_baseline_pause_24)
        )
    }

    private fun pauseAudio(h: BaseMsgVH, chats: Chats) {
        h.stateMediaPlayer = MediaState.PAUSE
        mediaSelectedMs = 0
        chronStateSave = SystemClock.elapsedRealtime()

        h.bindingRoot.tvTimer?.stop()
        h.bindingRoot.tvTimer?.text = chats.date.safeSub(23, 28)
        mediaPlayer?.pause()

        val pos = mediaPlayer?.currentPosition ?: 0
        h.bindingRoot.seekBar?.progress = pos

        h.bindingRoot.icPlayPause?.setImageDrawable(
            ContextCompat.getDrawable(context, R.drawable.ic_baseline_play_arrow_24)
        )
    }

    private fun onCompletion(h: BaseMsgVH) {
        h.stateMediaPlayer = MediaState.NOT_STARTED
        h.bindingRoot.icPlayPause?.setImageDrawable(
            ContextCompat.getDrawable(context, R.drawable.ic_baseline_play_arrow_24)
        )
        h.bindingRoot.tvTimer?.stop()
        handler?.removeCallbacks(moveSeekBarThread ?: return)
        h.bindingRoot.seekBar?.setOnSeekBarChangeListener(null)
        h.bindingRoot.seekBar?.progress = 0
        stopAndReleaseMedia(h)
        mediaSelectedMs = 0
    }

    private fun stopAndReleaseMedia(@Suppress("UNUSED_PARAMETER") h: BaseMsgVH) {
        try { mediaPlayer?.stop() } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
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

    private fun Int?.isAudio(): Boolean = when (this) {
        Constants.AUDIO, Constants.AUDIO_RECEIVER_DLT, Constants.AUDIO_SENDER_DLT -> true
        else -> false
    }

    private fun String?.safeSub(start: Int, end: Int): String {
        val s = this ?: return ""
        if (start < 0 || end <= start || start >= s.length) return ""
        val e = end.coerceAtMost(s.length)
        return try { s.substring(start, e) } catch (_: Exception) { "" }
    }

    private fun getItemOrNull(position: Int): Chats? =
        if (position in 0 until itemCount) getItem(position) else null

    override fun getItemId(position: Int): Long = super.getItemId(position)

    companion object {
        @JvmField
        var mediaPlayer: MediaPlayer? = null

        const val TYPE_INFO: Int = 0
        const val MSG_TYPE_LEFT: Int = 1
        const val MSG_TYPE_RIGHT: Int = 2

        val DIFF = object : DiffUtil.ItemCallback<Chats>() {
            override fun areItemsTheSame(oldItem: Chats, newItem: Chats): Boolean {
                return oldItem === newItem ||
                        (oldItem.date == newItem.date &&
                                oldItem.sender == newItem.sender &&
                                oldItem.type == newItem.type &&
                                oldItem.message == newItem.message)
            }
            override fun areContentsTheSame(oldItem: Chats, newItem: Chats): Boolean = oldItem == newItem
        }
    }

    private enum class MediaState { NOT_STARTED, PLAY, PAUSE }
}
