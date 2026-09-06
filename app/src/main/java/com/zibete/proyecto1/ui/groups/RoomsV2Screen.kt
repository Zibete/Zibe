package com.zibete.proyecto1.ui.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Status

object RoomsV2TestTags {
    const val SCREEN = "rooms_v2_screen"
    const val LIST = "rooms_v2_list"
    const val LOADING = "rooms_v2_loading"
    const val ERROR = "rooms_v2_error"
    const val CREATE = "rooms_v2_create"
    const val CREATE_SHEET = "rooms_v2_create_sheet"
    const val JOIN_SHEET = "rooms_v2_join_sheet"
    const val SUBMIT = "rooms_v2_submit"

    fun room(roomId: String) = "rooms_v2_room_$roomId"
}

@Composable
fun RoomsV2Route(viewModel: RoomsV2ViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoomsV2Screen(
        state = state,
        onRefresh = viewModel::refreshRooms,
        onRetry = viewModel::loadRooms,
        onCreateRoom = viewModel::onCreateRoomRequested,
        onRoomSelected = viewModel::onRoomSelected,
        onDismissSheet = viewModel::dismissSheet,
        onIdentityModeSelected = viewModel::onIdentityModeSelected,
        onAliasChanged = viewModel::onAliasChanged,
        onRoomNameChanged = viewModel::onRoomNameChanged,
        onRoomDescriptionChanged = viewModel::onRoomDescriptionChanged,
        onSubmit = viewModel::submitSheet,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsV2Screen(
    state: RoomsV2UiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onCreateRoom: () -> Unit,
    onRoomSelected: (RoomV2ListItem) -> Unit,
    onDismissSheet: () -> Unit,
    onIdentityModeSelected: (RoomV2IdentityMode) -> Unit,
    onAliasChanged: (String) -> Unit,
    onRoomNameChanged: (String) -> Unit,
    onRoomDescriptionChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(RoomsV2TestTags.SCREEN),
        floatingActionButton = {
            if (state.sheet == null) {
                FloatingActionButton(
                    onClick = onCreateRoom,
                    modifier = Modifier.testTag(RoomsV2TestTags.CREATE),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.rooms_v2_create),
                    )
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.rooms.isEmpty() -> RoomsV2Loading()
                state.error != null && state.rooms.isEmpty() -> RoomsV2Error(
                    message = state.error.asString(),
                    onRetry = onRetry,
                )
                state.visibleRooms.isEmpty() -> RoomsV2Empty(
                    searching = state.searchQuery.isNotBlank(),
                )
                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(RoomsV2TestTags.LIST),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 8.dp,
                        end = 12.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = state.visibleRooms,
                        key = { it.room.roomId },
                    ) { item ->
                        RoomV2Card(item = item, onClick = { onRoomSelected(item) })
                    }
                }
            }
        }
    }

    when (val sheet = state.sheet) {
        RoomsV2Sheet.Create -> CreateRoomV2Sheet(
            state = state,
            onDismiss = onDismissSheet,
            onRoomNameChanged = onRoomNameChanged,
            onRoomDescriptionChanged = onRoomDescriptionChanged,
            onSubmit = onSubmit,
        )
        is RoomsV2Sheet.Join -> JoinRoomV2Sheet(
            state = state,
            roomName = sheet.room.name,
            onDismiss = onDismissSheet,
            onIdentityModeSelected = onIdentityModeSelected,
            onAliasChanged = onAliasChanged,
            onSubmit = onSubmit,
        )
        null -> Unit
    }
}

@Composable
private fun RoomV2Card(item: RoomV2ListItem, onClick: () -> Unit) {
    val room = item.room
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RoomsV2TestTags.room(room.roomId)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = room.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.unreadCount > 0) {
                    Badge {
                        Text(item.unreadCount.coerceAtMost(99).toString())
                    }
                }
            }
            if (room.description.isNotBlank()) {
                Text(
                    text = room.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    stringResource(R.string.rooms_v2_participants, room.memberCount),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                if (room.status == RoomV2Status.CLOSED) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text(stringResource(R.string.rooms_v2_closed)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                } else if (item.isMember) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text(stringResource(R.string.rooms_v2_member)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomsV2Loading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(RoomsV2TestTags.LOADING),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.rooms_v2_loading))
        }
    }
}

