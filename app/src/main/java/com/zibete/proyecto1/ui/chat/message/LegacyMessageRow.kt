package com.zibete.proyecto1.ui.chat.message

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Handler
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.SeekBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_AUDIO_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO_SENDER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_RECEIVED
import com.zibete.proyecto1.core.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_RECEIVER_DLT
import com.zibete.proyecto1.core.constants.Constants.MSG_TEXT_SENDER_DLT
import com.zibete.proyecto1.core.utils.TimeUtils
import com.zibete.proyecto1.core.utils.TimeUtils.formatHour
import com.zibete.proyecto1.core.utils.ZibeApp.ScreenUtils.widthPx
import com.zibete.proyecto1.databinding.RowMsgLeftBinding
import com.zibete.proyecto1.databinding.RowMsgRightBinding
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.ui.chat.media.ChatAudioPlayer
import com.zibete.proyecto1.ui.chat.media.MediaState
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import de.hdodenhof.circleimageview.CircleImageView

@Composable
fun LegacyMessageRow(
    item: ChatMessageItem,
    isMe: Boolean,
    isSelected: Boolean,
    hasSelection: Boolean,
    myAudioAvatarUrl: String?,
    otherAudioAvatarUrl: String?,
    photoList: List<String>,
    onSelectionChanged: (ChatMessageItem, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        val label = when {
            item.message.type.isPhoto() -> "Preview photo"
            item.message.type.isAudio() -> "Preview audio"
            else -> "Preview message"
        }
        PreviewMessageRow(
            isMe = isMe,
            isSelected = isSelected,
            text = label
        )
        return
    }

    val layoutInflater = remember(context) { LayoutInflater.from(context) }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = {
            val binding = if (isMe) {
                RowMsgRightBinding.inflate(layoutInflater)
            } else {
                RowMsgLeftBinding.inflate(layoutInflater)
            }
            binding.root.tag = binding
            binding.root.setTag(R.id.tag_media_state, MediaState.NOT_STARTED)
            binding.root
        },
        update = { view ->
            val mediaState = view.getTag(R.id.tag_media_state) as? MediaState ?: MediaState.NOT_STARTED
            val audioAvatarUrl = if (isMe) myAudioAvatarUrl else otherAudioAvatarUrl

            if (isMe) {
                val binding = view.tag as RowMsgRightBinding
                bindRight(
                    binding = binding,
                    item = item,
                    isSelected = isSelected,
                    hasSelection = hasSelection,
                    photoList = photoList,
                    onSelectionChanged = onSelectionChanged,
                    audioAvatarUrl = audioAvatarUrl,
                    mediaState = mediaState,
                    onMediaStateChanged = { view.setTag(R.id.tag_media_state, it) }
                )
            } else {
                val binding = view.tag as RowMsgLeftBinding
                bindLeft(
                    binding = binding,
                    item = item,
                    isSelected = isSelected,
                    hasSelection = hasSelection,
                    photoList = photoList,
                    onSelectionChanged = onSelectionChanged,
                    audioAvatarUrl = audioAvatarUrl,
                    mediaState = mediaState,
                    onMediaStateChanged = { view.setTag(R.id.tag_media_state, it) }
                )
            }
        }
    )
}

