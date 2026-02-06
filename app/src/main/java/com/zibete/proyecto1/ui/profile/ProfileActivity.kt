package com.zibete.proyecto1.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_CHAT_NODE
import com.zibete.proyecto1.core.constants.Constants.EXTRA_START_INDEX
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_ID
import com.zibete.proyecto1.core.constants.Constants.EXTRA_USER_IDS
import com.zibete.proyecto1.core.constants.Constants.NODE_DM
import com.zibete.proyecto1.ui.base.BaseChatSessionActivity
import com.zibete.proyecto1.ui.chat.ChatActivity
import com.zibete.proyecto1.ui.media.PhotoViewerActivity
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileActivity : BaseChatSessionActivity() {

    override val enableComposeSnackHost: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userIds = intent.getStringArrayListExtra(EXTRA_USER_IDS)
        val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
        val singleUserId = intent.getStringExtra(EXTRA_USER_ID)
        val resolvedUserIds = when {
            !userIds.isNullOrEmpty() -> userIds
            !singleUserId.isNullOrBlank() -> arrayListOf(singleUserId)
            else -> arrayListOf("")
        }

        setContent {
            ZibeTheme {
                ProfileActivityContent(
                    userIds = resolvedUserIds,
                    startIndex = startIndex,
                    onBack = { onBackPressedDispatcher.onBackPressed() },
                    onOpenChat = { userId ->
                        startActivity(
                            Intent(this, ChatActivity::class.java).apply {
                                putExtra(EXTRA_CHAT_ID, userId)
                                putExtra(EXTRA_CHAT_NODE, NODE_DM)
                            }
                        )
                    },
                    onOpenPhoto = { url ->
                        PhotoViewerActivity.startSingle(this, url)
                    },
                    viewModelProvider = { userId -> provideProfileViewModel(userId) }
                )
            }
        }
    }

    private fun provideProfileViewModel(userId: String): ProfileViewModel {
        val extras = MutableCreationExtras(defaultViewModelCreationExtras).apply {
            set(DEFAULT_ARGS_KEY, bundleOf(EXTRA_USER_ID to userId))
        }
        return ViewModelProvider
            .create(this, defaultViewModelProviderFactory, extras)
            .get("profile_$userId", ProfileViewModel::class.java)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileActivityContent(
    userIds: List<String>,
    startIndex: Int,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenPhoto: (String) -> Unit,
    viewModelProvider: (String) -> ProfileViewModel
) {
    if (userIds.size <= 1) {
        val userId = userIds.firstOrNull().orEmpty()
        val profileViewModel = remember(userId) { viewModelProvider(userId) }
        ProfileRoute(
            profileViewModel = profileViewModel,
            isActive = true,
            onBack = onBack,
            onOpenChat = onOpenChat,
            onOpenPhoto = onOpenPhoto
        )
        return
    }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, userIds.lastIndex),
        pageCount = { userIds.size }
    )

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val userId = userIds[page]
        val profileViewModel = remember(userId) { viewModelProvider(userId) }
        ProfileRoute(
            profileViewModel = profileViewModel,
            isActive = pagerState.currentPage == page,
            onBack = onBack,
            onOpenChat = onOpenChat,
            onOpenPhoto = onOpenPhoto
        )
    }
}

