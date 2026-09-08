package com.zibete.proyecto1.ui.groups.host

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.domain.roomsv2.RoomV2Conversation
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Message
import com.zibete.proyecto1.domain.roomsv2.RoomV2Report
import com.zibete.proyecto1.domain.roomsv2.RoomV2Role
import com.zibete.proyecto1.ui.components.ZibeMenuDefaults
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors

@Composable
fun RoomV2HostRoute(
    viewModel: RoomV2HostViewModel,
    onOpenProfile: (RoomV2Identity) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoomV2HostScreen(
        state = state,
        onBack = viewModel::onBackRequested,
        onTabSelected = viewModel::onTabSelected,
        onDraftChanged = viewModel::onDraftChanged,
        onSend = viewModel::sendCurrentText,
        onOpenPrivate = viewModel::openPrivate,
        onOpenProfile = onOpenProfile,
        onConversationSelected = viewModel::selectConversation,
        onNotificationsChanged = viewModel::setNotifications,
        onBlockPrivate = viewModel::blockCurrentPrivate,
        onLeaveRequested = viewModel::onLeaveRequested,
        onDismissLeave = viewModel::dismissLeave,
        onConfirmLeave = viewModel::confirmLeave,
        onCloseRoom = viewModel::closeRoom,
        onTransferOwnership = viewModel::transferOwnership,
        onSetModerator = viewModel::setModerator,
        onRemoveMember = viewModel::removeMember,
        onRemoveMessage = viewModel::removeMessage,
        onReportMessage = viewModel::reportMessage,
        onResolveReport = viewModel::resolveReport,
    )
}

