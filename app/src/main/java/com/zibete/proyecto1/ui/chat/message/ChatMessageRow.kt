package com.zibete.proyecto1.ui.chat.message

import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.MSG_DELIVERED
import com.zibete.proyecto1.core.constants.Constants.MSG_RECEIVED
import com.zibete.proyecto1.core.constants.Constants.MSG_SEEN
import com.zibete.proyecto1.core.utils.TimeUtils
import com.zibete.proyecto1.model.ChatMessage
import com.zibete.proyecto1.model.ChatMessageItem
import com.zibete.proyecto1.model.isDeletedFor
import com.zibete.proyecto1.ui.chat.media.ChatAudioPlayer
import com.zibete.proyecto1.ui.chat.media.MediaState
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import kotlinx.coroutines.delay
import com.zibete.proyecto1.core.designsystem.R as DsR

@Composable
fun ChatMessageRow(
    item: ChatMessageItem,
    isMe: Boolean,
    isSelected: Boolean,
    hasSelection: Boolean,
    myAudioAvatarUrl: String?,
    otherAudioAvatarUrl: String?,
    photoList: List<String>,
    currentUid: String,
    onSelectionChanged: (ChatMessageItem, Boolean) -> Unit
) {
    if (item.message.isDeletedFor(currentUid)) return

    val context = LocalContext.current
    val selectionColor = colorResource(DsR.color.accent_transparent)
    val audioAvatarUrl = if (isMe) myAudioAvatarUrl else otherAudioAvatarUrl
    val horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    val rowArrangement = if (isMe) Arrangement.End else Arrangement.Start

    fun toggleSelection() {
        context.getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(75, VibrationEffect.DEFAULT_AMPLITUDE))
        onSelectionChanged(item, !isSelected)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) selectionColor else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = rowArrangement
        ) {
            Column(horizontalAlignment = horizontalAlignment) {
                Row(
                    modifier = Modifier
                        .widthIn(max = if (item.message.type.isPhoto()) 318.dp else 340.dp)
                        .combinedClickable(
                            onClick = { if (hasSelection) toggleSelection() },
                            onLongClick = { toggleSelection() }
                        ),
                    verticalAlignment = Alignment.Bottom
                ) {
                    MessageBubble(
                        msg = item.message,
                        isMe = isMe,
                        audioAvatarUrl = audioAvatarUrl,
                        photoList = photoList,
                        modifier = Modifier.weight(1f, fill = false),
                        onPhotoClick = {
                            if (hasSelection) {
                                toggleSelection()
                            } else {
                                val photoUrl = item.message.content.orEmpty()
                                val photoPos = photoList.indexOf(photoUrl)
                                PhotoViewerActivity.start(
                                    context = context,
                                    photoList = ArrayList(photoList),
                                    position = photoPos
                                )
                            }
                        },
                        onPhotoLongClick = { toggleSelection() }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    MessageMetaRow(
                        createdAt = item.message.createdAt,
                        seen = item.message.seen,
                        isMe = isMe,
                        modifier = Modifier
                            .widthIn(min = if (isMe) 56.dp else 38.dp)
                            .wrapContentWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    msg: ChatMessage,
    isMe: Boolean,
    audioAvatarUrl: String?,
    photoList: List<String>,
    modifier: Modifier = Modifier,
    onPhotoClick: () -> Unit,
    onPhotoLongClick: () -> Unit
) {
    val colors = LocalZibeExtendedColors.current
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMe) 18.dp else 0.dp,
        bottomEnd = if (isMe) 0.dp else 18.dp
    )
    val bubbleBrush = if (isMe) {
        Brush.linearGradient(listOf(colors.blueBubble, Color(0xFF5E6DFD)))
    } else {
        Brush.linearGradient(listOf(colors.pinkBubble, Color(0xFFFF6F91)))
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bubbleBrush)
            .border(1.dp, Color.Black.copy(alpha = 0.2f), shape)
    ) {
        when {
            msg.type.isText() -> ChatTextBubble(text = msg.content.orEmpty())
            msg.type.isPhoto() -> ChatPhotoBubble(
                url = msg.content.orEmpty(),
                onClick = onPhotoClick,
                onLongClick = onPhotoLongClick
            )
            msg.type.isAudio() -> ChatAudioBubble(
                msg = msg,
                avatarUrl = audioAvatarUrl
            )
        }
    }
}

@Composable
fun ChatTextBubble(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ChatPhotoBubble(
    url: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val painter = rememberAsyncImagePainter(model = url)
    val imageState = painter.state

    Box(
        modifier = Modifier
            .width(220.dp)
            .heightIn(min = 160.dp, max = 280.dp)
            .aspectRatio(3f / 4f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.photo_message),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (imageState is AsyncImagePainter.State.Loading) {
            CircularProgressIndicator(color = Color.White)
        }

        if (imageState is AsyncImagePainter.State.Error) {
            Text(
                text = stringResource(R.string.imagen_no_disponible),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
fun ChatAudioBubble(
    msg: ChatMessage,
    avatarUrl: String?
) {
    var mediaState by remember(msg.content) { mutableStateOf(MediaState.NOT_STARTED) }
    var sliderPosition by remember(msg.content) { mutableFloatStateOf(0f) }
    var durationMs by remember(msg.content) { mutableIntStateOf(msg.audioDurationMs.toInt()) }

    LaunchedEffect(ComposeAudioPlayback.activeUrl) {
        if (ComposeAudioPlayback.activeUrl != msg.content && mediaState != MediaState.NOT_STARTED) {
            sliderPosition = 0f
            mediaState = MediaState.NOT_STARTED
        }
    }

    LaunchedEffect(mediaState) {
        while (mediaState == MediaState.PLAY) {
            val player = ChatAudioPlayer.mediaPlayer
            if (player != null) {
                durationMs = player.duration.coerceAtLeast(msg.audioDurationMs.toInt())
                sliderPosition = player.currentPosition.toFloat()
            }
            delay(100)
        }
    }

    Row(
        modifier = Modifier
            .widthIn(min = 220.dp, max = 260.dp)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                when (mediaState) {
                    MediaState.NOT_STARTED -> playAudio(
                        url = msg.content.orEmpty(),
                        startMs = sliderPosition.toInt(),
                        onStarted = { ComposeAudioPlayback.activeUrl = msg.content },
                        onPrepared = { preparedDuration ->
                            durationMs = preparedDuration
                            mediaState = MediaState.PLAY
                        },
                        onProgressReset = {
                            ComposeAudioPlayback.activeUrl = null
                            sliderPosition = 0f
                            mediaState = MediaState.NOT_STARTED
                        }
                    )
                    MediaState.PLAY -> {
                        ChatAudioPlayer.mediaPlayer?.pause()
                        mediaState = MediaState.PAUSE
                    }
                    MediaState.PAUSE -> {
                        ChatAudioPlayer.mediaPlayer?.start()
                        mediaState = MediaState.PLAY
                    }
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (mediaState == MediaState.PLAY) {
                    Icons.Filled.Pause
                } else {
                    Icons.Filled.PlayArrow
                },
                contentDescription = null,
                tint = Color.White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Slider(
                value = sliderPosition.coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { position ->
                    sliderPosition = position
                    ChatAudioPlayer.mediaPlayer?.seekTo(position.toInt())
                },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                )
            )

            Text(
                text = TimeUtils.formatAudioDuration(
                    if (sliderPosition > 0f) sliderPosition.toLong() else msg.audioDurationMs
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        AudioAvatar(avatarUrl = avatarUrl)
    }
}

@Composable
private fun AudioAvatar(avatarUrl: String?) {
    val painter = rememberAsyncImagePainter(model = avatarUrl)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun MessageMetaRow(
    createdAt: Long,
    seen: Int,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier.padding(bottom = 2.dp)
    ) {
        Text(
            text = TimeUtils.formatHour(createdAt),
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.75f)
        )

        if (isMe) {
            Spacer(modifier = Modifier.width(2.dp))
            SeenStatusIcon(seen = seen)
        }
    }
}

@Composable
fun SeenStatusIcon(seen: Int) {
    val checkColor = if (seen == MSG_SEEN) colorResource(DsR.color.check_seen) else Color.White

    when (seen) {
        MSG_DELIVERED -> SeenCheck(tint = checkColor)
        MSG_RECEIVED, MSG_SEEN -> {
            Box(modifier = Modifier.width(18.dp)) {
                SeenCheck(
                    tint = checkColor,
                    modifier = Modifier.offset(x = 0.dp)
                )
                SeenCheck(
                    tint = checkColor,
                    modifier = Modifier.offset(x = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun SeenCheck(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(R.drawable.ic_check_24),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(14.dp)
    )
}

private object ComposeAudioPlayback {
    var activeUrl by mutableStateOf<String?>(null)
}

private fun playAudio(
    url: String,
    startMs: Int,
    onStarted: () -> Unit,
    onPrepared: (Int) -> Unit,
    onProgressReset: () -> Unit
) {
    ChatAudioPlayer.release()
    onStarted()
    ChatAudioPlayer.mediaPlayer = MediaPlayer().apply {
        setOnCompletionListener {
            ChatAudioPlayer.release()
            onProgressReset()
        }
        setOnPreparedListener { player ->
            player.seekTo(startMs)
            player.start()
            onPrepared(player.duration)
        }

        try {
            setDataSource(url)
            prepare()
        } catch (_: Exception) {
            ChatAudioPlayer.release()
            onProgressReset()
        }
    }
}
