package com.zibete.proyecto1.ui.chat.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.utils.TimeUtils.formatAudioDuration
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun RecordingRow(
    modifier: Modifier = Modifier,
    elapsedMs: Long,
    isRecordingCanceled: Boolean,
    playTrashDrop: Boolean = false,
    onTrashDropFinished: () -> Unit = {}
) {
    val timerText = formatAudioDuration(elapsedMs)
    val zibeExtendedColors = LocalZibeExtendedColors.current
    val cancelTextColor =
        if (isRecordingCanceled) zibeExtendedColors.snackRed else zibeExtendedColors.lightText

    val onTrashDropFinishedState by rememberUpdatedState(onTrashDropFinished)
    val playTrashDropState by rememberUpdatedState(playTrashDrop)
    var hasNotified by remember { mutableStateOf(false) }

    LaunchedEffect(playTrashDrop) {
        if (playTrashDrop) {
            hasNotified = false
        }
    }

    ZibeCard(
        modifier = modifier,
        border = null,
        contentPadding = PaddingValues(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (playTrashDrop) {
                    AndroidView(
                        modifier = Modifier.size(24.dp),
                        factory = { context ->
                            LottieAnimationView(context).apply {
                                setAnimation(R.raw.lottie_trash)
                                repeatCount = 0
                                playAnimation()
                                addAnimatorListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        if (playTrashDropState && !hasNotified) {
                                            hasNotified = true
                                            onTrashDropFinishedState()
                                        }
                                    }
                                })
                            }
                        },
                        update = { view ->
                            if (playTrashDrop && !hasNotified) {
                                view.repeatCount = 0
                                if (!view.isAnimating) {
                                    view.progress = 0f
                                    view.playAnimation()
                                }
                            } else if (!playTrashDrop) {
                                view.cancelAnimation()
                            }
                        }
                    )
                } else {
                    Text(
                        text = timerText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = zibeExtendedColors.accent
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.chat_cancel_recording_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = cancelTextColor
                )
            }
        }
    }
}

@Preview(name = "RecordingRow_Active", showBackground = true)
@Composable
private fun RecordingRowPreviewActive() {
    ZibeTheme {
        RecordingRow(elapsedMs = 9200L, isRecordingCanceled = false)
    }
}

@Preview(name = "RecordingRow_Canceled", showBackground = true)
@Composable
private fun RecordingRowPreviewCanceled() {
    ZibeTheme {
        RecordingRow(elapsedMs = 9200L, isRecordingCanceled = true)
    }
}