@Composable
fun RoomV2HostScreen(
    state: RoomV2HostUiState,
    onBack: () -> Unit,
    onTabSelected: (RoomV2HostTab) -> Unit,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onOpenPrivate: (RoomV2Identity) -> Unit,
    onOpenProfile: (RoomV2Identity) -> Unit,
    onConversationSelected: (String) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onBlockPrivate: (Boolean) -> Unit,
    onLeaveRequested: () -> Unit,
    onDismissLeave: () -> Unit,
    onConfirmLeave: () -> Unit,
    onCloseRoom: () -> Unit,
    onTransferOwnership: (String) -> Unit,
    onSetModerator: (String, Boolean) -> Unit,
    onRemoveMember: (String, Boolean) -> Unit,
    onRemoveMessage: (String) -> Unit,
    onReportMessage: (RoomV2Message, String) -> Unit,
    onResolveReport: (String, String) -> Unit,
) {
    val colors = LocalZibeExtendedColors.current
    val room = state.room
    val currentPrivate = state.currentConversation
    var showCloseConfirm by remember { mutableStateOf(false) }
    var messageActionTarget by remember { mutableStateOf<Pair<RoomV2Message, Boolean>?>(null) }
    var reportTarget by remember { mutableStateOf<RoomV2Message?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var resolutionTarget by remember { mutableStateOf<RoomV2Report?>(null) }
    var resolutionText by remember { mutableStateOf("") }

    val privateUnread = state.conversations.sumOf { it.unreadCount }
    val topTitle = if (
        state.selectedTab == RoomV2HostTab.PRIVATES && currentPrivate != null
    ) {
        currentPrivate.otherIdentity.displayName
    } else {
        room?.name.orEmpty()
    }
    val topSubtitle = when {
        state.selectedTab == RoomV2HostTab.PRIVATES && currentPrivate != null -> {
            stringResource(R.string.rooms_v2_private_context, room?.name.orEmpty())
        }
        state.selectedTab == RoomV2HostTab.REPORTS -> {
            stringResource(R.string.rooms_v2_moderation)
        }
        else -> stringResource(R.string.rooms_v2_participants, room?.memberCount ?: 0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.gradientZibe),
    ) {
        when {
            state.isLoading && room == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null && room == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.error.asString(),
                        color = colors.lightText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            room != null -> {
                val composerVisible = state.selectedTab == RoomV2HostTab.CHAT ||
                    (state.selectedTab == RoomV2HostTab.PRIVATES && currentPrivate != null)
                val composerEnabled = when {
                    state.selectedTab == RoomV2HostTab.CHAT -> true
                    currentPrivate == null -> false
                    else -> !currentPrivate.closed && !currentPrivate.blocked
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        RoomV2TopBar(
                            state = RoomV2TopBarState(
                                title = topTitle,
                                subtitle = topSubtitle,
                                notificationsEnabled = state.membership?.notificationsEnabled ?: true,
                                selectedTab = state.selectedTab,
                                privateUnreadCount = privateUnread,
                                reportCount = state.reports.count {
                                    it.status.equals("open", ignoreCase = true)
                                },
                                canModerate = state.canModerate,
                                isOwner = state.isOwner,
                                privateBlocked = currentPrivate?.blocked,
                            ),
                            onBack = onBack,
                            onChat = { onTabSelected(RoomV2HostTab.CHAT) },
                            onPeople = { onTabSelected(RoomV2HostTab.PEOPLE) },
                            onPrivates = { onTabSelected(RoomV2HostTab.PRIVATES) },
                            onReports = { onTabSelected(RoomV2HostTab.REPORTS) },
                            onToggleNotifications = {
                                onNotificationsChanged(
                                    !(state.membership?.notificationsEnabled ?: true)
                                )
                            },
                            onLeave = onLeaveRequested,
                            onCloseRoom = { showCloseConfirm = true },
                            onTogglePrivateBlock = if (currentPrivate != null) {
                                onBlockPrivate
                            } else {
                                null
                            },
                        )
                    },
                    bottomBar = {
                        if (composerVisible) {
                            Column {
                                if (currentPrivate?.closed == true || currentPrivate?.blocked == true) {
                                    PrivateStateBanner(currentPrivate)
                                }
                                RoomV2TextComposer(
                                    draft = state.draft,
                                    submitting = state.isSubmitting,
                                    enabled = composerEnabled,
                                    onDraftChanged = onDraftChanged,
                                    onSend = onSend,
                                )
                            }
                        }
                    },
                ) { padding ->
                    when (state.selectedTab) {
                        RoomV2HostTab.CHAT -> RoomV2MessageTimeline(
                            messages = state.messages,
                            myIdentityId = state.myIdentityId,
                            participants = state.participants,
                            canRemove = state.canModerate,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            onMessageLongPress = { message, isMine ->
                                messageActionTarget = message to isMine
                            },
                        )

                        RoomV2HostTab.PEOPLE -> PeoplePane(
                            state = state,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            onOpenPrivate = onOpenPrivate,
                            onOpenProfile = onOpenProfile,
                            onTransferOwnership = onTransferOwnership,
                            onSetModerator = onSetModerator,
                            onRemoveMember = onRemoveMember,
                        )

                        RoomV2HostTab.PRIVATES -> PrivatesPane(
                            state = state,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            onConversationSelected = onConversationSelected,
                            onMessageLongPress = { message, isMine ->
                                messageActionTarget = message to isMine
                            },
                        )

                        RoomV2HostTab.REPORTS -> ReportsPane(
                            reports = state.reports,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            onResolve = { report ->
                                resolutionTarget = report
                                resolutionText = ""
                            },
                        )
                    }
                }
            }
        }
    }

    if (state.showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = onDismissLeave,
            title = {
                Text(stringResource(R.string.rooms_v2_leave_title, room?.name.orEmpty()))
            },
            text = { Text(stringResource(R.string.rooms_v2_leave_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmLeave, enabled = !state.isSubmitting) {
                    Text(stringResource(R.string.rooms_v2_leave))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLeave, enabled = !state.isSubmitting) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text(stringResource(R.string.rooms_v2_close_title)) },
            text = { Text(stringResource(R.string.rooms_v2_close_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloseConfirm = false
                        onCloseRoom()
                    },
                    enabled = !state.isSubmitting,
                ) {
                    Text(stringResource(R.string.rooms_v2_close))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    messageActionTarget?.let { (message, isMine) ->
        AlertDialog(
            onDismissRequest = { messageActionTarget = null },
            title = { Text(message.authorDisplayName) },
            text = {
                Text(
                    if (message.removed) {
                        stringResource(R.string.rooms_v2_message_removed)
                    } else {
                        message.text
                    }
                )
            },
            confirmButton = {
                if (!isMine && !message.removed) {
                    TextButton(
                        onClick = {
                            messageActionTarget = null
                            reportTarget = message
                        }
                    ) {
                        Text(stringResource(R.string.rooms_v2_report))
                    }
                }
            },
            dismissButton = {
                Row {
                    if (state.canModerate && !message.removed && message.conversationId == null) {
                        TextButton(
                            onClick = {
                                messageActionTarget = null
                                onRemoveMessage(message.messageId)
                            }
                        ) {
                            Text(stringResource(R.string.rooms_v2_remove_message))
                        }
                    }
                    TextButton(onClick = { messageActionTarget = null }) {
                        Text(stringResource(R.string.close))
                    }
                }
            },
        )
    }

    reportTarget?.let { message ->
        AlertDialog(
            onDismissRequest = {
                reportTarget = null
                reportReason = ""
            },
            title = { Text(stringResource(R.string.rooms_v2_report)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rooms_v2_report_reason)) },
                    minLines = 2,
                    maxLines = 5,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReportMessage(message, reportReason)
                        reportTarget = null
                        reportReason = ""
                    },
                    enabled = reportReason.trim().length >= 3,
                ) {
                    Text(stringResource(R.string.rooms_v2_report))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        reportTarget = null
                        reportReason = ""
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    resolutionTarget?.let { report ->
        AlertDialog(
            onDismissRequest = {
                resolutionTarget = null
                resolutionText = ""
            },
            title = { Text(stringResource(R.string.rooms_v2_resolve)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = resolutionText,
                    onValueChange = { resolutionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rooms_v2_resolution)) },
                    minLines = 2,
                    maxLines = 5,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResolveReport(report.reportId, resolutionText)
                        resolutionTarget = null
                        resolutionText = ""
                    },
                    enabled = resolutionText.isNotBlank(),
                ) {
                    Text(stringResource(R.string.rooms_v2_resolve))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    resolutionTarget = null
                    resolutionText = ""
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun PeoplePane(
    state: RoomV2HostUiState,
    modifier: Modifier,
    onOpenPrivate: (RoomV2Identity) -> Unit,
    onOpenProfile: (RoomV2Identity) -> Unit,
    onTransferOwnership: (String) -> Unit,
    onSetModerator: (String, Boolean) -> Unit,
    onRemoveMember: (String, Boolean) -> Unit,
) {
    val colors = LocalZibeExtendedColors.current
    if (state.participants.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.rooms_v2_no_participants),
                color = colors.lightText.copy(alpha = 0.8f),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.participants, key = { it.identityId }) { identity ->
            ParticipantRow(
                state = state,
                identity = identity,
                onOpenPrivate = onOpenPrivate,
                onOpenProfile = onOpenProfile,
                onTransferOwnership = onTransferOwnership,
                onSetModerator = onSetModerator,
                onRemoveMember = onRemoveMember,
            )
        }
    }
}

@Composable
private fun ParticipantRow(
    state: RoomV2HostUiState,
    identity: RoomV2Identity,
    onOpenPrivate: (RoomV2Identity) -> Unit,
    onOpenProfile: (RoomV2Identity) -> Unit,
    onTransferOwnership: (String) -> Unit,
    onSetModerator: (String, Boolean) -> Unit,
    onRemoveMember: (String, Boolean) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val colors = LocalZibeExtendedColors.current
    val isMe = identity.identityId == state.myIdentityId
    val canModerateTarget = when (state.myRole) {
        RoomV2Role.OWNER -> identity.role != RoomV2Role.OWNER
        RoomV2Role.MODERATOR -> identity.role == RoomV2Role.MEMBER
        RoomV2Role.MEMBER,
        null -> false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colors.contentDarkBg.copy(alpha = 0.66f),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoomV2IdentityAvatar(
            displayName = identity.displayName,
            mode = identity.mode,
            modifier = Modifier.padding(2.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isMe) {
                    stringResource(R.string.rooms_v2_you_name, identity.displayName)
                } else {
                    identity.displayName
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.lightText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = identitySubtitle(identity),
                style = MaterialTheme.typography.bodySmall,
                color = colors.lightText.copy(alpha = 0.7f),
            )
        }
        if (!isMe || canModerateTarget || state.isOwner) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.content_description_more_options),
                        tint = colors.lightText,
                    )
                }
                MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(
                        extraSmall = RoundedCornerShape(ZibeMenuDefaults.Corner)
                    )
                ) {
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        offset = ZibeMenuDefaults.Offset,
                        modifier = Modifier.background(colors.contentDarkBg),
                        containerColor = Color.Transparent,
                    ) {
                        if (!isMe && identity.mode == RoomV2IdentityMode.REAL) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rooms_v2_view_profile)) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenProfile(identity)
                                },
                            )
                        }
                        if (!isMe) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rooms_v2_open_private)) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenPrivate(identity)
                                },
                            )
                        }
                        if (canModerateTarget) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rooms_v2_kick)) },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveMember(identity.identityId, false)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rooms_v2_ban)) },
                                onClick = {
                                    menuExpanded = false
                                    onRemoveMember(identity.identityId, true)
                                },
                            )
                        }
                        if (state.isOwner && !isMe && identity.role != RoomV2Role.OWNER) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (identity.role == RoomV2Role.MODERATOR) {
                                                R.string.rooms_v2_remove_moderator
                                            } else {
                                                R.string.rooms_v2_make_moderator
                                            }
                                        )
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSetModerator(
                                        identity.identityId,
                                        identity.role != RoomV2Role.MODERATOR,
                                    )
                                },
                            )
                            if (identity.mode == RoomV2IdentityMode.REAL) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.rooms_v2_transfer_owner))
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onTransferOwnership(identity.identityId)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun identitySubtitle(identity: RoomV2Identity): String {
    val role = when (identity.role) {
        RoomV2Role.OWNER -> stringResource(R.string.rooms_v2_owner_badge)
        RoomV2Role.MODERATOR -> stringResource(R.string.rooms_v2_moderator_badge)
        RoomV2Role.MEMBER -> stringResource(R.string.rooms_v2_member_badge)
    }
    return if (identity.mode == RoomV2IdentityMode.ANONYMOUS) {
        "$role · ${stringResource(R.string.rooms_v2_anonymous_badge)}"
    } else {
        role
    }
}

