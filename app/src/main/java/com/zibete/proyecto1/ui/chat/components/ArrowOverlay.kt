package com.zibete.proyecto1.ui.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.ui.chat.ChatState
import com.zibete.proyecto1.ui.chat.preview.sampleChatStateMixedMessages
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ArrowOverlay(
    chatState: ChatState,
    listState: LazyListState,
    scope: CoroutineScope
) {

    val zibeExtendedColors = LocalZibeExtendedColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        FloatingActionButton(
            onClick = {
                scope.launch {
                    if (chatState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(chatState.messages.lastIndex)
                    }
                }
            },
            containerColor = zibeExtendedColors.snackbarSurface.copy(alpha = 0.8f)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                tint = zibeExtendedColors.lightText,
                contentDescription = null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArrowOverlayPreview() {
    ZibeTheme {
        ArrowOverlay(
            chatState = sampleChatStateMixedMessages(),
            listState = rememberLazyListState(),
            scope = rememberCoroutineScope()
        )
    }
}