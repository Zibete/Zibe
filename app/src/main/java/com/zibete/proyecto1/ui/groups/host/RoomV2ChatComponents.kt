package com.zibete.proyecto1.ui.groups.host

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.utils.TimeUtils
import com.zibete.proyecto1.domain.roomsv2.RoomV2Identity
import com.zibete.proyecto1.domain.roomsv2.RoomV2IdentityMode
import com.zibete.proyecto1.domain.roomsv2.RoomV2Message
import com.zibete.proyecto1.domain.roomsv2.RoomV2MessageKind
import com.zibete.proyecto1.ui.chat.components.ChatActionCircleButton
import com.zibete.proyecto1.ui.chat.components.ChatInfoRow
import com.zibete.proyecto1.ui.chat.components.ChatMessageTextField
import com.zibete.proyecto1.ui.components.ZibeMenuDefaults
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

data class RoomV2TopBarState(
    val title: String,
    val subtitle: String,
    val notificationsEnabled: Boolean,
    val selectedTab: RoomV2HostTab,
    val privateUnreadCount: Long,
    val reportCount: Int,
    val canModerate: Boolean,
    val isOwner: Boolean,
    val privateBlocked: Boolean? = null,
)

@Composable
fun RoomV2TopBar(
    state: RoomV2TopBarState,
    onBack: () -> Unit,
    onChat: () -> Unit,
    onPeople: () -> Unit,
    onPrivates: () -> Unit,
    onReports: () -> Unit,
    onToggleNotifications: () -> Unit,
    onLeave: () -> Unit,
    onCloseRoom: () -> Unit,
    onTogglePrivateBlock: ((Boolean) -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val colors = LocalZibeExtendedColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.content_description_back),
                tint = colors.lightText,
            )
        }

        RoomV2IdentityAvatar(
            displayName = state.title,
            mode = RoomV2IdentityMode.REAL,
            modifier = Modifier.size(42.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = onChat, onLongClick = {}),
        ) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.lightText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.lightText.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onPeople) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = stringResource(R.string.rooms_v2_people),
                tint = if (state.selectedTab == RoomV2HostTab.PEOPLE) {
                    colors.accent
                } else {
                    colors.lightText
                },
            )
        }

        Box {
            IconButton(onClick = onPrivates) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = stringResource(R.string.rooms_v2_privates),
                    tint = if (state.selectedTab == RoomV2HostTab.PRIVATES) {
                        colors.accent
                    } else {
                        colors.lightText
                    },
                )
            }
            if (state.privateUnreadCount > 0) {
                Badge(modifier = Modifier.align(Alignment.TopEnd)) {
                    Text(state.privateUnreadCount.coerceAtMost(99).toString())
                }
            }
        }

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
                    modifier = Modifier
                        .background(colors.contentDarkBg.copy(alpha = ZibeMenuDefaults.BgAlpha))
                        .padding(vertical = 4.dp),
                    containerColor = Color.Transparent,
                    shadowElevation = ZibeMenuDefaults.Shadow,
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (state.notificationsEnabled) {
                                        R.string.menu_user_notifications_off
                                    } else {
                                        R.string.menu_user_notifications_on
                                    }
                                )
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleNotifications()
                        },
                    )
                    if (state.privateBlocked != null && onTogglePrivateBlock != null) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (state.privateBlocked) {
                                            R.string.rooms_v2_unblock_private
                                        } else {
                                            R.string.rooms_v2_block_private
                                        }
                                    )
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onTogglePrivateBlock(!state.privateBlocked)
                            },
                        )
                    }
                    if (state.canModerate) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (state.reportCount > 0) {
                                        stringResource(
                                            R.string.rooms_v2_reports_with_count,
                                            state.reportCount,
                                        )
                                    } else {
                                        stringResource(R.string.rooms_v2_reports)
                                    }
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onReports()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rooms_v2_leave)) },
                        onClick = {
                            menuExpanded = false
                            onLeave()
                        },
                    )
                    if (state.isOwner) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rooms_v2_close)) },
                            onClick = {
                                menuExpanded = false
                                onCloseRoom()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RoomV2MessageTimeline(
    messages: List<RoomV2Message>,
    myIdentityId: String?,
    participants: List<RoomV2Identity>,
    canRemove: Boolean,
    threadKey: String,
    isLoadingEarlier: Boolean,
    hasEarlierMessages: Boolean,
    modifier: Modifier = Modifier,
    onLoadEarlier: () -> Unit,
    onMessageLongPress: (RoomV2Message, Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    val participantsById = remember(participants) { participants.associateBy { it.identityId } }
    val timeline = remember(messages) { buildRoomTimeline(messages) }
    val latestSeq = messages.lastOrNull()?.seq ?: 0L
    var initialScrollDone by remember(threadKey) { mutableStateOf(false) }
    var previousLatestSeq by remember(threadKey) { mutableLongStateOf(0L) }

    LaunchedEffect(threadKey, latestSeq, timeline.size) {
        if (timeline.isEmpty()) return@LaunchedEffect
        if (!initialScrollDone) {
            delay(40)
            listState.scrollToItem(timeline.lastIndex)
            initialScrollDone = true
        } else if (latestSeq > previousLatestSeq) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val nearBottom = lastVisible >= (timeline.lastIndex - 2).coerceAtLeast(0)
            if (nearBottom) {
                listState.animateScrollToItem(timeline.lastIndex)
            }
        }
        previousLatestSeq = latestSeq
    }

    LaunchedEffect(listState, threadKey, hasEarlierMessages, isLoadingEarlier) {
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.isScrollInProgress
        }
            .distinctUntilChanged()
            .collect { (firstVisible, scrolling) ->
                if (
                    scrolling &&
                    firstVisible <= 2 &&
                    hasEarlierMessages &&
                    !isLoadingEarlier
                ) {
                    onLoadEarlier()
                }
            }
    }

    if (messages.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.rooms_v2_no_messages),
                color = LocalZibeExtendedColors.current.lightText.copy(alpha = 0.8f),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (isLoadingEarlier) {
            item(key = "history_loading_$threadKey") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        items(timeline, key = { it.key }) { item ->
            when (item) {
                is RoomV2TimelineItem.Date -> ChatInfoRow(text = item.label)
                is RoomV2TimelineItem.Event -> ChatInfoRow(text = item.message.text)
                is RoomV2TimelineItem.Message -> {
                    val message = item.message
                    val isMine = message.authorIdentityId == myIdentityId
                    RoomV2MessageRow(
                        message = message,
                        isMine = isMine,
                        identity = participantsById[message.authorIdentityId],
                        canRemove = canRemove,
                        onLongPress = { onMessageLongPress(message, isMine) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomV2MessageRow(
    message: RoomV2Message,
    isMine: Boolean,
    identity: RoomV2Identity?,
    canRemove: Boolean,
    onLongPress: () -> Unit,
) {
    val colors = LocalZibeExtendedColors.current
    val alignment = if (isMine) Arrangement.End else Arrangement.Start
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMine) 18.dp else 0.dp,
        bottomEnd = if (isMine) 0.dp else 18.dp,
    )
    val bubbleBrush = if (isMine) {
        Brush.linearGradient(listOf(colors.blueBubble, Color(0xFF5E6DFD)))
    } else {
        Brush.linearGradient(listOf(colors.pinkBubble, Color(0xFFFF6F91)))
    }
    val hasActions = canRemove || !isMine
    val resolvedMode = identity?.mode ?: message.authorMode

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = alignment,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMine) {
            RoomV2IdentityAvatar(
                displayName = message.authorDisplayName,
                mode = resolvedMode,
                modifier = Modifier
                    .size(38.dp)
                    .padding(bottom = 2.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 310.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.authorDisplayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.lightText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (message.authorMode == RoomV2IdentityMode.ANONYMOUS) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.rooms_v2_anonymous_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.lightText.copy(alpha = 0.65f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Box(
                    modifier = Modifier
                        .clip(bubbleShape)
                        .background(bubbleBrush)
                        .border(1.dp, Color.Black.copy(alpha = 0.2f), bubbleShape)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { if (hasActions) onLongPress() },
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (message.removed) {
                            stringResource(R.string.rooms_v2_message_removed)
                        } else {
                            message.text
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = if (message.removed) 0.7f else 1f),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = TimeUtils.formatHour(message.sentAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.lightText.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
        }

        if (isMine) {
            Spacer(modifier = Modifier.width(6.dp))
            RoomV2IdentityAvatar(
                displayName = message.authorDisplayName,
                mode = resolvedMode,
                modifier = Modifier
                    .size(38.dp)
                    .padding(bottom = 2.dp),
            )
        }
    }
}

@Composable
fun RoomV2IdentityAvatar(
    displayName: String,
    mode: RoomV2IdentityMode,
    modifier: Modifier = Modifier,
) {
    val colors = LocalZibeExtendedColors.current
    val initials = remember(displayName) {
        displayName
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "?" }
    }
    val borderColor = if (mode == RoomV2IdentityMode.ANONYMOUS) {
        colors.pinkBubble
    } else {
        colors.accent
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.contentDarkBg.copy(alpha = 0.92f))
            .border(1.dp, borderColor.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.lightText,
        )
    }
}

@Composable
fun RoomV2TextComposer(
    draft: String,
    submitting: Boolean,
    enabled: Boolean = true,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        ChatMessageTextField(
            value = draft,
            onValueChange = onDraftChanged,
            placeholder = stringResource(R.string.rooms_v2_message_hint),
            modifier = Modifier.weight(1f),
            enabled = enabled && !submitting,
            singleLine = true,
        )
        Spacer(modifier = Modifier.width(6.dp))
        ChatActionCircleButton(
            iconVector = Icons.AutoMirrored.Rounded.Send,
            enabled = enabled && !submitting && draft.isNotBlank(),
            isLoading = submitting,
            onClick = onSend,
        )
    }
}

private sealed interface RoomV2TimelineItem {
    val key: String

    data class Date(val label: String, override val key: String) : RoomV2TimelineItem
    data class Event(val message: RoomV2Message) : RoomV2TimelineItem {
        override val key: String = "event_${message.messageId}"
    }
    data class Message(val message: RoomV2Message) : RoomV2TimelineItem {
        override val key: String = "message_${message.messageId}"
    }
}

private fun buildRoomTimeline(messages: List<RoomV2Message>): List<RoomV2TimelineItem> {
    val result = mutableListOf<RoomV2TimelineItem>()
    var lastDate: String? = null
    messages.sortedBy { it.seq }.forEach { message ->
        val date = TimeUtils.formatHeaderDate(message.sentAt)
        if (date != lastDate) {
            result += RoomV2TimelineItem.Date(
                label = date,
                key = "date_${message.sentAt}_$date",
            )
            lastDate = date
        }
        result += if (message.kind == RoomV2MessageKind.EVENT) {
            RoomV2TimelineItem.Event(message)
        } else {
            RoomV2TimelineItem.Message(message)
        }
    }
    return result
}