@Composable
private fun PrivatesPane(
    state: RoomV2HostUiState,
    modifier: Modifier,
    onConversationSelected: (String) -> Unit,
    onMessageLongPress: (RoomV2Message, Boolean) -> Unit,
) {
    val conversation = state.currentConversation
    val colors = LocalZibeExtendedColors.current
    if (conversation != null) {
        RoomV2MessageTimeline(
            messages = state.messages,
            myIdentityId = state.myIdentityId,
            participants = state.participants,
            canRemove = false,
            modifier = modifier,
            onMessageLongPress = onMessageLongPress,
        )
        return
    }

    if (state.conversations.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.rooms_v2_no_privates),
                color = colors.lightText.copy(alpha = 0.8f),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.conversations, key = { it.conversationId }) { item ->
            ConversationRow(item = item, onClick = {
                onConversationSelected(item.conversationId)
            })
        }
    }
}

@Composable
private fun ConversationRow(item: RoomV2Conversation, onClick: () -> Unit) {
    val colors = LocalZibeExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.contentDarkBg.copy(alpha = 0.66f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoomV2IdentityAvatar(
            displayName = item.otherIdentity.displayName,
            mode = item.otherIdentity.mode,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
        ) {
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(0.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        item.otherIdentity.displayName,
                        color = colors.lightText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.lastText.ifBlank {
                            stringResource(R.string.rooms_v2_private_empty_preview)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.lightText.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (item.unreadCount > 0) {
            Badge { Text(item.unreadCount.coerceAtMost(99).toString()) }
        }
    }
}

@Composable
private fun PrivateStateBanner(conversation: RoomV2Conversation) {
    val colors = LocalZibeExtendedColors.current
    Text(
        text = stringResource(
            if (conversation.blocked) {
                R.string.rooms_v2_private_blocked
            } else {
                R.string.rooms_v2_private_closed
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.contentDarkBg.copy(alpha = 0.78f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = colors.lightText,
    )
}

@Composable
private fun ReportsPane(
    reports: List<RoomV2Report>,
    modifier: Modifier,
    onResolve: (RoomV2Report) -> Unit,
) {
    val colors = LocalZibeExtendedColors.current
    if (reports.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.rooms_v2_no_reports),
                color = colors.lightText.copy(alpha = 0.8f),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(reports, key = { it.reportId }) { report ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.contentDarkBg.copy(alpha = 0.72f),
                        RoundedCornerShape(18.dp),
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(report.reason, color = colors.lightText, fontWeight = FontWeight.Bold)
                HorizontalDivider(color = colors.lightText.copy(alpha = 0.16f))
                Text(
                    report.evidence.authorDisplayName,
                    color = colors.lightText.copy(alpha = 0.8f),
                )
                Text(
                    if (report.evidence.removed) {
                        stringResource(R.string.rooms_v2_message_removed)
                    } else {
                        report.evidence.text
                    },
                    color = colors.lightText,
                )
                if (report.status.equals("open", ignoreCase = true)) {
                    TextButton(onClick = { onResolve(report) }) {
                        Text(stringResource(R.string.rooms_v2_resolve))
                    }
                } else if (report.resolution.isNotBlank()) {
                    Text(
                        report.resolution,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.lightText.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}
