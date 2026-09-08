package com.zibete.proyecto1.ui.groups.host

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.domain.roomsv2.RoomV2Conversation
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Message
import com.zibete.proyecto1.domain.roomsv2.RoomV2Report
import com.zibete.proyecto1.domain.roomsv2.RoomV2Role

@Composable
fun RoomV2HostRoute(viewModel: RoomV2HostViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RoomV2HostScreen(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onDraftChanged = viewModel::onDraftChanged,
        onSend = viewModel::sendCurrentText,
        onOpenPrivate = viewModel::openPrivate,
        onConversationSelected = viewModel::selectConversation,
        onBackToPrivates = viewModel::backToPrivateList,
        onLeaveRequested = viewModel::onLeaveRequested,
        onDismissLeave = viewModel::dismissLeave,
        onConfirmLeave = viewModel::confirmLeave,
        onNotificationsChanged = viewModel::setNotifications,
        onBlockPrivate = viewModel::blockCurrentPrivate,
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
    onTabSelected: (RoomV2HostTab) -> Unit,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onOpenPrivate: (RoomV2Identity) -> Unit,
    onConversationSelected: (String) -> Unit,
    onBackToPrivates: () -> Unit,
    onLeaveRequested: () -> Unit,
    onDismissLeave: () -> Unit,
    onConfirmLeave: () -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onBlockPrivate: (Boolean) -> Unit,
    onCloseRoom: () -> Unit,
    onTransferOwnership: (String) -> Unit,
    onSetModerator: (String, Boolean) -> Unit,
    onRemoveMember: (String, Boolean) -> Unit,
    onRemoveMessage: (String) -> Unit,
    onReportMessage: (RoomV2Message, String) -> Unit,
    onResolveReport: (String, String) -> Unit,
) {
    var reportTarget by remember { mutableStateOf<RoomV2Message?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var resolutionTarget by remember { mutableStateOf<RoomV2Report?>(null) }
    var resolutionText by remember { mutableStateOf("") }

    Scaffold { padding ->
        when {
            state.isLoading && state.room == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            state.error != null && state.room == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.error.asString())
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                RoomHeader(
                    state = state,
                    onLeaveRequested = onLeaveRequested,
                    onNotificationsChanged = onNotificationsChanged,
                    onCloseRoom = onCloseRoom,
                )
                HostTabs(state = state, onTabSelected = onTabSelected)
                when (state.selectedTab) {
                    RoomV2HostTab.CHAT -> ThreadContent(
                        state = state,
                        onDraftChanged = onDraftChanged,
                        onSend = onSend,
                        onReport = { reportTarget = it },
                        onRemove = onRemoveMessage,
                    )

                    RoomV2HostTab.PEOPLE -> PeopleContent(
                        state = state,
                        onOpenPrivate = onOpenPrivate,
                        onTransferOwnership = onTransferOwnership,
                        onSetModerator = onSetModerator,
                        onRemoveMember = onRemoveMember,
                    )

                    RoomV2HostTab.PRIVATES -> PrivateContent(
                        state = state,
                        onConversationSelected = onConversationSelected,
                        onBackToPrivates = onBackToPrivates,
                        onDraftChanged = onDraftChanged,
                        onSend = onSend,
                        onBlockPrivate = onBlockPrivate,
                        onReport = { reportTarget = it },
                    )

                    RoomV2HostTab.REPORTS -> ReportsContent(
                        reports = state.reports,
                        onResolve = { report ->
                            resolutionTarget = report
                            resolutionText = ""
                        },
                    )
                }
            }
        }
    }

    if (state.showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = onDismissLeave,
            title = {
                Text(
                    stringResource(
                        R.string.rooms_v2_leave_title,
                        state.room?.name.orEmpty(),
                    )
                )
            },
            text = { Text(stringResource(R.string.rooms_v2_leave_message)) },
            confirmButton = {
                Button(onClick = onConfirmLeave, enabled = !state.isSubmitting) {
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

    reportTarget?.let { message ->
        AlertDialog(
            onDismissRequest = {
                reportTarget = null
                reportReason = ""
            },
            title = { Text(stringResource(R.string.rooms_v2_report)) },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rooms_v2_report_reason)) },
                    minLines = 2,
                    maxLines = 5,
                )
            },
            confirmButton = {
                Button(
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
                OutlinedTextField(
                    value = resolutionText,
                    onValueChange = { resolutionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.rooms_v2_resolution)) },
                    minLines = 2,
                    maxLines = 5,
                )
            },
            confirmButton = {
                Button(
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
                TextButton(
                    onClick = {
                        resolutionTarget = null
                        resolutionText = ""
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun RoomHeader(
    state: RoomV2HostUiState,
    onLeaveRequested: () -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onCloseRoom: () -> Unit,
) {
    val room = state.room ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (room.description.isNotBlank()) {
                        Text(
                            text = room.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                TextButton(onClick = onLeaveRequested, enabled = !state.isSubmitting) {
                    Text(stringResource(R.string.rooms_v2_leave))
                }
            }
            state.membership?.let { membership ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(
                            R.string.rooms_v2_identity_as,
                            membership.identity.displayName,
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(stringResource(R.string.rooms_v2_notifications))
                    Switch(
                        checked = membership.notificationsEnabled,
                        onCheckedChange = onNotificationsChanged,
                        enabled = !state.isSubmitting,
                    )
                }
            }
            if (state.isOwner) {
                TextButton(onClick = onCloseRoom, enabled = !state.isSubmitting) {
                    Text(stringResource(R.string.rooms_v2_close))
                }
            }
        }
    }
}

@Composable
private fun HostTabs(
    state: RoomV2HostUiState,
    onTabSelected: (RoomV2HostTab) -> Unit,
) {
    val tabs = buildList {
        add(RoomV2HostTab.CHAT)
        add(RoomV2HostTab.PEOPLE)
        add(RoomV2HostTab.PRIVATES)
        if (state.canModerate) add(RoomV2HostTab.REPORTS)
    }
    val selectedIndex = tabs.indexOf(state.selectedTab).coerceAtLeast(0)
    TabRow(selectedTabIndex = selectedIndex) {
        tabs.forEach { tab ->
            Tab(
                selected = state.selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        when (tab) {
                            RoomV2HostTab.CHAT -> stringResource(R.string.rooms_v2_chat)
                            RoomV2HostTab.PEOPLE -> stringResource(R.string.rooms_v2_people)
                            RoomV2HostTab.PRIVATES -> stringResource(R.string.rooms_v2_privates)
                            RoomV2HostTab.REPORTS -> stringResource(R.string.rooms_v2_reports)
                        }
                    )
                },
            )
        }
    }
}

@Composable
private fun ThreadContent(
    state: RoomV2HostUiState,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onReport: (RoomV2Message) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MessageList(
            messages = state.messages,
            myIdentityId = state.myIdentityId,
            canRemove = state.canModerate,
            modifier = Modifier.weight(1f),
            onReport = onReport,
            onRemove = onRemove,
        )
        MessageComposer(
            draft = state.draft,
            submitting = state.isSubmitting,
            onDraftChanged = onDraftChanged,
            onSend = onSend,
        )
    }
}

