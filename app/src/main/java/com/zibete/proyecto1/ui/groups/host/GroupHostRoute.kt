package com.zibete.proyecto1.ui.groups.host

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.NODE_GROUP_DM
import com.zibete.proyecto1.core.ui.SnackBarManagerEntryPoint
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.profile.ProfileActivity
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupHostRoute(
    groupHostViewModel: GroupHostViewModel,
    onExitRequested: () -> Unit
) {
    val state by groupHostViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContext = context.applicationContext
    val snackBarManager = remember(appContext) {
        EntryPointAccessors.fromApplication(
            appContext,
            SnackBarManagerEntryPoint::class.java
        ).snackBarManager()
    }

    DisposableEffect(lifecycleOwner, groupHostViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> groupHostViewModel.onScreenStarted()
                Lifecycle.Event.ON_STOP -> groupHostViewModel.onScreenStopped()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            groupHostViewModel.onScreenStarted()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            groupHostViewModel.onScreenStopped()
        }
    }

    LaunchedEffect(groupHostViewModel, context) {
        groupHostViewModel.events.collect { event ->
            when (event) {
                is GroupHostEvent.OpenPrivateChat -> context.startActivity(
                    Intent(context, ChatActivity::class.java).apply {
                        putExtra(EXTRA_CHAT_ID, event.otherUid)
                        putExtra(EXTRA_CHAT_NODE, NODE_GROUP_DM)
                    }
                )
                is GroupHostEvent.OpenProfile -> context.startActivity(
                    Intent(context, ProfileActivity::class.java).apply {
                        putExtra(EXTRA_USER_ID, event.userId)
                    }
                )
                is GroupHostEvent.ShowSnack -> snackBarManager.show(event.message, event.type)
            }
        }
    }

    GroupHostScreen(
        state = state,
        onTabSelected = groupHostViewModel::onTabSelected,
        onComposerChanged = groupHostViewModel::onComposerChanged,
        onSendText = groupHostViewModel::sendCurrentText,
        onSendPhoto = groupHostViewModel::sendPhoto,
        onRetrySend = groupHostViewModel::retryFailedSend,
        onDismissSendError = groupHostViewModel::dismissFailedSend,
        onMemberSelected = groupHostViewModel::onMemberSelected,
        onDismissMemberActions = groupHostViewModel::dismissMemberActions,
        onOpenProfile = groupHostViewModel::openSelectedMemberProfile,
        onOpenMemberPrivateChat = groupHostViewModel::openSelectedMemberPrivateChat,
        onOpenPrivateConversation = groupHostViewModel::openPrivateConversation,
        onRetryLoad = groupHostViewModel::retryLoad,
        onExitRequested = onExitRequested
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GroupHostScreen(
    state: GroupHostUiState,
    onTabSelected: (GroupHostTab) -> Unit,
    onComposerChanged: (String) -> Unit,
    onSendText: () -> Unit,
    onSendPhoto: (String) -> Unit,
    onRetrySend: () -> Unit,
    onDismissSendError: () -> Unit,
    onMemberSelected: (com.zibete.proyecto1.model.UserGroup) -> Unit,
    onDismissMemberActions: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenMemberPrivateChat: () -> Unit,
    onOpenPrivateConversation: (com.zibete.proyecto1.model.Conversation) -> Unit,
    onRetryLoad: () -> Unit,
    onExitRequested: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = LocalZibeExtendedColors.current
    val pagerState = rememberPagerState(
        initialPage = GroupHostTab.GROUP_CHAT.pageIndex,
        pageCount = { GroupHostTab.entries.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        onTabSelected(groupHostTabFromPage(pagerState.currentPage))
    }
    LaunchedEffect(state.selectedTab) {
        val target = state.selectedTab.pageIndex
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.gradientZibe)
    ) {
        Column(Modifier.fillMaxSize()) {
            RoomHeader(
                state = state,
                onExitRequested = onExitRequested
            )
            RoomTabs(
                selectedTab = state.selectedTab,
                publicUnread = state.publicUnread,
                privateUnread = state.privateUnread,
                onSelected = { tab ->
                    scope.launch { pagerState.animateScrollToPage(tab.pageIndex) }
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            Box(Modifier.weight(1f)) {
                when {
                    state.isLoading -> RoomLoading()
                    state.loadError -> RoomLoadError(onRetryLoad)
                    else -> HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (groupHostTabFromPage(page)) {
                            GroupHostTab.USERS -> GroupUsersTab(
                                state = state,
                                onUserClick = onMemberSelected,
                                onDismissActions = onDismissMemberActions,
                                onOpenProfile = onOpenProfile,
                                onOpenPrivateChat = onOpenMemberPrivateChat
                            )
                            GroupHostTab.GROUP_CHAT -> GroupChatTab(
                                state = state,
                                onTextChanged = onComposerChanged,
                                onSendText = onSendText,
                                onSendPhoto = onSendPhoto,
                                onRetrySend = onRetrySend,
                                onDismissSendError = onDismissSendError
                            )
                            GroupHostTab.PRIVATE_CHATS -> GroupPrivateChatsTab(
                                state = state,
                                onConversationClick = onOpenPrivateConversation
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomHeader(
    state: GroupHostUiState,
    onExitRequested: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.roomDisplayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.roomDescription.isNotBlank()) {
                    Text(
                        text = state.roomDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = if (state.users.size == 1) {
                        stringResource(R.string.rooms_one_participant)
                    } else {
                        stringResource(R.string.rooms_participants_count, state.users.size)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onExitRequested) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                    contentDescription = stringResource(R.string.rooms_leave)
                )
            }
        }
    }
}

@Composable
private fun RoomTabs(
    selectedTab: GroupHostTab,
    publicUnread: Int,
    privateUnread: Int,
    onSelected: (GroupHostTab) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.pageIndex,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    ) {
        GroupHostTab.entries.forEach { tab ->
            val unread = when (tab) {
                GroupHostTab.GROUP_CHAT -> publicUnread
                GroupHostTab.PRIVATE_CHATS -> privateUnread
                GroupHostTab.USERS -> 0
            }
            Tab(
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                text = {
                    BadgedBox(
                        badge = {
                            if (unread > 0) {
                                Badge { Text(unread.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Text(stringResource(tab.labelRes))
                    }
                }
            )
        }
    }
}

@Composable
private fun RoomLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            Text(stringResource(R.string.rooms_loading))
        }
    }
}

@Composable
private fun RoomLoadError(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.rooms_error_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(R.string.rooms_error_message),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.rooms_retry_message))
            }
        }
    }
}

private val GroupHostTab.pageIndex: Int
    get() = when (this) {
        GroupHostTab.USERS -> 0
        GroupHostTab.GROUP_CHAT -> 1
        GroupHostTab.PRIVATE_CHATS -> 2
    }

private val GroupHostTab.labelRes: Int
    get() = when (this) {
        GroupHostTab.USERS -> R.string.tab_group_users
        GroupHostTab.GROUP_CHAT -> R.string.tab_group_chat
        GroupHostTab.PRIVATE_CHATS -> R.string.tab_group_chats
    }

private fun groupHostTabFromPage(page: Int): GroupHostTab = when (page) {
    0 -> GroupHostTab.USERS
    2 -> GroupHostTab.PRIVATE_CHATS
    else -> GroupHostTab.GROUP_CHAT
}
