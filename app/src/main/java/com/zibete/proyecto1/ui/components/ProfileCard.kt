package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.zibete.proyecto1.R
import com.zibete.proyecto1.core.utils.TimeUtils.ageCalculator
import com.zibete.proyecto1.model.UserStatus
import com.zibete.proyecto1.model.Users
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
    val spacingSm = dimensionResource(R.dimen.element_spacing_small)
    val spacingMd = dimensionResource(R.dimen.element_spacing_medium)

    val ageText = remember(profile.birthDate) {
        ageCalculator(profile.birthDate).toString()
    }

    ZibeCard(contentPadding = PaddingValues(spacingMd)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacingSm)) {

            // 1) Header (edad + nombre + favorito)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacingSm)
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

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(R.string.content_description_toggle_favorite),
                        tint = zibeColors.snackYellow
                    )
                }
            }

            HorizontalDivider(color = zibeColors.accent.copy(alpha = 0.5f))

            // 2) Meta row (status + distancia + badges)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacingSm)
            ) {
                UserStatusRow(
                    userStatus = userStatus,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = zibeColors.accent
                )

                Text(
                    text = distanceLabel,
                    style = zibeTypography.label,
                    color = zibeColors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }


            // Badges (en una fila aparte para que no se apelmace)
            if (state.isBlockedByMe || state.hasBlockedMe) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacingSm)) {

                    if (state.isBlockedByMe) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.chip_user_blocked)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.PersonOff,
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    if (state.hasBlockedMe) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.menu_user_unblock)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LockPerson,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }

            // Divider suave antes de fotos
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

                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacingSm)) {
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

@Preview(showBackground = true)
@Composable
fun ProfileCardPreview() {
    ZibeTheme {
        ProfileCard(
            profile = Users(
                id = "1",
                name = "Ziberiano Peralta",
                birthDate = "1990-01-01"
            ),
            state = ProfileUiState(
                isFavorite = true,
                isBlockedByMe = false,
                hasBlockedMe = false
            ),
            userStatus = UserStatus.LastSeen("últ. vez 12/01/2026 a las 16:55"),
            distanceLabel = "1600.5 km",
            photoList = listOf("url1", "url2", "url3"),
            onToggleFavorite = {},
            onOpenPhoto = {}
        )
    }
}