@Composable
private fun PrivateContent(
    state: RoomV2HostUiState,
    onConversationSelected: (String) -> Unit,
    onBackToPrivates: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    onBlockPrivate: (Boolean) -> Unit,
    onReport: (RoomV2Message) -> Unit,
) {
    val conversation = state.currentConversation
    if (conversation == null) {
        if (state.conversations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.rooms_v2_no_privates))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.conversations, key = { it.conversationId }) { item ->
                    ConversationRow(item = item, onClick = {
                        onConversationSelected(item.conversationId)
                    })
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBackToPrivates) {
                Text(stringResource(R.string.rooms_v2_back_to_privates))
            }
            Text(
                conversation.otherIdentity.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(
                onClick = { onBlockPrivate(!conversation.blocked) },
                enabled = !state.isSubmitting,
            ) {
                Text(
                    stringResource(
                        if (conversation.blocked) R.string.rooms_v2_unblock_private
                        else R.string.rooms_v2_block_private,
                    )
                )
            }
        }
        if (conversation.closed || conversation.blocked) {
            Text(
                text = stringResource(
                    if (conversation.blocked) R.string.rooms_v2_private_blocked
                    else R.string.rooms_v2_private_closed,
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        MessageList(
            messages = state.messages,
            myIdentityId = state.myIdentityId,
            canRemove = false,
            modifier = Modifier.weight(1f),
            onReport = onReport,
            onRemove = {},
        )
        MessageComposer(
            draft = state.draft,
            submitting = state.isSubmitting,
            enabled = !conversation.closed && !conversation.blocked,
            onDraftChanged = onDraftChanged,
            onSend = onSend,
        )
    }
}

@Composable
private fun ConversationRow(item: RoomV2Conversation, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.otherIdentity.displayName, fontWeight = FontWeight.Bold)
                Text(
                    item.lastText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (item.unreadCount > 0) {
                Badge { Text(item.unreadCount.coerceAtMost(99).toString()) }
            }
            if (item.closed) AssistChip(onClick = onClick, label = {
                Text(stringResource(R.string.rooms_v2_private_closed))
            })
        }
    }
}