@Composable
private fun RoomsV2Error(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag(RoomsV2TestTags.ERROR),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.rooms_v2_error_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRetry) {
                Text(stringResource(R.string.rooms_v2_retry))
            }
        }
    }
}

@Composable
private fun RoomsV2Empty(searching: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(
                    if (searching) R.string.rooms_v2_search_empty_title
                    else R.string.rooms_v2_empty_title,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    if (searching) R.string.rooms_v2_search_empty_message
                    else R.string.rooms_v2_empty_message,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRoomV2Sheet(
    state: RoomsV2UiState,
    onDismiss: () -> Unit,
    onRoomNameChanged: (String) -> Unit,
    onRoomDescriptionChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(RoomsV2TestTags.CREATE_SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.rooms_v2_create_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(
                    R.string.rooms_v2_public_identity,
                    state.publicIdentityName,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = state.roomName,
                onValueChange = onRoomNameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rooms_v2_room_name)) },
                singleLine = true,
                isError = state.roomNameError != null,
                supportingText = state.roomNameError?.let { error ->
                    { Text(error.asString()) }
                },
                enabled = !state.isSubmitting,
            )
            OutlinedTextField(
                value = state.roomDescription,
                onValueChange = onRoomDescriptionChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.rooms_v2_description)) },
                minLines = 3,
                maxLines = 5,
                isError = state.roomDescriptionError != null,
                supportingText = state.roomDescriptionError?.let { error ->
                    { Text(error.asString()) }
                },
                enabled = !state.isSubmitting,
            )
            SheetButtons(
                submitting = state.isSubmitting,
                submitText = stringResource(R.string.rooms_v2_create_confirm),
                onDismiss = onDismiss,
                onSubmit = onSubmit,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinRoomV2Sheet(
    state: RoomsV2UiState,
    roomName: String,
    onDismiss: () -> Unit,
    onIdentityModeSelected: (RoomV2IdentityMode) -> Unit,
    onAliasChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(RoomsV2TestTags.JOIN_SHEET),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(R.string.rooms_v2_join_title, roomName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.rooms_v2_identity_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.identityMode == RoomV2IdentityMode.REAL,
                    onClick = { onIdentityModeSelected(RoomV2IdentityMode.REAL) },
                    label = { Text(stringResource(R.string.rooms_v2_identity_real)) },
                    enabled = !state.isSubmitting,
                )
                FilterChip(
                    selected = state.identityMode == RoomV2IdentityMode.ANONYMOUS,
                    onClick = { onIdentityModeSelected(RoomV2IdentityMode.ANONYMOUS) },
                    label = { Text(stringResource(R.string.rooms_v2_identity_anonymous)) },
                    enabled = !state.isSubmitting,
                )
            }
            if (state.identityMode == RoomV2IdentityMode.REAL) {
                Text(
                    stringResource(
                        R.string.rooms_v2_public_identity,
                        state.publicIdentityName,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                OutlinedTextField(
                    value = state.alias,
                    onValueChange = onAliasChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rooms_v2_alias)) },
                    singleLine = true,
                    isError = state.identityError != null,
                    supportingText = state.identityError?.let { error ->
                        { Text(error.asString()) }
                    },
                    enabled = !state.isSubmitting,
                )
            }
            SheetButtons(
                submitting = state.isSubmitting,
                submitText = stringResource(R.string.rooms_v2_join_confirm),
                onDismiss = onDismiss,
                onSubmit = onSubmit,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetButtons(
    submitting: Boolean,
    submitText: String,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismiss, enabled = !submitting) {
            Text(stringResource(android.R.string.cancel))
        }
        Button(
            onClick = onSubmit,
            enabled = !submitting,
            modifier = Modifier.testTag(RoomsV2TestTags.SUBMIT),
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(submitText)
            }
        }
    }
}
