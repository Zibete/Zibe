package com.zibete.proyecto1.ui.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zibete.proyecto1.R
import com.zibete.proyecto1.model.Groups
import com.zibete.proyecto1.model.RoomIdentityType
import com.zibete.proyecto1.ui.components.SheetActions
import com.zibete.proyecto1.ui.components.SheetHeader
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.ZibeDialog
import com.zibete.proyecto1.ui.components.ZibeInputField
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.LocalZibeTextStyles
import com.zibete.proyecto1.core.designsystem.R as DsR

object RoomsTestTags {
    const val SCREEN = "rooms_screen"
    const val LIST = "rooms_list"
    const val LOADING = "rooms_loading"
    const val EMPTY = "rooms_empty"
    const val ERROR = "rooms_error"
    const val CREATE_ACTION = "rooms_create_action"
    const val CREATE_SHEET = "rooms_create_sheet"
    const val JOIN_SHEET = "rooms_join_sheet"
    const val CREATE_CONFIRM = "rooms_create_confirm"
    const val JOIN_CONFIRM = "rooms_join_confirm"

    fun room(roomKey: String) = "room_$roomKey"
}

@Composable
fun RoomsRoute(viewModel: GroupsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val joinEventContent = stringResource(R.string.rooms_joined_event)
    val leaveEventContent = stringResource(R.string.rooms_left_event)
    RoomsScreen(
        state = state,
        onRefresh = viewModel::refreshRooms,
        onRetry = viewModel::loadRooms,
        onCreateRoom = viewModel::onCreateRoomRequested,
        onRoomSelected = viewModel::onRoomSelected,
        onDismissSheet = viewModel::dismissSheet,
        onIdentitySelected = viewModel::onIdentitySelected,
        onAliasChanged = viewModel::onAliasChanged,
        onRoomNameChanged = viewModel::onRoomNameChanged,
        onRoomDescriptionChanged = viewModel::onRoomDescriptionChanged,
        onSubmitSheet = {
            viewModel.submitSheet(joinEventContent, leaveEventContent)
        },
        onConfirmSwitch = { viewModel.confirmSwitch(leaveEventContent) },
        onDismissSwitch = viewModel::dismissSwitch
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomsScreen(
    state: GroupsUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onCreateRoom: () -> Unit,
    onRoomSelected: (Groups) -> Unit,
    onDismissSheet: () -> Unit,
    onIdentitySelected: (RoomIdentityType) -> Unit,
    onAliasChanged: (String) -> Unit,
    onRoomNameChanged: (String) -> Unit,
    onRoomDescriptionChanged: (String) -> Unit,
    onSubmitSheet: () -> Unit,
    onConfirmSwitch: () -> Unit,
    onDismissSwitch: () -> Unit
) {
    val colors = LocalZibeExtendedColors.current
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(RoomsTestTags.SCREEN)
    ) {
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.isLoading && state.rooms.isEmpty() -> RoomsLoading()
                state.error != null && state.rooms.isEmpty() -> RoomsError(
                    message = state.error.asString(),
                    onRetry = onRetry
                )
                state.visibleRooms.isEmpty() -> RoomsEmpty(
                    isSearchEmpty = state.searchQuery.isNotBlank()
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(RoomsTestTags.LIST),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = 4.dp,
                        end = 8.dp,
                        bottom = 104.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = state.visibleRooms,
                        key = { it.resolvedRoomKey() },
                        contentType = { "room" }
                    ) { room ->
                        GlassRoomCard(
                            room = room,
                            isActive = state.activeSession?.roomKey == room.resolvedRoomKey(),
                            onClick = { onRoomSelected(room) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !state.isLoading && state.error == null &&
                state.sheet == null && state.pendingSwitch == null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = onCreateRoom,
                modifier = Modifier.testTag(RoomsTestTags.CREATE_ACTION),
                containerColor = colors.accent,
                contentColor = colors.lightText
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.rooms_create)
                )
            }
        }
    }

    when (val sheet = state.sheet) {
        RoomsSheet.Create -> CreateRoomSheet(
            state = state,
            onDismiss = onDismissSheet,
            onRoomNameChanged = onRoomNameChanged,
            onRoomDescriptionChanged = onRoomDescriptionChanged,
            onSubmit = onSubmitSheet
        )
        is RoomsSheet.Join -> JoinRoomSheet(
            room = sheet.room,
            state = state,
            onDismiss = onDismissSheet,
            onIdentitySelected = onIdentitySelected,
            onAliasChanged = onAliasChanged,
            onSubmit = onSubmitSheet
        )
        null -> Unit
    }

    state.pendingSwitch?.let { pending ->
        val targetName = when (pending) {
            is PendingRoomSwitch.Create -> pending.command.roomName
            is PendingRoomSwitch.Join -> pending.command.displayName
        }
        ZibeDialog(
            title = stringResource(R.string.rooms_switch_title),
            content = {
                Text(
                    stringResource(
                        R.string.rooms_switch_message,
                        targetName,
                        pending.currentSession.displayName
                    )
                )
            },
            confirmText = stringResource(R.string.rooms_switch_confirm),
            onConfirm = onConfirmSwitch,
            onCancel = onDismissSwitch,
            enabled = !state.isSubmitting
        )
    }
}

