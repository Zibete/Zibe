package com.zibete.proyecto1.ui.chat.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.utils.TimeUtils.formatAudioDuration
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun RecordingRow(
    modifier: Modifier = Modifier,
    elapsedMs: Long,
    isRecordingCanceled: Boolean
) {
    val timerText = formatAudioDuration(elapsedMs)
    val zibeExtendedColors = LocalZibeExtendedColors.current
    val cancelTextColor =
        if (isRecordingCanceled) zibeExtendedColors.snackRed else zibeExtendedColors.lightText

    ZibeCard(
        modifier = modifier,
        border = null,
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timerText,
                style = MaterialTheme.typography.bodyMedium,
                color = zibeExtendedColors.accent
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.chat_cancel_recording_hint),
                style = MaterialTheme.typography.bodySmall,
                color = cancelTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
