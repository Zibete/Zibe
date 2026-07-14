package com.zibete.proyecto1.ui.users

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_EMPTY
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_FILTERS
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_LIST
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_ONLINE_FILTER
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_SCREEN
import com.zibete.proyecto1.core.constants.Constants.UiTags.DISCOVER_SEARCH
import com.zibete.proyecto1.core.constants.Constants.UiTags.FIRST_DM_SHEET
import com.zibete.proyecto1.ui.components.SheetActions
import com.zibete.proyecto1.ui.components.SheetHeader
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.components.ZibeButtonOutlined
import com.zibete.proyecto1.ui.components.ZibeButtonPrimary
import com.zibete.proyecto1.ui.motion.ZibeHapticFeedback
import com.zibete.proyecto1.ui.motion.performZibeFeedback
import com.zibete.proyecto1.ui.motion.zibePressFeedback
import com.zibete.proyecto1.ui.motion.zibePressable
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun DiscoverRoute(
    viewModel: UsersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiscoverScreen(
        state = state,
        errorMessage = state.error?.asString(),
        formatDistance = viewModel::formatDistance,
        onSearchChanged = viewModel::onSearchQueryChanged,
        onOnlineFilterChanged = viewModel::onOnlineFilterChanged,
        onApplyFilters = viewModel::applyFilters,
        onClearFilters = viewModel::clearFilters,
        onRefresh = viewModel::loadUsers,
        onRetry = viewModel::loadUsers,
        onProfileClick = viewModel::onUserProfileClick,
        onChatClick = viewModel::onUserChatClick,
        onFavoriteClick = viewModel::onFavoriteClick,
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
    onSearchChanged: (String) -> Unit,
    onOnlineFilterChanged: (Boolean) -> Unit,
    onApplyFilters: (Boolean, Boolean, Int, Int) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onProfileClick: (String) -> Unit,
    onChatClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onConfirmFirstContact: () -> Unit,
    onCancelFirstContact: () -> Unit,
    onVisibleUserIdsChanged: (List<String>) -> Unit = {}
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val colors = LocalZibeExtendedColors.current
    val haptics = LocalHapticFeedback.current
    val onlineFilterInteraction = remember { MutableInteractionSource() }
    val filtersInteraction = remember { MutableInteractionSource() }
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
        }.distinctUntilChanged().collect(onVisibleUserIdsChanged)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(DISCOVER_SCREEN)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DISCOVER_SEARCH),
            placeholder = { Text(stringResource(R.string.discover_search_hint)) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.discover_for_you),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.lightText
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = state.applyOnlineFilter,
                    onClick = {
                        haptics.performZibeFeedback(ZibeHapticFeedback.Selection)
                        onOnlineFilterChanged(!state.applyOnlineFilter)
                    },
                    label = { Text(stringResource(R.string.online)) },
                    modifier = Modifier
                        .testTag(DISCOVER_ONLINE_FILTER)
                        .zibePressFeedback(onlineFilterInteraction),
                    interactionSource = onlineFilterInteraction
                )
                FilterChip(
                    selected = state.hasActiveFilters,
                    onClick = {
                        haptics.performZibeFeedback(ZibeHapticFeedback.Selection)
                        showFilters = true
                    },
                    label = { Text(stringResource(R.string.discover_filters)) },
                    leadingIcon = {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                    },
                    modifier = Modifier
                        .testTag(DISCOVER_FILTERS)
                        .zibePressFeedback(filtersInteraction),
                    interactionSource = filtersInteraction
                )
            }
        }
        Spacer(Modifier.height(8.dp))

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
                    onClearFilters = {
                        onSearchChanged("")
                        onClearFilters()
                    }
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(DISCOVER_LIST),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            isFavoriteLoading = state.favoriteActionUserId == user.id,
                            onProfileClick = { onProfileClick(user.id) },
                            onChatClick = { onChatClick(user.id) },
                            onFavoriteClick = { onFavoriteClick(user.id) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    DiscoverFiltersSheet(
        isOpen = showFilters,
        state = state,
        onDismiss = { showFilters = false },
        onApply = { ageEnabled, onlineEnabled, minAge, maxAge ->
            onApplyFilters(ageEnabled, onlineEnabled, minAge, maxAge)
            showFilters = false
        },
        onClear = {
            onClearFilters()
            showFilters = false
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
    isFavoriteLoading: Boolean,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val colors = LocalZibeExtendedColors.current
    val chatDescription = stringResource(R.string.discover_start_chat, user.name)
    val favoriteDescription = stringResource(
        if (user.isFavorite) R.string.discover_remove_favorite else R.string.discover_add_favorite,
        user.name
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discover_person_${user.id}")
            .zibePressable(role = Role.Button, onClick = onProfileClick),
        shape = MaterialTheme.shapes.large,
        color = colors.cardBackground,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                placeholder = painterResource(R.mipmap.logo_zibe_icon),
                error = painterResource(R.mipmap.logo_zibe_icon),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.lightText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (user.age > 0) {
                        Text(
                            text = stringResource(R.string.label_age, user.age),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.hintText
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusText = if (user.isOnline) {
                        stringResource(R.string.online)
                    } else {
                        stringResource(R.string.offline)
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (user.isOnline) colors.statusOnline else colors.statusOffline,
                        modifier = Modifier.semantics {
                            stateDescription = statusText
                        }
                    )
                    if (distance.isNotBlank()) {
                        Text(
                            text = distance,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.hintText
                        )
                    }
                }
                if (user.description.isNotBlank()) {
                    Text(
                        text = user.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.hintText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (user.isFavorite) {
                        DiscoverStatusBadge(stringResource(R.string.tag_favorite))
                    }
                    if (user.isBlockedByMe) {
                        DiscoverStatusBadge(stringResource(R.string.tag_blocked_by_me))
                    }
                    if (user.hasBlockedMe) {
                        DiscoverStatusBadge(stringResource(R.string.tag_has_blocked_me))
                    }
                    if (user.isNotificationsSilenced) {
                        DiscoverStatusBadge(stringResource(R.string.tag_silent))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onFavoriteClick,
                        enabled = !isFavoriteLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = favoriteDescription }
                    ) {
                        Icon(
                            imageVector = if (user.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (user.isFavorite) colors.accent else colors.hintText
                        )
                    }
                    IconButton(
                        onClick = onChatClick,
                        enabled = !isChatLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = chatDescription }
                    ) {
                        if (isChatLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
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
    }
}

@Composable
private fun DiscoverStatusBadge(text: String) {
    val colors = LocalZibeExtendedColors.current
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.accent.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.lightText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

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
    val haptics = LocalHapticFeedback.current
    val onlineInteraction = remember { MutableInteractionSource() }
    val ageInteraction = remember { MutableInteractionSource() }

    ZibeBottomSheet(isOpen = isOpen, onCancel = onDismiss) {
        SheetHeader(
            title = stringResource(R.string.discover_filters),
            subtitle = stringResource(R.string.discover_filters_subtitle)
        )
        FilterChip(
            selected = onlineEnabled,
            onClick = {
                haptics.performZibeFeedback(ZibeHapticFeedback.Selection)
                onlineEnabled = !onlineEnabled
            },
            label = { Text(stringResource(R.string.discover_only_online)) },
            modifier = Modifier.zibePressFeedback(onlineInteraction),
            interactionSource = onlineInteraction
        )
        FilterChip(
            selected = ageEnabled,
            onClick = {
                haptics.performZibeFeedback(ZibeHapticFeedback.Selection)
                ageEnabled = !ageEnabled
            },
            label = { Text(stringResource(R.string.discover_filter_age)) },
            modifier = Modifier.zibePressFeedback(ageInteraction),
            interactionSource = ageInteraction
        )
        Text(stringResource(R.string.discover_age_range, minAge.toInt(), maxAge.toInt()))
        Slider(
            value = minAge,
            onValueChange = { minAge = it.coerceAtMost(maxAge) },
            valueRange = 18f..99f,
            steps = 80,
            enabled = ageEnabled
        )
        Slider(
            value = maxAge,
            onValueChange = { maxAge = it.coerceAtLeast(minAge) },
            valueRange = 18f..99f,
            steps = 80,
            enabled = ageEnabled
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