@Composable
private fun MessageList(
    messages: List<RoomV2Message>,
    myIdentityId: String?,
    canRemove: Boolean,
    modifier: Modifier,
    onReport: (RoomV2Message) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (messages.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.rooms_v2_no_messages))
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(messages, key = { it.messageId }) { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            message.authorDisplayName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (message.authorMode == RoomV2IdentityMode.ANONYMOUS) {
                            Text(
                                stringResource(R.string.rooms_v2_anonymous_badge),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Text(
                        if (message.removed) {
                            stringResource(R.string.rooms_v2_message_removed)
                        } else {
                            message.text
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (!message.removed && message.authorIdentityId != myIdentityId) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { onReport(message) }) {
                                Text(stringResource(R.string.rooms_v2_report))
                            }
                            if (canRemove) {
                                TextButton(onClick = { onRemove(message.messageId) }) {
                                    Text(stringResource(R.string.rooms_v2_remove_message))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(
    draft: String,
    submitting: Boolean,
    enabled: Boolean = true,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.rooms_v2_message_hint)) },
            enabled = enabled && !submitting,
            maxLines = 4,
        )
        Button(
            onClick = onSend,
            enabled = enabled && !submitting && draft.isNotBlank(),
        ) {
            Text(stringResource(R.string.rooms_v2_send))
        }
    }
}

@Composable
private fun PeopleContent(
    state: RoomV2HostUiState,
    onOpenPrivate: (RoomV2Identity) -> Unit,
    onTransferOwnership: (String) -> Unit,
    onSetModerator: (String, Boolean) -> Unit,
    onRemoveMember: (String, Boolean) -> Unit,
) {
    if (state.participants.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.rooms_v2_no_participants))
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.participants, key = { it.identityId }) { identity ->
            ParticipantRow(
                state = state,
                identity = identity,
                onOpenPrivate = onOpenPrivate,
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
    onTransferOwnership: (String) -> Unit,
    onSetModerator: (String, Boolean) -> Unit,
    onRemoveMember: (String, Boolean) -> Unit,
) {
    val isMe = identity.identityId == state.myIdentityId
    val canModerateTarget = when (state.myRole) {
        RoomV2Role.OWNER -> identity.role != RoomV2Role.OWNER
        RoomV2Role.MODERATOR -> identity.role == RoomV2Role.MEMBER
        RoomV2Role.MEMBER,
        null -> false
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    identity.displayName,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when (identity.role) {
                        RoomV2Role.OWNER -> stringResource(R.string.rooms_v2_owner_badge)
                        RoomV2Role.MODERATOR -> stringResource(R.string.rooms_v2_moderator_badge)
                        RoomV2Role.MEMBER -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (!isMe) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = { onOpenPrivate(identity) }) {
                        Text(stringResource(R.string.rooms_v2_open_private))
                    }
                    if (canModerateTarget) {
                        TextButton(onClick = { onRemoveMember(identity.identityId, false) }) {
                            Text(stringResource(R.string.rooms_v2_kick))
                        }
                        TextButton(onClick = { onRemoveMember(identity.identityId, true) }) {
                            Text(stringResource(R.string.rooms_v2_ban))
                        }
                    }
                }
                if (state.isOwner && identity.role != RoomV2Role.OWNER) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TextButton(
                            onClick = {
                                onSetModerator(
                                    identity.identityId,
                                    identity.role != RoomV2Role.MODERATOR,
                                )
                            }
                        ) {
                            Text(
                                stringResource(
                                    if (identity.role == RoomV2Role.MODERATOR) {
                                        R.string.rooms_v2_remove_moderator
                                    } else {
                                        R.string.rooms_v2_make_moderator
                                    }
                                )
                            )
                        }
                        if (identity.mode == RoomV2IdentityMode.REAL) {
                            TextButton(onClick = { onTransferOwnership(identity.identityId) }) {
                                Text(stringResource(R.string.rooms_v2_transfer_owner))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsContent(
    reports: List<RoomV2Report>,
    onResolve: (RoomV2Report) -> Unit,
) {
    if (reports.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.rooms_v2_no_reports))
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(reports, key = { it.reportId }) { report ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(report.reason, fontWeight = FontWeight.Bold)
                    HorizontalDivider()
                    Text(report.evidence.authorDisplayName)
                    Text(
                        if (report.evidence.removed) {
                            stringResource(R.string.rooms_v2_message_removed)
                        } else {
                            report.evidence.text
                        },
                    )
                    if (report.status.equals("open", ignoreCase = true)) {
                        TextButton(onClick = { onResolve(report) }) {
                            Text(stringResource(R.string.rooms_v2_resolve))
                        }
                    } else if (report.resolution.isNotBlank()) {
                        Text(report.resolution, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