@Composable
private fun PreviewMessageRow(
    isMe: Boolean,
    isSelected: Boolean,
    text: String
) {
    val bubbleColor = if (isMe) R.color.colorB else R.color.colorC
    val backgroundColor = if (isSelected) R.color.accent_transparent else bubbleColor
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = colorResource(backgroundColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = colorResource(R.color.blanco),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private fun bindRight(
    binding: RowMsgRightBinding,
    item: ChatMessageItem,
    isSelected: Boolean,
    hasSelection: Boolean,
    photoList: List<String>,
    onSelectionChanged: (ChatMessageItem, Boolean) -> Unit,
    audioAvatarUrl: String?,
    mediaState: MediaState,
    onMediaStateChanged: (MediaState) -> Unit
) {
    val msg = item.message
    bindBubbleMargins(isMe = true, isPhoto = msg.type.isPhoto(), bubble = binding.linearBubble)

    binding.linearMensajeMsg.isVisible = msg.type.isText()
    binding.linearMensajePic.isVisible = msg.type.isPhoto()
    binding.linearMensajeAudio.isVisible = msg.type.isAudio()

    binding.selectedItem.setBackgroundColor(
        ContextCompat.getColor(
            binding.root.context,
            if (isSelected) R.color.accent_transparent else R.color.transparent
        )
    )

    binding.horaMsg.text = formatHour(msg.createdAt)
    wireSelection(
        item = item,
        bubble = binding.linearCardMsg,
        photo = binding.imgPic,
        hasSelection = hasSelection,
        isSelected = isSelected,
        onSelectionChanged = onSelectionChanged,
        photoList = photoList
    )

    renderSeenChecksRight(binding, msg.seen)

    when {
        msg.type.isText() -> binding.tvMsg.text = msg.content
        msg.type.isPhoto() -> {
            renderPhoto(
                image = binding.imgPic,
                loading = binding.loadingPhoto,
                notFound = binding.tvNotFound,
                url = msg.content.orEmpty()
            )
        }
        msg.type.isAudio() -> {
            renderAudio(
                avatarUrl = audioAvatarUrl,
                msg = msg,
                icPlayPause = binding.icPlayPause,
                seekBar = binding.seekBar,
                tvTimer = binding.tvTimer,
                circle = binding.circleImgAudio,
                getState = { mediaState },
                setState = onMediaStateChanged
            )
        }
    }
}

private fun bindLeft(
    binding: RowMsgLeftBinding,
    item: ChatMessageItem,
    isSelected: Boolean,
    hasSelection: Boolean,
    photoList: List<String>,
    onSelectionChanged: (ChatMessageItem, Boolean) -> Unit,
    audioAvatarUrl: String?,
    mediaState: MediaState,
    onMediaStateChanged: (MediaState) -> Unit
) {
    val msg = item.message
    bindBubbleMargins(isMe = false, isPhoto = msg.type.isPhoto(), bubble = binding.linearBubble)

    binding.linearMensajeMsg.isVisible = msg.type.isText()
    binding.linearMensajePic.isVisible = msg.type.isPhoto()
    binding.linearMensajeAudio.isVisible = msg.type.isAudio()

    binding.selectedItem.setBackgroundColor(
        ContextCompat.getColor(
            binding.root.context,
            if (isSelected) R.color.accent_transparent else R.color.transparent
        )
    )

    binding.horaMsg.text = formatHour(msg.createdAt)

    wireSelection(
        item = item,
        bubble = binding.linearCardMsg,
        photo = binding.imgPic,
        hasSelection = hasSelection,
        isSelected = isSelected,
        onSelectionChanged = onSelectionChanged,
        photoList = photoList
    )

    binding.checked.isVisible = false
    binding.checked2.isVisible = false

    when {
        msg.type.isText() -> binding.tvMsg.text = msg.content.orEmpty()
        msg.type.isPhoto() -> {
            renderPhoto(
                image = binding.imgPic,
                loading = binding.loadingPhoto,
                notFound = binding.tvNotFound,
                url = msg.content.orEmpty()
            )
        }
        msg.type.isAudio() -> {
            renderAudio(
                avatarUrl = audioAvatarUrl,
                msg = msg,
                icPlayPause = binding.icPlayPause,
                seekBar = binding.seekBar,
                tvTimer = binding.tvTimer,
                circle = binding.circleImgAudio,
                getState = { mediaState },
                setState = onMediaStateChanged
            )
        }
    }
}

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
    photo: View?,
    hasSelection: Boolean,
    isSelected: Boolean,
    onSelectionChanged: (ChatMessageItem, Boolean) -> Unit,
    photoList: List<String>
) {
    bubble.setOnClickListener {
        if (!hasSelection) return@setOnClickListener
        vibrateShort(bubble)
        toggleSelect(item, isSelected, onSelectionChanged)
    }

    val longClick: (View) -> Boolean = {
        vibrateShort(it)
        toggleSelect(item, isSelected, onSelectionChanged)
        false
    }

    bubble.setOnLongClickListener(longClick)
    photo?.setOnLongClickListener(longClick)

    photo?.setOnClickListener { v ->
        val url = item.message.content.orEmpty()
        val photoPos = photoList.indexOf(url)
        if (!hasSelection) {
            PhotoViewerActivity.start(v.context, ArrayList(photoList), photoPos)
        } else {
            vibrateShort(v)
            toggleSelect(item, isSelected, onSelectionChanged)
        }
    }
}

private fun toggleSelect(
    item: ChatMessageItem,
    isSelected: Boolean,
    onSelectionChanged: (ChatMessageItem, Boolean) -> Unit
) {
    onSelectionChanged(item, !isSelected)
}

private fun vibrateShort(view: View) {
    val vibrator = view.context.getSystemService(Vibrator::class.java)
    vibrator?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
}

private fun renderSeenChecksRight(b: RowMsgRightBinding, seen: Int) {
    when (seen) {
        MSG_DELIVERED -> {
            b.checked.isVisible = true
            b.checked2.isVisible = false
            b.checked.setColorFilter(
                ContextCompat.getColor(b.root.context, R.color.blanco),
                PorterDuff.Mode.SRC_IN
            )
        }
        MSG_RECEIVED -> {
            b.checked.isVisible = true
            b.checked2.isVisible = true
            val c = ContextCompat.getColor(b.root.context, R.color.blanco)
            b.checked.setColorFilter(c, PorterDuff.Mode.SRC_IN)
            b.checked2.setColorFilter(c, PorterDuff.Mode.SRC_IN)
        }
        MSG_SEEN -> {
            b.checked.isVisible = true
            b.checked2.isVisible = true
            val c = ContextCompat.getColor(b.root.context, R.color.visto)
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

    Glide.with(image)
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
    image.contentDescription = image.context.getString(R.string.photo_message)
}

private fun renderAudio(
    avatarUrl: String?,
    msg: ChatMessage,
    icPlayPause: ImageView,
    seekBar: SeekBar,
    tvTimer: Chronometer,
    circle: CircleImageView,
    getState: () -> MediaState,
    setState: (MediaState) -> Unit
) {
    Glide.with(circle).load(avatarUrl).into(circle)

    tvTimer.text = TimeUtils.formatAudioDuration(msg.audioDurationMs)

    seekBar.progress = 0
    seekBar.max = 0
    icPlayPause.setImageDrawable(
        ContextCompat.getDrawable(
            icPlayPause.context,
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
            if (fromUser && ChatAudioPlayer.mediaPlayer != null) {
                ChatAudioPlayer.mediaSelectedMs = progress.toLong()
                ChatAudioPlayer.mediaPlayer?.seekTo(progress)
                tvTimer.base = SystemClock.elapsedRealtime() - progress
            }
        }

        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    })
}

private fun playAudio(
    url: String,
    icPlayPause: ImageView,
    seekBar: SeekBar,
    tvTimer: Chronometer,
    setState: (MediaState) -> Unit
) {
    ChatAudioPlayer.mediaPlayer?.let { onCompletion(icPlayPause, seekBar, tvTimer, setState) }

    setState(MediaState.PLAY)
    ChatAudioPlayer.handler = Handler(icPlayPause.context.mainLooper)

    ChatAudioPlayer.moveSeekBarThread = object : Runnable {
        override fun run() {
            ChatAudioPlayer.mediaPlayer?.let { mp ->
                seekBar.max = mp.duration
                seekBar.progress = mp.currentPosition
                ChatAudioPlayer.handler?.postDelayed(this, 16)
            }
        }
    }

    ChatAudioPlayer.mediaPlayer = MediaPlayer().apply {
        setOnCompletionListener { onCompletion(icPlayPause, seekBar, tvTimer, setState) }
        setOnPreparedListener {
            icPlayPause.setImageDrawable(
                ContextCompat.getDrawable(
                    icPlayPause.context,
                    R.drawable.ic_baseline_pause_24
                )
            )
            seekTo(ChatAudioPlayer.mediaSelectedMs.toInt())
            start()
            tvTimer.base = SystemClock.elapsedRealtime() - ChatAudioPlayer.mediaSelectedMs
            tvTimer.start()
            ChatAudioPlayer.moveSeekBarThread?.run()
        }

        try {
            setDataSource(url)
            prepare()
        } catch (_: Exception) {
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
    if (ChatAudioPlayer.mediaSelectedMs != 0L) {
        tvTimer.base = SystemClock.elapsedRealtime() - ChatAudioPlayer.mediaSelectedMs
    } else {
        val intervalOnPause = (SystemClock.elapsedRealtime() - ChatAudioPlayer.chronStateSave)
        tvTimer.base = tvTimer.base + intervalOnPause - ChatAudioPlayer.mediaSelectedMs
    }
    tvTimer.start()
    ChatAudioPlayer.mediaPlayer?.start()
    icPlayPause.setImageDrawable(
        ContextCompat.getDrawable(
            icPlayPause.context,
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
    ChatAudioPlayer.mediaSelectedMs = 0
    ChatAudioPlayer.chronStateSave = SystemClock.elapsedRealtime()

    tvTimer.stop()
    tvTimer.text = TimeUtils.formatAudioDuration(msg.audioDurationMs)
    ChatAudioPlayer.mediaPlayer?.pause()
    seekBar.progress = ChatAudioPlayer.mediaPlayer?.currentPosition ?: 0

    icPlayPause.setImageDrawable(
        ContextCompat.getDrawable(
            icPlayPause.context,
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
            icPlayPause.context,
            R.drawable.ic_baseline_play_arrow_24
        )
    )
    tvTimer.stop()

    ChatAudioPlayer.handler?.removeCallbacks(ChatAudioPlayer.moveSeekBarThread ?: return)
    seekBar.setOnSeekBarChangeListener(null)
    seekBar.progress = 0

    try {
        ChatAudioPlayer.mediaPlayer?.stop()
    } catch (_: Exception) {
    }
    ChatAudioPlayer.mediaPlayer?.release()
    ChatAudioPlayer.mediaPlayer = null

    ChatAudioPlayer.mediaSelectedMs = 0
}

fun Int?.isPhoto(): Boolean = when (this) {
    MSG_PHOTO, MSG_PHOTO_RECEIVER_DLT, MSG_PHOTO_SENDER_DLT -> true
    else -> false
}

fun Int?.isText(): Boolean = when (this) {
    MSG_TEXT, MSG_TEXT_RECEIVER_DLT, MSG_TEXT_SENDER_DLT -> true
    else -> false
}

fun Int?.isAudio(): Boolean = when (this) {
    MSG_AUDIO, MSG_AUDIO_RECEIVER_DLT, MSG_AUDIO_SENDER_DLT -> true
    else -> false
}
