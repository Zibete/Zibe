package com.zibete.proyecto1.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ChatInfoRow(text: String) {

    val zibeExtendedColors = LocalZibeExtendedColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(zibeExtendedColors.lightText)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = zibeExtendedColors.lightText,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .weight(1f)
                .background(zibeExtendedColors.lightText)
        )
    }
}

@Preview(name = "ChatInfoRow_Default", showBackground = true)
@Composable
private fun ChatInfoRowPreview() {
    ZibeTheme {
        ChatInfoRow(text = "Today")
    }
}
