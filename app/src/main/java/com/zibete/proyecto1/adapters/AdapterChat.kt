package com.zibete.proyecto1.adapters

import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zibete.proyecto1.R
import com.zibete.proyecto1.databinding.RowDateChatBinding
import com.zibete.proyecto1.databinding.RowMsgLeftBinding
import com.zibete.proyecto1.databinding.RowMsgRightBinding
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.ui.constants.Constants.FRAGMENT_ID_CHATLIST
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_AUDIO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_INFO
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT_SENDER_DLT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_LEFT
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_MID
import com.zibete.proyecto1.ui.constants.Constants.MSG_TYPE_RIGHT
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.utils.TimeUtils
import com.zibete.proyecto1.utils.TimeUtils.formatHour
import com.zibete.proyecto1.utils.ZibeApp.ScreenUtils.widthPx
import de.hdodenhof.circleimageview.CircleImageView
import java.io.IOException

class AdapterChat(
    private val maxSize: Int,
    private val context: Context,
    private val hasSelection: () -> Boolean,
    private val isSelected: (ChatMessageItem) -> Boolean,
    private val onSelectionChanged: (ChatMessageItem, Boolean) -> Unit,
    private val myAudioAvatarUrl: String?,
    private val otherAudioAvatarUrl: String?,
    private val myUid: String
) : ListAdapter<ChatMessageItem, RecyclerView.ViewHolder>(DIFF), View.OnCreateContextMenuListener {

    private val photoList = arrayListOf<String>() // URLs
    private var positionForContext = 0

    private val vibrator: Vibrator? =
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    // audio runtime
    private var handler: Handler? = null
    private var moveSeekBarThread: Runnable? = null
    private var mediaSelectedMs: Long = 0
    private var chronStateSave: Long = 0

    override fun submitList(list: List<ChatMessageItem>?) {
        super.submitList(list?.takeLast(maxSize))
        rebuildPhotoList()
    }

    private fun rebuildPhotoList() {
        photoList.clear()
        currentList.forEach { item ->
            val msg = item.message
            if (msg.type.isPhoto()) photoList.add(msg.content.orEmpty())
        }
    }

    override fun getItemViewType(position: Int): Int {
        val msg = getItem(position).message
        return when {
            msg.type == MSG_INFO -> MSG_TYPE_MID
            msg.senderUid == myUid -> MSG_TYPE_RIGHT
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
        val item = getItem(position)
        when (holder) {
            is InfoVH -> holder.bind(item.message)
            is RightVH -> holder.bind(item)
            is LeftVH -> holder.bind(item)
        }
    }

    // region ContextMenu
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
    // endregion

    // region ViewHolders
    private class InfoVH(private val b: RowDateChatBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: ChatMessage) {
            b.tvInfo.text = msg.content
        }
    }

    private inner class RightVH(private val b: RowMsgRightBinding) :
        RecyclerView.ViewHolder(b.root) {
        private var mediaState: MediaState = MediaState.NOT_STARTED

        fun bind(item: ChatMessageItem) {
            val msg = item.message
            bindBubbleMargins(isMe = true, isPhoto = msg.type.isPhoto(), bubble = b.linearBubble)

            // base visibility
            b.linearMensajeMsg.isVisible = msg.type.isText()
            b.linearMensajePic.isVisible = msg.type.isPhoto()
            b.linearMensajeAudio.isVisible = msg.type.isAudio()

            // selection bg
            b.selectedItem.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (isSelected(item)) R.color.accent_transparent else R.color.transparent
                )
            )

            // time
            b.horaMsg.text = formatHour(msg.createdAt)

            // click + long click
            wireSelection(
                item = item,
                bubble = b.linearCardMsg,
                photo = b.imgPic
            )

            // seen (solo sender)
            renderSeenChecksRight(b, msg.seen)

            // content render
            when {
                msg.type.isText() -> {
                    b.tvMsg.text = msg.content
                }

                msg.type.isPhoto() -> {
                    renderPhoto(
                        image = b.imgPic,
                        loading = b.loadingPhoto,
                        notFound = b.tvNotFound,
                        url = msg.content.orEmpty()
                    )
                }

                msg.type.isAudio() -> {
                    renderAudio(
                        isMe = true,
                        avatarUrl = myAudioAvatarUrl,
                        msg = msg,
                        icPlayPause = b.icPlayPause,
                        seekBar = b.seekBar,
                        tvTimer = b.tvTimer,
                        circle = b.circleImgAudio,
                        getState = { mediaState },
                        setState = { mediaState = it }
                    )
                }
            }
        }
    }

    private inner class LeftVH(private val b: RowMsgLeftBinding) : RecyclerView.ViewHolder(b.root) {
        private var mediaState: MediaState = MediaState.NOT_STARTED

        fun bind(item: ChatMessageItem) {
            val msg = item.message
            bindBubbleMargins(isMe = false, isPhoto = msg.type.isPhoto(), bubble = b.linearBubble)

            b.linearMensajeMsg.isVisible = msg.type.isText()
            b.linearMensajePic.isVisible = msg.type.isPhoto()
            b.linearMensajeAudio.isVisible = msg.type.isAudio()

            b.selectedItem.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (isSelected(item)) R.color.accent_transparent else R.color.transparent
                )
            )

            b.horaMsg.text = formatHour(msg.createdAt)

            wireSelection(
                item = item,
                bubble = b.linearCardMsg,
                photo = b.imgPic
            )

            // en left no mostramos checks
            b.checked.isVisible = false
            b.checked2.isVisible = false

            when {
                msg.type.isText() -> {
                    b.tvMsg.text = msg.content.orEmpty()
                }

                msg.type.isPhoto() -> {
                    renderPhoto(
                        image = b.imgPic,
                        loading = b.loadingPhoto,
                        notFound = b.tvNotFound,
                        url = msg.content.orEmpty()
                    )
                }

                msg.type.isAudio() -> {
                    renderAudio(
                        isMe = false,
                        avatarUrl = otherAudioAvatarUrl,
                        msg = msg,
                        icPlayPause = b.icPlayPause,
                        seekBar = b.seekBar,
                        tvTimer = b.tvTimer,
                        circle = b.circleImgAudio,
                        getState = { mediaState },
                        setState = { mediaState = it }
                    )
                }
            }
        }
    }
    // endregion

    // region Binding helpers
    private fun bindBubbleMargins(isMe: Boolean, isPhoto: Boolean, bubble: View) {
        val params = bubble.layoutParams as ViewGroup.MarginLayoutParams
        val big = (widthPx / 3)
        val small = (widthPx / 6)

        if (isPhoto) {
            if (isMe) {
                params.marginStart = big; params.marginEnd = 0
            } else {
                params.marginEnd = big; params.marginStart = 0
            }
        } else {
            if (isMe) {
                params.marginStart = small; params.marginEnd = 0
            } else {
                params.marginEnd = small; params.marginStart = 0
            }
        }
        bubble.layoutParams = params
    }

    private fun wireSelection(
        item: ChatMessageItem,
        bubble: View,
        photo: View?
    ) {
        bubble.setOnClickListener {
            if (!hasSelection()) return@setOnClickListener
            toggleSelect(item)
        }

        val longClick: (View) -> Boolean = {
            vibrateShort()
            val pos = (it.parent as? View)?.let { _ -> RecyclerView.NO_POSITION } // no confiamos
            // mejor: usa position en onBind si necesitás, acá solo selección
            if (!hasSelection()) {
                select(item)
            } else {
                if (!isSelected(item)) select(item) else unselect(item)
            }
            false
        }

        bubble.setOnLongClickListener(longClick)
        photo?.setOnLongClickListener(longClick)

        // Click foto
        photo?.setOnClickListener { v ->
            val url = item.message.content.orEmpty()
            val photoPos = photoList.indexOf(url)
            if (!hasSelection()) {
                PhotoViewerActivity.start(v.context, photoList, photoPos)
            } else {
                toggleSelect(item)
            }
        }
    }

    private fun toggleSelect(item: ChatMessageItem) {
        vibrateShort()
        if (!isSelected(item)) select(item) else unselect(item)
    }

    private fun select(item: ChatMessageItem) {
        onSelectionChanged(item, true)
    }

    private fun unselect(item: ChatMessageItem) {
        onSelectionChanged(item, false)
    }

    private fun vibrateShort() {
        vibrator?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun renderSeenChecksRight(b: RowMsgRightBinding, seen: Int) {
        when (seen) {
            1 -> {
                b.checked.isVisible = true
                b.checked2.isVisible = false
                b.checked.setColorFilter(
                    ContextCompat.getColor(context, R.color.blanco),
                    PorterDuff.Mode.SRC_IN
                )
            }

            2 -> {
                b.checked.isVisible = true
                b.checked2.isVisible = true
                val c = ContextCompat.getColor(context, R.color.blanco)
                b.checked.setColorFilter(c, PorterDuff.Mode.SRC_IN)
                b.checked2.setColorFilter(c, PorterDuff.Mode.SRC_IN)
            }

            3 -> {
                b.checked.isVisible = true
                b.checked2.isVisible = true
                val c = ContextCompat.getColor(context, R.color.visto)
                b.checked.setColorFilter(c, PorterDuff.Mode.SRC_IN)
                b.checked2.setColorFilter(c, PorterDuff.Mode.SRC_IN)
            }

            else -> {
                b.checked.isVisible = false
                b.checked2.isVisible = false
            }
        }
    }

    private fun renderPhoto(image: ImageView, loading: View, notFound: View, url: String) {
        notFound.isVisible = false
        loading.isVisible = true

        Glide.with(context)
            .load(url)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    notFound.isVisible = true
                    loading.isVisible = false
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    loading.isVisible = false
                    return false
                }
            })
            .into(image)
        image.contentDescription = context.getString(R.string.photo_message)
    }

    private fun renderAudio(
        isMe: Boolean,
        avatarUrl: String?,
        msg: ChatMessage,
        icPlayPause: ImageView,
        seekBar: SeekBar,
        tvTimer: Chronometer,
        circle: CircleImageView,
        getState: () -> MediaState,
        setState: (MediaState) -> Unit
    ) {
        Glide.with(context).load(avatarUrl).into(circle)

        tvTimer.text = TimeUtils.formatAudioDuration(msg.audioDurationMs)

        seekBar.progress = 0
        seekBar.max = 0
        icPlayPause.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_baseline_play_arrow_24
            )
        )

        icPlayPause.setOnClickListener {
            when (getState()) {
                MediaState.NOT_STARTED -> playAudio(
                    msg.content.orEmpty(),
                    icPlayPause,
                    seekBar,
                    tvTimer,
                    setState
                )

                MediaState.PLAY -> pauseAudio(icPlayPause, seekBar, tvTimer, msg, setState)
                MediaState.PAUSE -> continueAudio(icPlayPause, tvTimer, setState)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    mediaSelectedMs = progress.toLong()
                    mediaPlayer?.seekTo(progress)
                    tvTimer.base = SystemClock.elapsedRealtime() - progress
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
    // endregion

    // region Audio engine (global)
    private fun playAudio(
        url: String,
        icPlayPause: ImageView,
        seekBar: SeekBar,
        tvTimer: Chronometer,
        setState: (MediaState) -> Unit
    ) {
        mediaPlayer?.let { onCompletion(icPlayPause, seekBar, tvTimer, setState) }

        setState(MediaState.PLAY)
        handler = Handler(context.mainLooper)

        moveSeekBarThread = object : Runnable {
            override fun run() {
                mediaPlayer?.let { mp ->
                    seekBar.max = mp.duration
                    seekBar.progress = mp.currentPosition
                    handler?.postDelayed(this, 16)
                }
            }
        }

        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { onCompletion(icPlayPause, seekBar, tvTimer, setState) }
            setOnPreparedListener {
                icPlayPause.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_baseline_pause_24
                    )
                )
                seekTo(mediaSelectedMs.toInt())
                start()
                tvTimer.base = SystemClock.elapsedRealtime() - mediaSelectedMs
                tvTimer.start()
                moveSeekBarThread?.run()
            }

            try {
                setDataSource(url)
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
                onCompletion(icPlayPause, seekBar, tvTimer, setState)
            }
        }
    }

    private fun continueAudio(
        icPlayPause: ImageView,
        tvTimer: Chronometer,
        setState: (MediaState) -> Unit
    ) {
        setState(MediaState.PLAY)
        if (mediaSelectedMs != 0L) {
            tvTimer.base = SystemClock.elapsedRealtime() - mediaSelectedMs
        } else {
            val intervalOnPause = (SystemClock.elapsedRealtime() - chronStateSave)
            tvTimer.base = tvTimer.base + intervalOnPause - mediaSelectedMs
        }
        tvTimer.start()
        mediaPlayer?.start()
        icPlayPause.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_baseline_pause_24
            )
        )
    }

    private fun pauseAudio(
        icPlayPause: ImageView,
        seekBar: SeekBar,
        tvTimer: Chronometer,
        msg: ChatMessage,
        setState: (MediaState) -> Unit
    ) {
        setState(MediaState.PAUSE)
        mediaSelectedMs = 0
        chronStateSave = SystemClock.elapsedRealtime()

        tvTimer.stop()
        tvTimer.text = TimeUtils.formatAudioDuration(msg.audioDurationMs)
        mediaPlayer?.pause()
        seekBar.progress = mediaPlayer?.currentPosition ?: 0

        icPlayPause.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_baseline_play_arrow_24
            )
        )
    }

    private fun onCompletion(
        icPlayPause: ImageView,
        seekBar: SeekBar,
        tvTimer: Chronometer,
        setState: (MediaState) -> Unit
    ) {
        setState(MediaState.NOT_STARTED)
        icPlayPause.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.ic_baseline_play_arrow_24
            )
        )
        tvTimer.stop()

        handler?.removeCallbacks(moveSeekBarThread ?: return)
        seekBar.setOnSeekBarChangeListener(null)
        seekBar.progress = 0

        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null

        mediaSelectedMs = 0
    }
    // endregion

    // region Public helpers
    fun getDateChat(position: Int): String {
        val millis = getItemOrNull(position)?.message?.createdAt ?: return ""

        return TimeUtils.formatDateChatTimestamp(millis, context)
    }

    // endregion

    // region Utils
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

    private fun getItemOrNull(position: Int): ChatMessageItem? =
        if (position in 0 until itemCount) getItem(position) else null
    // endregion

    private enum class MediaState { NOT_STARTED, PLAY, PAUSE }

    companion object {
        @JvmField
        var mediaPlayer: MediaPlayer? = null

        val DIFF = object : DiffUtil.ItemCallback<ChatMessageItem>() {
            override fun areItemsTheSame(
                oldItem: ChatMessageItem,
                newItem: ChatMessageItem
            ): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ChatMessageItem,
                newItem: ChatMessageItem
            ): Boolean =
                oldItem.message == newItem.message
        }
    }
}
