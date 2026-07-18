package com.zibete.proyecto1.ui.groups.host

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.MSG_INFO
import com.zibete.proyecto1.core.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.core.utils.TimeUtils
import com.zibete.proyecto1.model.ChatGroupItem
import com.zibete.proyecto1.ui.chat.components.ChatMessageTextField
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors

@Composable
fun GroupChatTab(
    state: GroupHostUiState,
    onTextChanged: (String) -> Unit,
    onSendText: () -> Unit,
    onSendPhoto: (String) -> Unit,
    onRetrySend: () -> Unit,
    onDismissSendError: () -> Unit
) {
    val listState = rememberLazyListState()
    var initialScrollCompleted by remember(state.roomKey) { mutableStateOf(false) }
    val lastMessage = state.messages.lastOrNull()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.toString()?.let(onSendPhoto) }
    )

    LaunchedEffect(lastMessage?.id) {
        if (lastMessage == null) return@LaunchedEffect
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        val isNearBottom = lastVisible >= state.messages.lastIndex - AUTO_SCROLL_THRESHOLD
        val isOwnMessage = lastMessage.message.senderUid == state.currentUid
        if (!initialScrollCompleted || isNearBottom || isOwnMessage) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
        initialScrollCompleted = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        Column(Modifier.fillMaxSize()) {
            if (state.messages.isEmpty()) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.rooms_chat_empty),
                        modifier = Modifier.padding(28.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.messages, key = { item -> item.id }) { item ->
                        GroupChatRow(
                            item = item,
                            isMine = item.message.senderUid == state.currentUid
                        )
                    }
                }
            }

            state.failedSend?.let {
                SendFailureBanner(
                    onRetry = onRetrySend,
                    onDismiss = onDismissSendError
                )
            }
            HorizontalDivider()
            RoomMessageComposer(
                text = state.composerText,
                isSending = state.isSending,
                onTextChanged = onTextChanged,
                onPickPhoto = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onSendText = onSendText
            )
        }
    }
}

@Composable
private fun RoomMessageComposer(
    text: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onSendText: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatMessageTextField(
            value = text,
            onValueChange = onTextChanged,
            placeholder = stringResource(R.string.rooms_message_hint),
            modifier = Modifier.weight(1f),
            enabled = !isSending
        )
        FilledIconButton(
            enabled = !isSending,
            onClick = onPickPhoto
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = stringResource(R.string.rooms_attach_photo)
            )
        }
        FilledIconButton(
            enabled = !isSending && text.isNotBlank(),
            onClick = onSendText
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = stringResource(R.string.rooms_send_message)
                )
            }
        }
    }
}

@Composable
private fun SendFailureBanner(
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.rooms_message_failed),
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.rooms_retry_message))
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }
    }
}

@Composable
private fun GroupChatRow(
    item: ChatGroupItem,
    isMine: Boolean
) {
    val message = item.message
    if (message.chatType == MSG_INFO) {
        RoomInfoEvent(
            text = listOf(message.resolvedUserName(), message.content)
                .filter { it.isNotBlank() }
                .joinToString(" "),
            timestamp = message.timestamp
        )
        return
    }

    val colors = LocalZibeExtendedColors.current
    val bubbleBrush = if (isMine) {
        Brush.linearGradient(listOf(colors.blueBubble, colors.zibeGradientEnd))
    } else {
        Brush.linearGradient(listOf(colors.pinkBubble, colors.zibeGradientStart))
    }
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 4.dp,
        bottomEnd = if (isMine) 4.dp else 18.dp
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (isMine) {
                    stringResource(R.string.rooms_you)
                } else {
                    message.resolvedUserName()
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(bubbleBrush)
            ) {
                if (message.chatType == MSG_PHOTO) {
                    RoomPhotoMessage(message.content)
                } else {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Text(
                text = if (message.timestamp > 0L) {
                    TimeUtils.formatHour(message.timestamp)
                } else {
                    ""
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun RoomPhotoMessage(url: String) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(url)
    val imageState = painter.state
    Box(
        modifier = Modifier
            .width(220.dp)
            .heightIn(min = 160.dp, max = 280.dp)
            .aspectRatio(3f / 4f)
            .clickable(enabled = url.isNotBlank()) {
                PhotoViewerActivity.startSingle(context, url)
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.rooms_photo_message),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        when (imageState) {
            is AsyncImagePainter.State.Loading -> CircularProgressIndicator(color = Color.White)
            is AsyncImagePainter.State.Error -> Text(
                text = stringResource(R.string.imagen_no_disponible),
                modifier = Modifier.padding(16.dp),
                color = Color.White
            )
            else -> Unit
        }
    }
}

@Composable
private fun RoomInfoEvent(text: String, timestamp: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            shape = RoundedCornerShape(50)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall
                )
                if (timestamp > 0L) {
                    Text(
                        text = TimeUtils.formatHour(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

private const val AUTO_SCROLL_THRESHOLD = 2
