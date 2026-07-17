package com.zibete.proyecto1.ui.users

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_EMPTY
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_FILTER_SHEET
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_LIST
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_SCROLL_TOP
import com.zibete.proyecto1.core.constants.Constants.UiTags.FIRST_DM_SHEET
import com.zibete.proyecto1.ui.components.SheetActions
import com.zibete.proyecto1.ui.components.SheetHeader
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.components.ZibeButtonOutlined
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.components.UserStatusTag
import com.zibete.proyecto1.ui.components.UserStatusTagType
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun DiscoverRoute(
    viewModel: UsersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiscoverScreen(
        state = state,
        errorMessage = state.error?.asString(),
        formatDistance = viewModel::formatDistance,
        onApplyFilters = viewModel::applyFilters,
        onClearFilters = viewModel::clearFilters,
        onClearAllCriteria = viewModel::clearAllCriteria,
        onDismissFilters = viewModel::onFilterDismissed,
        onRefresh = viewModel::loadUsers,
        onRetry = viewModel::loadUsers,
        onProfileClick = viewModel::onUserProfileClick,
        onChatClick = viewModel::onUserChatClick,
        onVisibleUserIdsChanged = viewModel::onVisibleUsersChanged,
        onConfirmFirstContact = viewModel::confirmFirstContact,
        onCancelFirstContact = viewModel::cancelFirstContact
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
    state: UsersUiState,
    errorMessage: String?,
    formatDistance: (Double) -> String,
    onApplyFilters: (Boolean, Boolean, Int, Int) -> Unit,
    onClearFilters: () -> Unit,
    onClearAllCriteria: () -> Unit,
    onDismissFilters: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onProfileClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onConfirmFirstContact: () -> Unit,
    onCancelFirstContact: () -> Unit,
    onVisibleUserIdsChanged: (List<String>) -> Unit = {}
) {
    val colors = LocalZibeExtendedColors.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var scrollToTopJob by remember { mutableStateOf<Job?>(null) }
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 ||
                (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 100)
        }
    }
    val currentVisibleUsersCallback by rememberUpdatedState(onVisibleUserIdsChanged)

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
        }.distinctUntilChanged().collect(currentVisibleUsersCallback)
    }
    DisposableEffect(Unit) {
        onDispose { currentVisibleUsersCallback(emptyList()) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(DISCOVER_SCREEN)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.discover_for_you),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.lightText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading -> DiscoverLoading()
                    errorMessage != null && state.users.isEmpty() -> DiscoverError(
                        message = errorMessage,
                        onRetry = onRetry
                    )
                    state.users.isEmpty() -> DiscoverEmpty(
                        hasActiveFilters = state.hasActiveFilters || state.searchQuery.isNotBlank(),
                        onClearFilters = onClearAllCriteria
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(DISCOVER_LIST),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = state.users,
                            key = { it.id },
                            contentType = { "person" }
                        ) { user ->
                            DiscoverPersonCard(
                                user = user,
                                distance = formatDistance(user.distanceMeters),
                                isChatLoading = state.chatCheckUserId == user.id,
                                onProfileClick = { onProfileClick(user.id) },
                                onChatClick = { onChatClick(user.id) }
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showScrollToTop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    if (scrollToTopJob?.isActive == true) return@FloatingActionButton
                    scrollToTopJob = coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                modifier = Modifier.testTag(DISCOVER_SCROLL_TOP),
                shape = RoundedCornerShape(20.dp),
                containerColor = colors.accent,
                contentColor = colors.lightText
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.action_scroll_to_top)
                )
            }
        }
    }

    DiscoverFiltersSheet(
        isOpen = state.isFilterSheetOpen,
        state = state,
        onDismiss = onDismissFilters,
        onApply = { ageEnabled, onlineEnabled, minAge, maxAge ->
            onApplyFilters(ageEnabled, onlineEnabled, minAge, maxAge)
            onDismissFilters()
        },
        onClear = {
            onClearFilters()
            onDismissFilters()
        }
    )

    FirstContactSheet(
        user = state.pendingFirstContact,
        onConfirm = onConfirmFirstContact,
        onCancel = onCancelFirstContact
    )
}

