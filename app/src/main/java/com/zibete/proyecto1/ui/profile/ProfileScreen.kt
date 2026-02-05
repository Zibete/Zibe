package com.zibete.proyecto1.ui.profile

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.ui.SnackBarManagerEntryPoint
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.ui.chat.session.ChatSessionUiHandler
import com.zibete.proyecto1.ui.components.ChatPhotoItem
import com.zibete.proyecto1.ui.components.PhotoHeader
import com.zibete.proyecto1.ui.components.UserStatusRow
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.components.ZibeCircularProgress
import com.zibete.proyecto1.ui.components.ZibeCollapsingFabStack
import com.zibete.proyecto1.ui.components.ZibeMenuItem
import com.zibete.proyecto1.ui.components.ZibeSnackbar
import com.zibete.proyecto1.ui.components.ZibeToolbar
import com.zibete.proyecto1.ui.components.showZibeMessage
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    profileViewModel: ProfileViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenPhoto: (String) -> Unit
) {
    val state by profileViewModel.uiState.collectAsStateWithLifecycle()
    val userStatus by profileViewModel.userStatus.collectAsStateWithLifecycle()
    val photosFromChat by profileViewModel.photoList.collectAsStateWithLifecycle()
    val groupName by profileViewModel.groupName.collectAsStateWithLifecycle()

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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(profileViewModel.otherUid) {
        profileViewModel.loadProfile()
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            profileViewModel.snackEvents.collectLatest { snackEvent ->
                snackbarHostState.showZibeMessage(
                    message = snackEvent.uiText.asString(context),
                    snackType = snackEvent.type
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        profileViewModel.events.collect { event ->
            ChatSessionUiHandler.handle(
                context = context,
                event = event,
                scope = scope,
                snackBarManager = snackBarManager
            )
        }
    }

    ProfileScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        userStatus = userStatus,
        photoList = photosFromChat,
        groupName = groupName,
        distanceLabel = state.distanceLabel,
        onBack = onBack,
        onRefresh = { profileViewModel.loadProfile() },
        onOpenChat = onOpenChat,
        onOpenPhoto = onOpenPhoto,
        onToggleFavorite = profileViewModel::onToggleFavorite,
        onToggleNotifications = { profileViewModel.onToggleNotifications() },
        onConfirmBlockAction = { profileViewModel.onConfirmBlockAction() },
        onConfirmDeleteAction = { profileViewModel.onConfirmDeleteAction() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    userStatus: UserStatus,
    photoList: List<String>,
    groupName: String,
    distanceLabel: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenChat: (userId: String) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onToggleNotifications: () -> Unit,
    onConfirmBlockAction: () -> Unit,
    onConfirmDeleteAction: () -> Unit,
) {
    val zibeColors = LocalZibeExtendedColors.current
    val zibeTypography = LocalZibeTypography.current

    val scrollState = rememberScrollState()
    val spacingMd = dimensionResource(R.dimen.element_spacing_medium)
    val spacingSm = dimensionResource(R.dimen.element_spacing_small)

    var fabHeightPx by remember { mutableIntStateOf(0) }
    val fabHeightDp = with(LocalDensity.current) { fabHeightPx.toDp() }
    val bottomSpacerTarget = fabHeightDp + spacingMd
    val bottomSpacer by animateDpAsState(bottomSpacerTarget, label = "fabSpacer")

    val profile = state.profile

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
                    stringResource(R.string.menu_user_notifications_off)
                } else stringResource(R.string.menu_user_notifications_on),
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
        if (state.canDeleteChat) {
            add(
                ZibeMenuItem(
                    label = stringResource(R.string.menu_delete_chat),
                    onClick = onConfirmDeleteAction
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
            snackbarHost = {
                ZibeSnackbar(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ZibeToolbar(
                    title = "",
                    onBack = onBack,
                    menuItems = menuItems
                )
            },
            floatingActionButton = {
                val secondaryLabel = if (state.isGroupMatch) {
                    stringResource(R.string.chat_private_in_group, groupName)
                } else null

                val collapseThresholdPx = with(LocalDensity.current) {
                    dimensionResource(R.dimen.fab_collapse_scroll_threshold).toPx()
                }

                ZibeCollapsingFabStack(
                    collapsed = scrollState.value > collapseThresholdPx,
                    primaryText = {
                        Text(
                            text = stringResource(R.string.chat_zibe_app),
                            style = zibeTypography.label
                        )
                    },
                    primaryIcon = {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = stringResource(R.string.chat_zibe_app)
                        )
                    },
                    primaryEnabled = state.canOpenChat,
                    primaryLoading = state.isActionLoading,
                    onPrimaryClick = { state.profile?.id?.let(onOpenChat) },
                    secondaryText = secondaryLabel?.let { label ->
                        {
                            Text(
                                label,
                                style = zibeTypography.label
                            )
                        }
                    },
                    secondaryIcon = secondaryLabel?.let {
                        {
                            Icon(
                                imageVector = Icons.Filled.PersonAdd,
                                contentDescription = null
                            )
                        }
                    },
                    secondaryEnabled = !state.isActionLoading,
                    onSecondaryClick = secondaryLabel?.let {
                        { profile?.id?.let(onOpenChat) }
                    },
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
                        isRefreshing = false,
                        onRefresh = onRefresh,
                        state = rememberPullToRefreshState(),
                        modifier = contentModifier
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(
                                    horizontal = dimensionResource(R.dimen.screen_padding),
                                    vertical = dimensionResource(R.dimen.screen_padding)
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

                            ZibeCard(
                                contentPadding = PaddingValues(spacingSm)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacingSm)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = zibeColors.accent
                                    )
                                    Text(
                                        text = distanceLabel,
                                        style = zibeTypography.label,
                                        color = zibeColors.accent,
                                        modifier = Modifier.weight(1f)
                                    )

                                    IconButton(onClick = onToggleFavorite) {
                                        Icon(
                                            imageVector = if (state.isFavorite) {
                                                Icons.Filled.Star
                                            } else {
                                                Icons.Outlined.StarOutline
                                            },
                                            contentDescription = stringResource(R.string.content_description_toggle_favorite),
                                            tint = zibeColors.accent
                                        )
                                    }

                                    if (state.isBlockedByMe) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_baseline_block_24),
                                            contentDescription = stringResource(R.string.menu_user_block),
                                            tint = zibeColors.snackRed
                                        )
                                    }

                                    if (state.hasBlockedMe) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_baseline_cancel_schedule_send_24),
                                            contentDescription = stringResource(R.string.menu_user_unblock),
                                            tint = zibeColors.snackRed
                                        )
                                    }
                                }
                            }

                            ZibeCard(
                                contentPadding = PaddingValues(spacingSm)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val ageText = remember(profile.birthDate) {
                                        ageCalculator(profile.birthDate).toString()
                                    }
                                    Text(
                                        text = ageText,
                                        style = zibeTypography.h1,
                                        color = zibeColors.lightText
                                    )
                                    Text(
                                        text = profile.name,
                                        style = zibeTypography.h2,
                                        color = zibeColors.lightText
                                    )
                                }

                                Spacer(modifier = Modifier.height(spacingSm))

                                UserStatusRow(userStatus = userStatus)
                            }

                            if (photoList.isNotEmpty()) {
                                ZibeCard(contentPadding = PaddingValues(spacingSm)) {
                                    Text(
                                        text = stringResource(R.string.photos_received),
                                        style = zibeTypography.subtitle,
                                        color = zibeColors.lightText
                                    )

                                    Spacer(modifier = Modifier.height(spacingSm))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(spacingSm)
                                    ) {
                                        items(photoList) { url ->
                                            ChatPhotoItem(
                                                url = url,
                                                onClick = { onOpenPhoto(url) }
                                            )
                                        }
                                    }
                                }
                            }

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
        distanceLabel = "1.2 km away",
        isFavorite = true
    )

    ZibeTheme {
        ProfileScreen(
            state = sampleState,
            snackbarHostState = SnackbarHostState(),
            userStatus = UserStatus.Online,
            photoList = listOf("url1", "url2"),
            groupName = "Sample Group",
            distanceLabel = sampleState.distanceLabel,
            onBack = {},
            onRefresh = {},
            onToggleFavorite = {},
            onOpenChat = {},
            onOpenPhoto = {},
            onToggleNotifications = {},
            onConfirmBlockAction = {},
            onConfirmDeleteAction = {}
        )
    }
}
