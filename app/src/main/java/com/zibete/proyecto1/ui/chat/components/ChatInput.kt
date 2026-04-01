package com.zibete.proyecto1.ui.chat.components

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ChatInput(
    inputText: String,
    pendingPhotoUri: Uri?,
    showSend: Boolean,
    sendEnabled: Boolean,
    isPhotoUploading: Boolean,
    isRecording: Boolean,
    recordingElapsedMs: Long,
    isRecordingCanceled: Boolean,
    showTrashDrop: Boolean,
    onTrashDropFinished: () -> Unit,
    isAudioUploading: Boolean,
    onInputChange: (String) -> Unit,
    onRemovePendingPhoto: () -> Unit,
    onCameraClick: () -> Unit,
    onSendClick: () -> Unit,
    onMicPressed: () -> Unit,
    onMicMoved: (Float, Float, Float) -> Unit,
    onMicReleased: () -> Unit,
    onMicPressStateChange: (Boolean) -> Unit = {},
    onMicButtonPositioned: (Offset) -> Unit = {},
    onMicPointerInWindowChanged: (Offset) -> Unit = {}
) {
    var isMicPressed by remember { mutableStateOf(false) }
    val chatComponentsHeight = dimensionResource(DsR.dimen.zibe_btn_height)
    val showRecordingTrash = isRecording || showTrashDrop

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(all = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (showRecordingTrash) {
                TrashLottie(
                    isVisible = true,
                    isHighlighted = isRecordingCanceled,
                    playDrop = showTrashDrop,
                    onDropFinished = onTrashDropFinished,
                    size = chatComponentsHeight
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (pendingPhotoUri != null) {
                    ZibePhotoPreview(
                        uri = pendingPhotoUri,
                        isUploading = isPhotoUploading,
                        onRemove = onRemovePendingPhoto
                    )
                } else if (isRecording) {
                    RecordingRow(
                        modifier = Modifier.height(chatComponentsHeight),
                        elapsedMs = recordingElapsedMs,
                        isRecordingCanceled = isRecordingCanceled
                    )
                } else {
                    ChatMessageTextField(
                        modifier = Modifier.height(chatComponentsHeight),
                        value = inputText,
                        onValueChange = onInputChange,
                        placeholder = stringResource(R.string.escribe_un_mensaje),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier
                    .wrapContentWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showSend) {
                    ChatActionCircleButton(
                        iconVector = Icons.AutoMirrored.Rounded.Send,
                        enabled = sendEnabled,
                        isLoading = isPhotoUploading || isAudioUploading,
                        onClick = onSendClick
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRecording) {
                            ChatActionCircleButton(
                                iconVector = Icons.Filled.PhotoCamera,
                                enabled = !isPhotoUploading,
                                isLoading = isPhotoUploading,
                                onClick = onCameraClick
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        val hideMic = isMicPressed || isRecording
                        MicRecordButton(
                            isRecording = isRecording,
                            modifier = Modifier
                                .alpha(if (hideMic) 0f else 1f)
                                .height(chatComponentsHeight),
                            onMicPressed = onMicPressed,
                            onMicMoved = onMicMoved,
                            onMicReleased = onMicReleased,
                            onPressStateChange = { pressed ->
                                isMicPressed = pressed
                                onMicPressStateChange(pressed)
                            },
                            onCenterPositioned = onMicButtonPositioned,
                            onPointerInWindowChanged = onMicPointerInWindowChanged
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "ChatInput_Idle", showBackground = true)
@Composable
private fun ChatInputPreviewIdle() {
    ZibeTheme {
        ChatInput(
            inputText = "",
            onInputChange = {},
            pendingPhotoUri = null,
            isPhotoUploading = false,
            onRemovePendingPhoto = {},
            isRecording = false,
            recordingElapsedMs = 0L,
            isRecordingCanceled = false,
            showSend = false,
            sendEnabled = false,
            onCameraClick = {},
            onSendClick = {},
            onMicPressed = {},
            onMicMoved = { _, _, _ -> },
            onMicReleased = {},
            showTrashDrop = false,
            onTrashDropFinished = {},
            isAudioUploading = false,
        )
    }
}

@Preview(name = "ChatInput_Typing", showBackground = true)
@Composable
private fun ChatInputPreviewTyping() {
    ZibeTheme {
        ChatInput(
            inputText = "Hola",
            onInputChange = {},
            pendingPhotoUri = null,
            isPhotoUploading = false,
            onRemovePendingPhoto = {},
            isRecording = false,
            recordingElapsedMs = 0L,
            isRecordingCanceled = false,
            showSend = true,
            sendEnabled = true,
            onCameraClick = {},
            onSendClick = {},
            onMicPressed = {},
            onMicMoved = { _, _, _ -> },
            onMicReleased = {},
            showTrashDrop = false,
            onTrashDropFinished = {},
            isAudioUploading = false,
        )
    }
}

@Preview(name = "ChatInput_PendingPhoto", showBackground = true)
@Composable
private fun ChatInputPreviewPendingPhoto() {
    ZibeTheme {
        ChatInput(
            inputText = "",
            onInputChange = {},
            pendingPhotoUri = Uri.parse("content://preview/photo"),
            isPhotoUploading = true,
            onRemovePendingPhoto = {},
            isRecording = false,
            recordingElapsedMs = 0L,
            isRecordingCanceled = false,
            showSend = true,
            sendEnabled = false,
            onCameraClick = {},
            onSendClick = {},
            onMicPressed = {},
            onMicMoved = { _, _, _ -> },
            onMicReleased = {},
            showTrashDrop = false,
            onTrashDropFinished = {},
            isAudioUploading = false,
        )
    }
}

@Preview(name = "ChatInput_Recording", showBackground = true)
@Composable
private fun ChatInputPreviewRecording() {
    ZibeTheme {
        ChatInput(
            inputText = "",
            onInputChange = {},
            pendingPhotoUri = null,
            isPhotoUploading = false,
            onRemovePendingPhoto = {},
            isRecording = true,
            recordingElapsedMs = 6500L,
            isRecordingCanceled = false,
            showSend = false,
            sendEnabled = false,
            onCameraClick = {},
            onSendClick = {},
            onMicPressed = {},
            onMicMoved = { _, _, _ -> },
            onMicReleased = {},
            showTrashDrop = false,
            onTrashDropFinished = {},
            isAudioUploading = false,
        )
    }
}

@Preview(name = "ChatInput_RecordingCanceled", showBackground = true)
@Composable
private fun ChatInputPreviewRecordingCanceled() {
    ZibeTheme {
        ChatInput(
            inputText = "",
            onInputChange = {},
            pendingPhotoUri = null,
            isPhotoUploading = false,
            onRemovePendingPhoto = {},
            isRecording = true,
            recordingElapsedMs = 6500L,
            isRecordingCanceled = true,
            showSend = false,
            sendEnabled = false,
            onCameraClick = {},
            onSendClick = {},
            onMicPressed = {},
            onMicMoved = { _, _, _ -> },
            onMicReleased = {},
            showTrashDrop = false,
            onTrashDropFinished = {},
            isAudioUploading = false,
        )
    }
}



