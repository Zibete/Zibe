package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.model.Users
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.profile.ProfileUiState
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ProfileCard(
    profile: Users,
    state: ProfileUiState,
    userStatus: UserStatus,
    distanceLabel: String,
    photoList: List<String>,
    onToggleFavorite: () -> Unit,
    onOpenPhoto: (String) -> Unit,
) {
    val zibeColors = LocalZibeExtendedColors.current
    val zibeTypography = LocalZibeTypography.current
    val spacing6 = dimensionResource(DsR.dimen.element_spacing_xxs)
    val spacing12 = dimensionResource(DsR.dimen.element_spacing_small)
    val spacing16 = dimensionResource(DsR.dimen.element_spacing_medium)
    val ageText = remember(profile.birthDate) {
        ageCalculator(profile.birthDate).toString()
    }

    ZibeCard(contentPadding = PaddingValues(spacing16)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing12)
            ) {
                Text(
                    text = ageText,
                    style = zibeTypography.h1,
                    color = zibeColors.lightText
                )
                Text(
                    text = profile.name,
                    style = zibeTypography.h2,
                    color = zibeColors.lightText,
                    modifier = Modifier.weight(1f)
                )
                FavoriteLottieStar(
                    isFavorite = state.isFavorite,
                    onClick = onToggleFavorite,
                    contentDescription = stringResource(R.string.content_description_toggle_favorite)
                )
            }

            HorizontalDivider(color = zibeColors.accent.copy(alpha = 0.5f))

            UserStatusRow(
                userStatus = userStatus,
                modifier = Modifier.fillMaxWidth()
            )

            val showTags = distanceLabel.isNotBlank() ||
                state.isFavorite ||
                state.isBlockedByMe ||
                state.hasBlockedMe ||
                state.isNotificationsSilenced ||
                state.isGroupMatch

            if (showTags) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing6),
                    verticalArrangement = Arrangement.spacedBy(spacing6)
                ) {
                    if (distanceLabel.isNotBlank()) {
                        UserStatusTag(
                            type = UserStatusTagType.DISTANCE,
                            text = distanceLabel,
                        )
                    }

                    if (state.isFavorite) {
                        UserStatusTag(
                            type = UserStatusTagType.FAVORITE,
                            text = stringResource(R.string.tag_favorite),
                        )
                    }

                    if (state.isBlockedByMe) {
                        UserStatusTag(
                            type = UserStatusTagType.BLOCKED_BY_ME,
                            text = stringResource(R.string.tag_blocked_by_me),
                        )
                    }

                    if (state.hasBlockedMe) {
                        UserStatusTag(
                            type = UserStatusTagType.HAS_BLOCKED_ME,
                            text = stringResource(R.string.tag_has_blocked_me),
                        )
                    }

                    if (state.isNotificationsSilenced) {
                        UserStatusTag(
                            type = UserStatusTagType.SILENCED,
                            text = stringResource(R.string.tag_silent),
                        )
                    }

                    if (state.isGroupMatch) {
                        UserStatusTag(
                            type = UserStatusTagType.GROUP_MATCH,
                            text = stringResource(R.string.tag_group_match),
                        )
                    }
                }
            }

            if (photoList.isNotEmpty()) {

                HorizontalDivider(color = zibeColors.accent.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.photos_received),
                        style = zibeTypography.subtitle,
                        color = zibeColors.lightText,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = photoList.size.toString(),
                        style = zibeTypography.label,
                        color = zibeColors.hintText
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing12)) {
                    items(photoList) { url ->
                        ChatPhotoItem(
                            url = url,
                            onClick = { onOpenPhoto(url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteLottieStar(
    isFavorite: Boolean,
    onClick: () -> Unit,
    contentDescription: String
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_favorite))
    val anim = rememberLottieAnimatable()
    var prev by remember { mutableStateOf(isFavorite) }
    val size = 28.dp

    LaunchedEffect(composition, isFavorite) {
        val c = composition ?: return@LaunchedEffect

        if (!prev && isFavorite) {
            anim.snapTo(c, 0f)
            anim.animate(c, iterations = 1)
        } else {
            anim.snapTo(c, if (isFavorite) 1f else 0f)
        }
        prev = isFavorite
    }

    IconButton(onClick = onClick) {
        if (composition == null) {
            Box(
                Modifier
                    .size(size)
                    .semantics { this.contentDescription = contentDescription }
            )
        } else {
            LottieAnimation(
                composition = composition,
                progress = { anim.progress },
                modifier = Modifier
                    .size(size)
                    .semantics { this.contentDescription = contentDescription }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileCardPreview() {
    ZibeTheme {
        ProfileCard(
            profile = Users(
                id = "1",
                name = "Nombre Visible",
                birthDate = "1990-01-01"
            ),
            state = ProfileUiState(
                isFavorite = false,
                isBlockedByMe = false,
                hasBlockedMe = false
            ),
            userStatus = UserStatus.LastSeen("últ. vez 12/01/2026 a las 16:55"),
            distanceLabel = "A 1600.5 km",
            photoList = listOf("url1", "url2", "url3"),
            onToggleFavorite = {},
            onOpenPhoto = {}
        )
    }
}


