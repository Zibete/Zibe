package com.zibete.proyecto1.ui.groups.host

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.SnackBarManagerEntryPoint
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupHostRoute(groupHostViewModel: GroupHostViewModel) {
    val state by groupHostViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    val snackBarManager = remember(appContext) {
        EntryPointAccessors.fromApplication(
            appContext,
            SnackBarManagerEntryPoint::class.java
        ).snackBarManager()
    }

    val pagerState = rememberPagerState(
        initialPage = 1, // GROUP_CHAT
        pageCount = { 2 }
    )

    // Sync tab selection
    LaunchedEffect(pagerState.currentPage) {
        val tab = when (pagerState.currentPage) {
            0 -> GroupHostTab.USERS
            1 -> GroupHostTab.GROUP_CHAT
            else -> GroupHostTab.PRIVATE_CHATS
        }
        groupHostViewModel.onTabSelected(tab)
    }

    // Events
    LaunchedEffect(Unit) {
        groupHostViewModel.events.collect { event ->
            when (event) {
                is GroupHostEvent.OpenPrivateChat -> {
                    val i = Intent(context, ChatActivity::class.java).apply {
                        putExtra(EXTRA_CHAT_ID, event.otherUid)
                        putExtra(EXTRA_CHAT_NODE, event.nodeType)
                    }
                    context.startActivity(i)
                }
                is GroupHostEvent.ShowSnack -> {
                    snackBarManager.show(event.message, event.type)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(stringResource(R.string.tab_group_users)) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(state.groupContext?.groupName ?: stringResource(R.string.tab_group_chat)) }
            )
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                text = { Text(stringResource(R.string.tab_group_chats)) }
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> GroupUsersTab(state = state, onUserClick = groupHostViewModel::onUserClicked)
                1 -> GroupChatTab(state = state, onSendText = groupHostViewModel::sendTextMessage, onSendPhoto = groupHostViewModel::sendPhotoMessage)
                2 -> PrivateChatsPlaceholder(unread = state.unreadMessages)
            }
        }
    }
}

@Composable
private fun PrivateChatsPlaceholder(unread: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Tab privados: lo armamos después.")
    }
}
