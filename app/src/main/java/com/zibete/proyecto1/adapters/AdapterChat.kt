package com.zibete.proyecto1.adapters

import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.databinding.RowDateChatBinding
import com.zibete.proyecto1.databinding.RowMsgLeftBinding
import com.zibete.proyecto1.databinding.RowMsgRightBinding
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.ui.constants.Constants.FRAGMENT_ID_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.INFO
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_LEFT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_MID
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_RIGHT
import com.zibete.proyecto1.utils.Utils.today
import com.zibete.proyecto1.utils.Utils.yesterday
import com.zibete.proyecto1.utils.ZibeApp.ScreenUtils.widthPx
import java.io.IOException


class AdapterChat(
    private val maxSize: Int,
    private val context: Context,
    private val hasSelection: () -> Boolean,
    private val isSelected: (ChatMessage) -> Boolean,
    private val onSelectionChanged: (ChatMessage, Boolean) -> Unit,
    private val myAudioAvatarUrl: String?,
    private val otherAudioAvatarUrl: String?,
    private val myUid: String
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF), View.OnCreateContextMenuListener {

    private val photoList = arrayListOf<String>()
    private var positionForContext = 0
    private var handler: Handler? = null
    private var moveSeekBarThread: Runnable? = null
    private var mediaSelectedMs: Long = 0
    private var chronStateSave: Long = 0

    private val vibrator: Vibrator? =
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        setHasStableIds(false)
    }

    override fun submitList(list: List<ChatMessage>?) {
        super.submitList(list?.takeLast(maxSize))
        rebuildPhotoList()
    }

    private fun rebuildPhotoList() {
        photoList.clear()
        currentList.forEach { if (it.type.isPhoto()) photoList.add(it.message) }
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.type == INFO -> MSG_TYPE_MID
            item.sender == myUid -> MSG_TYPE_RIGHT
            else -> MSG_TYPE_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return when (viewType) {
            MSG_TYPE_MID -> InfoVH(RowDateChatBinding.inflate(inf, parent, false)).also {
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
        val msg = getItem(position)
        when (holder) {
            is InfoVH -> holder.bind(msg)
            is RightVH -> holder.bindCommon(msg) { h -> bindInteractive(h, msg, isMe = true) }
            is LeftVH -> holder.bindCommon(msg) { h -> bindInteractive(h, msg, isMe = false) }
        }
    }

    private fun bindInteractive(h: BaseMsgVH, chatMessage: ChatMessage, isMe: Boolean) {
        val selectedColor = ContextCompat.getColor(context, R.color.accent_transparent)
        val transparent = ContextCompat.getColor(context, R.color.transparent)
        val position = photoList.indexOf(chatMessage.message)

        val selected = isSelected(chatMessage)
        h.bindingRoot.selectedItem.setBackgroundColor(if (selected) selectedColor else transparent)

        // Click foto
        h.bindingRoot.imgPic?.setOnClickListener { v ->
            if (!hasSelection()) {
                PhotoViewerActivity.start(v.context, photoList, position)
            } else {
                onClickToggleSelect(h, chatMessage)
            }
        }

        // Click burbuja
        h.bindingRoot.linearCardMsg.setOnClickListener {
            onClickToggleSelect(h, chatMessage)
        }

        // Long click (entra/continúa selección)
        val longClick: (View) -> Boolean = {
            vibrateShort()

            if (!hasSelection()) {
                select(h, chatMessage)
            } else {
                if (!isSelected(chatMessage)) select(h, chatMessage) else unselect(h, chatMessage)
            }

            val pos = h.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) setPosition(pos)
            false
        }

        h.bindingRoot.linearCardMsg.setOnLongClickListener(longClick)
        h.bindingRoot.imgPic?.setOnLongClickListener(longClick)

        // Seen checks (solo sender)
        if (isMe) {
            when (chatMessage.seen) {
                1 -> {
                    h.bindingRoot.checked?.visibility = View.VISIBLE
                    h.bindingRoot.checked?.setColorFilter(
                        ContextCompat.getColor(context, R.color.blanco),
                        PorterDuff.Mode.SRC_IN
                    )
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

        // Audio
        if (chatMessage.type.isAudio()) {
            h.bindingRoot.icPlayPause?.setOnClickListener {
                when (h.stateMediaPlayer) {
                    MediaState.NOT_STARTED -> playAudio(h, chatMessage)
                    MediaState.PLAY -> pauseAudio(h, chatMessage)
                    MediaState.PAUSE -> continueAudio(h)
                }
            }

            h.bindingRoot.seekBar?.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
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

    private fun onClickToggleSelect(holder: BaseMsgVH, chatMessage: ChatMessage) {
        if (!hasSelection()) return
        vibrateShort()
        if (!isSelected(chatMessage)) select(holder, chatMessage) else unselect(holder, chatMessage)
    }

    private fun select(holder: BaseMsgVH, chatMessage: ChatMessage) {
        holder.bindingRoot.selectedItem.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, R.color.accent_transparent)
        )
        onSelectionChanged(chatMessage, true)
    }

    private fun unselect(holder: BaseMsgVH, chatMessage: ChatMessage) {
        holder.bindingRoot.selectedItem.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, R.color.transparent)
        )
        onSelectionChanged(chatMessage, false)
    }

    private fun vibrateShort() {
        vibrator?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun getDate(position: Int): String {
        val fecha = getItemOrNull(position)?.date?.safeSub(0, 10) ?: return ""

        val t = today()
        val y = yesterday()

        return when (fecha) {
            t -> context.getString(R.string.today)
            y -> context.getString(R.string.yesterday)
            else -> fecha
        }
    }

    fun setPosition(position: Int) {
        positionForContext = position
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        menu.add(FRAGMENT_ID_CHATLIST, 1, positionForContext, R.string.eliminar)
    }

    // ==== ViewHolders ====

    private class InfoVH(private val b: RowDateChatBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(model: ChatMessage) {
            b.tvInfo.text = model.message.orEmpty()
        }
    }

    private abstract class BaseMsgVH(root: View) : RecyclerView.ViewHolder(root) {
        abstract val bindingRoot: CommonBindingAccessor
        var stateMediaPlayer: MediaState = MediaState.NOT_STARTED
    }

    private inner class RightVH(val b: RowMsgRightBinding) : BaseMsgVH(b.root) {
        override val bindingRoot: CommonBindingAccessor = CommonBindingAccessor.from(b)
        fun bindCommon(model: ChatMessage, attach: (BaseMsgVH) -> Unit) {
            bindModel(model)
            attach(this)
        }

        private fun bindModel(model: ChatMessage) =
            bindCommonFields(bCommon = bindingRoot, model = model, isMe = true)
    }

    private inner class LeftVH(val b: RowMsgLeftBinding) : BaseMsgVH(b.root) {
        override val bindingRoot: CommonBindingAccessor = CommonBindingAccessor.from(b)
        fun bindCommon(model: ChatMessage, attach: (BaseMsgVH) -> Unit) {
            bindModel(model)
            attach(this)
        }

        private fun bindModel(model: ChatMessage) =
            bindCommonFields(bCommon = bindingRoot, model = model, isMe = false)
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
        val linearMessageMessage: View,
        val linearMessagePhoto: View,
        val linearMessageAudio: View,
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
                linearMessageMessage = b.linearMensajeMsg,
                linearMessagePhoto = b.linearMensajePic,
                linearMessageAudio = b.linearMensajeAudio,
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
                linearMessageMessage = b.linearMensajeMsg,
                linearMessagePhoto = b.linearMensajePic,
                linearMessageAudio = b.linearMensajeAudio,
                linearBubble = b.linearBubble,
                circleImgAudio = b.circleImgAudio,
                icPlayPause = b.icPlayPause,
                tvTimer = b.tvTimer,
                seekBar = b.seekBar
            )
        }
    }

    private fun bindCommonFields(bCommon: CommonBindingAccessor, model: ChatMessage, isMe: Boolean) {
        bCommon.hora?.text = model.date.safeSub(11, 16)

        val paramsBubble = bCommon.linearBubble.layoutParams as ViewGroup.MarginLayoutParams

        if (model.type.isPhoto()) {
            if (isMe) paramsBubble.marginStart = (widthPx / 3) else paramsBubble.marginEnd =
                (widthPx / 3)
        } else {
            if (isMe) paramsBubble.marginStart = (widthPx / 6) else paramsBubble.marginEnd =
                (widthPx / 6)
        }

        when {
            model.type.isPhoto() -> {
                bCommon.linearMessagePhoto.visibility = View.VISIBLE
                bCommon.linearMessageMessage.visibility = View.GONE
                bCommon.linearMessageAudio.visibility = View.GONE
                bCommon.tvNotFound?.visibility = View.GONE
                bCommon.loadingPhoto?.visibility = View.VISIBLE

                Glide.with(context)
                    .load(model.message)
                    .listener(object : com.bumptech.glide.request.RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            bCommon.tvNotFound?.visibility = View.VISIBLE
                            bCommon.loadingPhoto?.visibility = View.GONE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
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
                bCommon.linearMessagePhoto.visibility = View.GONE
                bCommon.linearMessageMessage.visibility = View.VISIBLE
                bCommon.linearMessageAudio.visibility = View.GONE
                bCommon.tvMsg?.text = model.message.orEmpty()
            }

            model.type.isAudio() -> {
                bCommon.linearMessagePhoto.visibility = View.GONE
                bCommon.linearMessageMessage.visibility = View.GONE
                bCommon.linearMessageAudio.visibility = View.VISIBLE

                val url = if (isMe) myAudioAvatarUrl else otherAudioAvatarUrl
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
    private fun playAudio(h: BaseMsgVH, chatMessage: ChatMessage) {
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
                setDataSource(chatMessage.message.orEmpty())
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

    private fun pauseAudio(h: BaseMsgVH, chatMessage: ChatMessage) {
        h.stateMediaPlayer = MediaState.PAUSE
        mediaSelectedMs = 0
        chronStateSave = SystemClock.elapsedRealtime()

        h.bindingRoot.tvTimer?.stop()
        h.bindingRoot.tvTimer?.text = chatMessage.date.safeSub(23, 28)
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
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // ==== Utils ====
    private fun Int?.isPhoto(): Boolean = when (this) {
        MSG_PHOTO, MSG_PHOTO_RECEIVER_DLT, MSG_PHOTO_SENDER_DLT -> true
        else -> false
    }

    private fun Int?.isText(): Boolean = when (this) {
        MSG_TEXT, MSG_TEXT_RECEIVER_DLT, MSG_TEXT_SENDER_DLT -> true
        else -> false
    }

    private fun Int?.isAudio(): Boolean = when (this) {
        MSG_AUDIO, MSG_AUDIO_RECEIVER_DLT, MSG_AUDIO_SENDER_DLT -> true
        else -> false
    }

    private fun String?.safeSub(start: Int, end: Int): String {
        val s = this ?: return ""
        if (start < 0 || end <= start || start >= s.length) return ""
        val e = end.coerceAtMost(s.length)
        return try {
            s.substring(start, e)
        } catch (_: Exception) {
            ""
        }
    }

    private fun getItemOrNull(position: Int): ChatMessage? =
        if (position in 0 until itemCount) getItem(position) else null

    override fun getItemId(position: Int): Long = super.getItemId(position)

    companion object {
        @JvmField
        var mediaPlayer: MediaPlayer? = null

        val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
                return oldItem === newItem ||
                        (oldItem.date == newItem.date &&
                                oldItem.sender == newItem.sender &&
                                oldItem.type == newItem.type &&
                                oldItem.message == newItem.message)
            }

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem == newItem
        }
    }

    private enum class MediaState { NOT_STARTED, PLAY, PAUSE }
}
