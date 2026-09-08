package com.zibete.proyecto1.ui.profile

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.core.ui.SnackBarManagerEntryPoint
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiHandler
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiEvent
import com.zibete.proyecto1.ui.components.FirstContactSheet
import com.zibete.proyecto1.ui.components.PhotoHeader
import com.zibete.proyecto1.ui.components.ProfileCard
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.components.ZibeCircularProgress
import com.zibete.proyecto1.ui.components.ZibeCollapsingFabStack
import com.zibete.proyecto1.ui.components.ZibeMenuItem
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    profileViewModel: ProfileViewModel = hiltViewModel(),
    isActive: Boolean = true,
    onBack: () -> Unit,
    onOpenDmChat: (String) -> Unit,
    onOpenPhoto: (String) -> Unit
) {
    val state by profileViewModel.uiState.collectAsStateWithLifecycle()
    val userStatus by profileViewModel.userStatus.collectAsStateWithLifecycle()
    val photosFromChat by profileViewModel.photoList.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContext = context.applicationContext
    val snackBarManager = remember(appContext) {
        EntryPointAccessors.fromApplication(
            appContext,
            SnackBarManagerEntryPoint::class.java
        ).snackBarManager()
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(profileViewModel.otherUid) {
        profileViewModel.loadProfile()
    }

    LaunchedEffect(lifecycleOwner, isActive) {
        if (!isActive) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            profileViewModel.refreshProfileMeta()
        }
    }

    DisposableEffect(profileViewModel, isActive) {
        profileViewModel.onPageActiveChanged(isActive)
        onDispose {
            if (isActive) profileViewModel.onPageActiveChanged(false)
        }
    }

    LaunchedEffect(profileViewModel, isActive) {
        if (!isActive) return@LaunchedEffect
        profileViewModel.events.collect { event ->
            if (event is ChatSessionUiEvent.OpenDirectMessage) {
                if (event.userId == profileViewModel.otherUid) onOpenDmChat(event.userId)
            } else {
                ChatSessionUiHandler.handle(
                    context = context,
                    event = event,
                    scope = scope,
                    snackBarManager = snackBarManager
                )
            }
        }
    }

    ProfileScreen(
        state = state,
        userStatus = userStatus,
        photoList = photosFromChat,
        distanceLabel = state.distanceLabel,
        isActive = isActive,
        onBack = onBack,
        onRefresh = { profileViewModel.refreshProfile() },
        onDmChatClick = profileViewModel::onDmChatRequested,
        onConfirmFirstContact = profileViewModel::confirmFirstContact,
        onCancelFirstContact = profileViewModel::cancelFirstContact,
        onOpenPhoto = onOpenPhoto,
        onToggleFavorite = profileViewModel::onToggleFavorite,
        onToggleNotifications = { profileViewModel.onToggleNotifications() },
        onConfirmBlockAction = { profileViewModel.onConfirmBlockAction() },
        onDeleteChoiceMode = { profileViewModel.onDeleteChoiceMode() },
        onConfirmHide = { profileViewModel.onConfirmHide() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    userStatus: UserStatus,
    photoList: List<String>,
    distanceLabel: String,
    isActive: Boolean = true,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDmChatClick: () -> Unit,
    onConfirmFirstContact: () -> Unit,
    onCancelFirstContact: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onToggleNotifications: () -> Unit,
    onConfirmBlockAction: () -> Unit,
    onDeleteChoiceMode: () -> Unit,
    onConfirmHide: () -> Unit
) {
    val zibeColors = LocalZibeExtendedColors.current
    val zibeTypography = LocalZibeTypography.current

    val scrollState = rememberScrollState()
    val spacingMd = dimensionResource(DsR.dimen.element_spacing_medium)
    val spacingSm = dimensionResource(DsR.dimen.element_spacing_small)

    var fabHeightPx by remember { mutableIntStateOf(0) }
    val fabHeightDp = with(LocalDensity.current) { fabHeightPx.toDp() }
    val bottomSpacerTarget = fabHeightDp + spacingMd
    val bottomSpacer by animateDpAsState(bottomSpacerTarget, label = "fabSpacer")

    val menuItems = buildList {
        add(
            ZibeMenuItem(
                label = if (state.isFavorite) {
                    stringResource(R.string.menu_remove_favorite)
                } else stringResource(R.string.menu_add_favorite),
                onClick = onToggleFavorite
            )
        )
        add(
            ZibeMenuItem(
                label = if (state.isNotificationsSilenced) {
                    stringResource(R.string.menu_user_notifications_on)
                } else stringResource(R.string.menu_user_notifications_off),
                onClick = onToggleNotifications
            )
        )
        add(
            ZibeMenuItem(
                label = if (state.isBlockedByMe) {
                    stringResource(R.string.menu_user_unblock)
                } else stringResource(R.string.menu_user_block),
                onClick = onConfirmBlockAction
            )
        )
        if (state.hasConversation && !state.isHide) {
            add(
                ZibeMenuItem(
                    label = stringResource(R.string.menu_hide_chat),
                    onClick = onConfirmHide
                )
            )
        }
        if (state.hasConversation) {
            add(
                ZibeMenuItem(
                    label = stringResource(R.string.menu_delete_chat),
                    onClick = onDeleteChoiceMode
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(zibeColors.gradientZibe)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ZibeToolbar(
                    title = "",
                    onBack = onBack,
                    menuItems = menuItems
                )
            },
            floatingActionButton = {
                val collapseThresholdPx = with(LocalDensity.current) {
                    dimensionResource(DsR.dimen.fab_collapse_scroll_threshold).toPx()
                }
                val isFabCollapsed by remember(scrollState, collapseThresholdPx) {
                    derivedStateOf { scrollState.value > collapseThresholdPx }
                }

                ZibeCollapsingFabStack(
                    collapsed = isFabCollapsed,
                    primaryText = {
                        Text(
                            text = stringResource(R.string.chat_zibe_app),
                            style = zibeTypography.label
                        )
                    },
                    primaryIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_zibe_app)
                        )
                    },
                    primaryEnabled = state.canOpenChat,
                    primaryLoading = state.isDmEntryLoading,
                    onPrimaryClick = onDmChatClick,
                    secondaryText = null,
                    secondaryIcon = null,
                    secondaryEnabled = false,
                    onSecondaryClick = null,
                    onHeightPxChanged = { fabHeightPx = it }
                )
            }
        ) { innerPadding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

            when (val content = state.content) {
                is ProfileContent.Loading -> {
                    Box(
                        modifier = contentModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        ZibeCircularProgress()
                    }
                }

                is ProfileContent.NotFound -> {
                    Box(
                        modifier = contentModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.deleted_profile_fallback),
                            style = zibeTypography.label,
                            color = zibeColors.lightText
                        )
                    }
                }

                is ProfileContent.Error -> {
                    Box(
                        modifier = contentModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = content.uiText.asString(),
                            style = zibeTypography.label,
                            color = zibeColors.lightText
                        )
                    }
                }

                is ProfileContent.Ready -> {
                    val profile = content.profile

                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        state = rememberPullToRefreshState(),
                        modifier = contentModifier
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(
                                    horizontal = dimensionResource(DsR.dimen.screen_padding),
                                    vertical = dimensionResource(DsR.dimen.screen_padding)
                                )
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
                                ),
                            verticalArrangement = Arrangement.spacedBy(spacingMd)
                        ) {
                            PhotoHeader(
                                photoUrl = profile.photoUrl,
                                isLoading = false,
                                onClick = {
                                    if (profile.photoUrl.isNotBlank()) {
                                        onOpenPhoto(profile.photoUrl)
                                    }
                                }
                            )

                            ProfileCard(
                                profile = profile,
                                state = state,
                                userStatus = userStatus,
                                distanceLabel = distanceLabel,
                                photoList = photoList,
                                onToggleFavorite = onToggleFavorite,
                                onOpenPhoto = onOpenPhoto
                            )

                            if (profile.description.isNotBlank()) {
                                ZibeCard(contentPadding = PaddingValues(spacingSm)) {
                                    Text(
                                        text = profile.description,
                                        style = zibeTypography.body,
                                        color = zibeColors.lightText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(bottomSpacer))
                        }
                    }
                }
            }
        }
    }

    FirstContactSheet(
        isOpen = isActive && state.pendingFirstContactUserId != null,
        onConfirm = onConfirmFirstContact,
        onCancel = onCancelFirstContact
    )
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    val sampleUser = Users(
        id = "1",
        name = "Jane Doe",
        birthDate = "1995-05-15",
        photoUrl = "https://example.com/photo.jpg",
        description = "Hello, I am using Zibe!"
    )
    val sampleState = ProfileUiState(
        content = ProfileContent.Ready(sampleUser),
        isActionLoading = false,
        profile = sampleUser,
        distanceLabel = "1.2 km",
        isFavorite = true
    )

    ZibeTheme {
        ProfileScreen(
            state = sampleState,
            userStatus = UserStatus.Online,
            photoList = listOf("url1", "url2"),
            distanceLabel = sampleState.distanceLabel,
            isActive = true,
            onBack = {},
            onRefresh = {},
            onToggleFavorite = {},
            onDmChatClick = {},
            onConfirmFirstContact = {},
            onCancelFirstContact = {},
            onOpenPhoto = {},
            onToggleNotifications = {},
            onConfirmBlockAction = {},
            onDeleteChoiceMode = {},
            onConfirmHide = {}
        )
    }
}