@Composable
internal fun GlassRoomCard(room: Groups, isActive: Boolean, onClick: () -> Unit) {
    val colors = LocalZibeExtendedColors.current
    val textStyles = LocalZibeTextStyles.current
    val roomName = room.resolvedDisplayName()
    val participants = if (room.users == 1) {
        stringResource(R.string.rooms_one_participant)
    } else {
        stringResource(R.string.rooms_participants_count, room.users)
    }
    val cardDescription = stringResource(
        R.string.rooms_card_content_description,
        roomName,
        participants
    )
    val activeDescription = stringResource(R.string.rooms_active)
    val shape = RoundedCornerShape(20.dp)
    val glow = Brush.linearGradient(
        listOf(
            colorResource(DsR.color.zibe_gradient_end),
            colorResource(DsR.color.zibe_pink),
            colorResource(DsR.color.zibe_purple)
        )
    )
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(RoomsTestTags.room(room.resolvedRoomKey()))
            .semantics { contentDescription = cardDescription },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(2.dp, glow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colorResource(DsR.color.glass_bg_light),
                            colorResource(DsR.color.glass_indicator_light)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = roomName,
                    modifier = Modifier.weight(1f),
                    style = textStyles.brandSubtitle,
                    color = colors.lightText,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Surface(
                        modifier = Modifier
                            .size(10.dp)
                            .semantics {
                                contentDescription = activeDescription
                            },
                        shape = CircleShape,
                        color = colors.accent
                    ) {}
                }
                if (room.unreadCount > 0) RoomUnreadBadge(room.unreadCount)
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                color = colorResource(DsR.color.glass_stroke_light)
            )
            Text(
                text = room.description,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.lightText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = colors.lightText,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = participants,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.lightText
                )
            }
        }
    }
}

@Composable
private fun RoomMetadata(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val colors = LocalZibeExtendedColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.hintText, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = colors.hintText)
    }
}

@Composable
private fun RoomUnreadBadge(unreadCount: Int) {
    val colors = LocalZibeExtendedColors.current
    Surface(shape = CircleShape, color = colors.accent) {
        Text(
            text = unreadCount.coerceAtMost(99).toString(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = colors.lightText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoomsLoading() {
    val colors = LocalZibeExtendedColors.current
    Box(
        modifier = Modifier.fillMaxSize().testTag(RoomsTestTags.LOADING),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = colors.accent)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.rooms_loading), color = colors.lightText)
        }
    }
}