@Composable
private fun DiscoverLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.discover_loading))
        }
    }
}

@Composable
private fun DiscoverError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(message, color = MaterialTheme.colorScheme.error)
            ZibeButtonPrimary(
                text = stringResource(R.string.action_retry),
                onClick = onRetry
            )
        }
    }
}

@Composable
private fun DiscoverEmpty(hasActiveFilters: Boolean, onClearFilters: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(DISCOVER_EMPTY),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.discover_empty))
            if (hasActiveFilters) {
                ZibeButtonOutlined(
                    text = stringResource(R.string.discover_clear_filters),
                    onClick = onClearFilters
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverPersonCard(
    user: UsersRowUiModel,
    distance: String,
    isChatLoading: Boolean,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val colors = LocalZibeExtendedColors.current
    val typography = LocalZibeTypography.current
    val titleTextSize = with(LocalDensity.current) {
        dimensionResource(discoverPersonTitleTextSizeRes).toSp()
    }
    val descriptionTextSize = with(LocalDensity.current) {
        dimensionResource(discoverPersonDescriptionTextSizeRes).toSp()
    }
    val displayName = user.name.ifBlank { stringResource(R.string.deleted_profile_fallback) }
    val chatDescription = stringResource(R.string.discover_start_chat, displayName)
    val presenceDescription = stringResource(
        if (user.isOnline) R.string.discover_presence_online
        else R.string.discover_presence_offline
    )
    Surface(
        onClick = onProfileClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("discover_person_${user.id}"),
        shape = RoundedCornerShape(20.dp),
        color = colorResource(discoverPersonCardColorRes),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .testTag("discover_avatar_${user.id}")
                ) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = stringResource(
                            R.string.discover_avatar_description,
                            displayName
                        ),
                        placeholder = painterResource(R.mipmap.logo_zibe_icon),
                        error = painterResource(R.mipmap.logo_zibe_icon),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 2.dp,
                                color = colorResource(DsR.color.white),
                                shape = CircleShape
                            )
                            .padding(2.dp)
                            .clip(CircleShape)
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .shadow(4.dp, CircleShape, clip = false)
                            .testTag(
                                if (user.isOnline) "discover_presence_online_${user.id}"
                                else "discover_presence_offline_${user.id}"
                            )
                            .semantics { stateDescription = presenceDescription },
                        shape = CircleShape,
                        color = colorResource(
                            if (user.isOnline) DsR.color.status_online
                            else DsR.color.status_offline
                        ),
                        border = BorderStroke(1.dp, colorResource(DsR.color.status_stroke))
                    ) {}
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName,
                            style = typography.h2.copy(fontSize = titleTextSize),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.lightText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.label_age, user.age),
                            style = typography.h2.copy(fontSize = titleTextSize),
                            color = colors.lightText,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.padding(end = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (distance.isNotBlank()) {
                            UserStatusTag(
                                type = UserStatusTagType.DISTANCE,
                                text = distance
                            )
                        }
                        if (user.isFavorite) {
                            UserStatusTag(
                                type = UserStatusTagType.FAVORITE,
                                text = stringResource(R.string.tag_favorite)
                            )
                        }
                        if (user.isBlockedByMe) {
                            UserStatusTag(
                                type = UserStatusTagType.BLOCKED_BY_ME,
                                text = stringResource(R.string.tag_blocked_by_me)
                            )
                        }
                        if (user.hasBlockedMe) {
                            UserStatusTag(
                                type = UserStatusTagType.HAS_BLOCKED_ME,
                                text = stringResource(R.string.tag_has_blocked_me)
                            )
                        }
                        if (user.isNotificationsSilenced) {
                            UserStatusTag(
                                type = UserStatusTagType.SILENCED,
                                text = stringResource(R.string.tag_silent)
                            )
                        }
                    }
                    if (user.description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = user.description,
                            style = typography.body.copy(fontSize = descriptionTextSize),
                            color = colors.lightText.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(end = 48.dp)
                                .testTag("discover_description_${user.id}")
                        )
                    }
                }
            }
            IconButton(
                onClick = onChatClick,
                enabled = !isChatLoading,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(48.dp)
                    .testTag("discover_chat_${user.id}")
                    .semantics { contentDescription = chatDescription }
            ) {
                if (isChatLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(22.dp)
                            .testTag("discover_chat_loading_${user.id}")
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = colors.accent
                    )
                }
            }
        }
    }
}

