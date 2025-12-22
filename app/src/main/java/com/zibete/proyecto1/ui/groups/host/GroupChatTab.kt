package com.zibete.proyecto1.ui.groups.host

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zibete.proyecto1.model.ChatGroupItem
import com.zibete.proyecto1.ui.constants.Constants.MSG_PHOTO
import com.zibete.proyecto1.ui.constants.Constants.MSG_TEXT
import kotlinx.coroutines.launch

@Composable
fun GroupChatTab(
    state: GroupHostUiState,
    onSendText: (String) -> Unit,
    onSendPhoto: (Uri) -> Unit
) {


    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onSendPhoto(uri)
    }

    // Auto-scroll al final si llegan mensajes
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            scope.launch { listState.scrollToItem(state.messages.lastIndex) }
        }
    }

    Column(Modifier.fillMaxSize()) {

        if (state.messages.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("El chat está vacío")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(10.dp)
            ) {
                items(
                    count = state.messages.size,
                    key = { idx -> state.messages[idx].id }
                ) { idx ->
                    GroupChatRow(
                        item = state.messages[idx]
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Divider()

        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = text,
                onValueChange = { text = it },
                enabled = !state.isSending,
                placeholder = { Text("Escribe un mensaje") },
                singleLine = true
            )

            val picker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri -> /* ... */ }
            )

            IconButton(
                enabled = !state.isSending,
                onClick = {
                    picker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Text("📷")
            }



            Button(
                enabled = !state.isSending && text.isNotBlank(),
                onClick = {
                    onSendText(text)
                    text = ""
                }
            ) {
                if (state.isSending) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Enviar")
            }
        }
    }
}

@Composable
private fun GroupChatRow(
    item: ChatGroupItem
) {
    val message = item.message

    val groupHostViewModel: GroupHostViewModel = hiltViewModel()

    val isMine = message.senderUid == groupHostViewModel.myUid

    val who = message.nameUser.ifBlank { "?" }
    val header = if (isMine) "Yo ($who)" else who

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text(header)
            Spacer(Modifier.height(4.dp))

            when (message.chatType) {
                MSG_PHOTO -> Text("📷 foto: ${message.content}")
                MSG_TEXT -> Text(message.content)
                else -> Text(message.content)
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = message.timestamp.toString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