@Composable
private fun RoomsEmpty(isSearchEmpty: Boolean) {
    val colors = LocalZibeExtendedColors.current
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp).testTag(RoomsTestTags.EMPTY),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSearchEmpty) Icons.Default.SearchOff else Icons.Default.MeetingRoom,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(
                    if (isSearchEmpty) R.string.rooms_search_empty_title
                    else R.string.rooms_empty_title
                ),
                style = MaterialTheme.typography.titleLarge,
                color = colors.lightText,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (isSearchEmpty) R.string.rooms_search_empty_message
                    else R.string.rooms_empty_message
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.hintText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RoomsError(message: String, onRetry: () -> Unit) {
    val colors = LocalZibeExtendedColors.current
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp).testTag(RoomsTestTags.ERROR),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = colors.snackRed,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.rooms_error_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.lightText,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.hintText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            ZibeButtonPrimary(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(0.72f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRoomSheet(
    state: GroupsUiState,
    onDismiss: () -> Unit,
    onRoomNameChanged: (String) -> Unit,
    onRoomDescriptionChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    ZibeBottomSheet(
        isOpen = true,
        onCancel = onDismiss,
        openFullyExpanded = true,
        modifier = Modifier.testTag(RoomsTestTags.CREATE_SHEET),
        footer = {
            SheetActions(
                onConfirm = onSubmit,
                onCancel = onDismiss,
                modifier = Modifier.testTag(RoomsTestTags.CREATE_CONFIRM),
                confirmText = stringResource(R.string.rooms_create),
                confirmEnabled = state.roomName.isNotBlank() &&
                    state.roomDescription.isNotBlank() &&
                    state.publicIdentityName.isNotBlank(),
                isConfirmLoading = state.isSubmitting
            )
        }
    ) {
        SheetHeader(
            title = stringResource(R.string.rooms_create),
            subtitle = stringResource(R.string.rooms_create_description)
        )
        ZibeInputField(
            value = state.roomName,
            label = stringResource(R.string.rooms_name),
            onValueChange = onRoomNameChanged,
            enabled = !state.isSubmitting,
            error = state.roomNameError?.asString()
        )
        ZibeInputField(
            value = state.roomDescription,
            label = stringResource(R.string.rooms_description),
            onValueChange = onRoomDescriptionChanged,
            enabled = !state.isSubmitting,
            singleLine = false,
            error = state.roomDescriptionError?.asString()
        )
        PublicCreatorIdentity(state)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinRoomSheet(
    room: Groups,
    state: GroupsUiState,
    onDismiss: () -> Unit,
    onIdentitySelected: (RoomIdentityType) -> Unit,
    onAliasChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val participantText = if (room.users == 1) {
        stringResource(R.string.rooms_one_participant)
    } else {
        stringResource(R.string.rooms_participants_count, room.users)
    }
    ZibeBottomSheet(
        isOpen = true,
        onCancel = onDismiss,
        openFullyExpanded = true,
        modifier = Modifier.testTag(RoomsTestTags.JOIN_SHEET),
        footer = {
            SheetActions(
                onConfirm = onSubmit,
                onCancel = onDismiss,
                modifier = Modifier.testTag(RoomsTestTags.JOIN_CONFIRM),
                confirmText = stringResource(R.string.rooms_join),
                confirmEnabled = state.hasValidIdentityInput(),
                isConfirmLoading = state.isSubmitting
            )
        }
    ) {
        SheetHeader(
            title = room.resolvedDisplayName(),
            subtitle = stringResource(R.string.rooms_join_description)
        )
        Text(room.description, color = LocalZibeExtendedColors.current.hintText)
        RoomMetadata(Icons.Default.Groups, participantText)
        RoomIdentitySelector(
            state = state,
            onIdentitySelected = onIdentitySelected,
            onAliasChanged = onAliasChanged
        )
    }
}

@Composable
private fun PublicCreatorIdentity(state: GroupsUiState) {
    val colors = LocalZibeExtendedColors.current
    Text(
        text = stringResource(R.string.rooms_creator_identity),
        style = MaterialTheme.typography.titleMedium,
        color = colors.lightText,
        fontWeight = FontWeight.SemiBold
    )
    PublicIdentitySummary(
        state = state,
        supportingText = stringResource(R.string.rooms_creator_identity_supporting)
    )
}

@Composable
private fun RoomIdentitySelector(
    state: GroupsUiState,
    onIdentitySelected: (RoomIdentityType) -> Unit,
    onAliasChanged: (String) -> Unit
) {
    val colors = LocalZibeExtendedColors.current
    Text(
        text = stringResource(R.string.rooms_identity),
        style = MaterialTheme.typography.titleMedium,
        color = colors.lightText,
        fontWeight = FontWeight.SemiBold
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = state.identityType == RoomIdentityType.PUBLIC,
            onClick = { onIdentitySelected(RoomIdentityType.PUBLIC) },
            enabled = !state.isSubmitting,
            label = { Text(stringResource(R.string.rooms_identity_public)) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        FilterChip(
            selected = state.identityType == RoomIdentityType.ANONYMOUS,
            onClick = { onIdentitySelected(RoomIdentityType.ANONYMOUS) },
            enabled = !state.isSubmitting,
            label = { Text(stringResource(R.string.rooms_identity_anonymous)) },
            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) }
        )
    }
    if (state.identityType == RoomIdentityType.PUBLIC) {
        PublicIdentitySummary(
            state = state,
            supportingText = stringResource(R.string.rooms_identity_public_supporting)
        )
    } else {
        Text(
            text = stringResource(R.string.rooms_identity_anonymous_supporting),
            style = MaterialTheme.typography.bodySmall,
            color = colors.hintText
        )
        ZibeInputField(
            value = state.alias,
            label = stringResource(R.string.rooms_alias),
            onValueChange = onAliasChanged,
            enabled = !state.isSubmitting,
            error = state.identityError?.asString()
        )
        Text(
            text = stringResource(R.string.rooms_alias_supporting),
            style = MaterialTheme.typography.bodySmall,
            color = colors.hintText
        )
    }
    if (state.identityType == RoomIdentityType.PUBLIC) {
        state.identityError?.let {
            Text(
                text = it.asString(),
                style = MaterialTheme.typography.bodySmall,
                color = colors.snackRed
            )
        }
    }
}

@Composable
private fun PublicIdentitySummary(state: GroupsUiState, supportingText: String) {
    val colors = LocalZibeExtendedColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model = state.publicIdentityPhotoUrl,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.publicIdentityName,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.lightText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.hintText
            )
        }
    }
}

private fun GroupsUiState.hasValidIdentityInput(): Boolean =
    when (identityType) {
        RoomIdentityType.PUBLIC -> publicIdentityName.isNotBlank()
        RoomIdentityType.ANONYMOUS -> alias.isNotBlank()
    }