internal val discoverPersonCardColorRes: Int = DsR.color.glass_bg_light
internal val discoverPersonTitleTextSizeRes: Int = DsR.dimen.text_size_row_title
internal val discoverPersonDescriptionTextSizeRes: Int = DsR.dimen.text_size_row_description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverFiltersSheet(
    isOpen: Boolean,
    state: UsersUiState,
    onDismiss: () -> Unit,
    onApply: (Boolean, Boolean, Int, Int) -> Unit,
    onClear: () -> Unit
) {
    var ageEnabled by remember(isOpen, state.applyAgeFilter) {
        mutableStateOf(state.applyAgeFilter)
    }
    var onlineEnabled by remember(isOpen, state.applyOnlineFilter) {
        mutableStateOf(state.applyOnlineFilter)
    }
    var minAge by remember(isOpen, state.minAge) { mutableFloatStateOf(state.minAge.toFloat()) }
    var maxAge by remember(isOpen, state.maxAge) { mutableFloatStateOf(state.maxAge.toFloat()) }
    val minAgeDescription = stringResource(R.string.discover_min_age)
    val maxAgeDescription = stringResource(R.string.discover_max_age)

    ZibeBottomSheet(
        isOpen = isOpen,
        onCancel = onDismiss,
        contentModifier = Modifier.testTag(DISCOVER_FILTER_SHEET)
    ) {
        SheetHeader(
            title = stringResource(R.string.discover_filters),
            subtitle = stringResource(R.string.discover_filters_subtitle)
        )
        FilterChip(
            selected = onlineEnabled,
            onClick = { onlineEnabled = !onlineEnabled },
            label = { Text(stringResource(R.string.discover_only_online)) },
            modifier = Modifier
        )
        FilterChip(
            selected = ageEnabled,
            onClick = { ageEnabled = !ageEnabled },
            label = { Text(stringResource(R.string.discover_filter_age)) },
            modifier = Modifier
        )
        Text(stringResource(R.string.discover_age_range, minAge.toInt(), maxAge.toInt()))
        Slider(
            value = minAge,
            onValueChange = { minAge = it.coerceAtMost(maxAge) },
            valueRange = 18f..99f,
            steps = 80,
            enabled = ageEnabled,
            modifier = Modifier.semantics {
                contentDescription = minAgeDescription
            }
        )
        Slider(
            value = maxAge,
            onValueChange = { maxAge = it.coerceAtLeast(minAge) },
            valueRange = 18f..99f,
            steps = 80,
            enabled = ageEnabled,
            modifier = Modifier.semantics {
                contentDescription = maxAgeDescription
            }
        )
        if (state.hasActiveFilters) {
            ZibeButtonOutlined(
                text = stringResource(R.string.discover_clear_filters),
                onClick = onClear,
                modifier = Modifier.fillMaxWidth()
            )
        }
        SheetActions(
            confirmText = stringResource(R.string.discover_apply_filters),
            onConfirm = {
                onApply(ageEnabled, onlineEnabled, minAge.toInt(), maxAge.toInt())
            },
            onCancel = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstContactSheet(
    user: UsersRowUiModel?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    ZibeBottomSheet(
        isOpen = user != null,
        onCancel = onCancel
    ) {
        Column(
            modifier = Modifier.testTag(FIRST_DM_SHEET),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SheetHeader(
                title = stringResource(R.string.discover_first_dm_title),
                subtitle = stringResource(R.string.discover_first_dm_message)
            )
            SheetActions(
                confirmText = stringResource(R.string.discover_first_dm_confirm),
                cancelText = stringResource(R.string.action_cancel),
                onConfirm = onConfirm,
                onCancel = onCancel
            )
        }
    }
}
