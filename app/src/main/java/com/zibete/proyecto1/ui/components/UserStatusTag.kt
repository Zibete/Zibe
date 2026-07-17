package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.ui.theme.LocalZibeTypography

enum class UserStatusTagType {
    DISTANCE,
    FAVORITE,
    BLOCKED_BY_ME,
    HAS_BLOCKED_ME,
    SILENCED,
    GROUP_MATCH
}

@Composable
fun UserStatusTag(
    type: UserStatusTagType,
    text: String,
    modifier: Modifier = Modifier
) {
    val palette = userStatusTagPalette(type)
    val tagTextSize = dimensionResource(DsR.dimen.tag_text_size).value.sp
    val iconSize = dimensionResource(DsR.dimen.tag_icon_size)
    val paddingX = dimensionResource(DsR.dimen.element_spacing_xs)
    val paddingY = dimensionResource(DsR.dimen.layout_margin_xs)
    val iconSpacing = dimensionResource(DsR.dimen.layout_margin_small)
    val strokeWidth = dimensionResource(DsR.dimen.popup_menu_stroke)
    val typography = LocalZibeTypography.current

    Surface(
        modifier = modifier.testTag("user_status_tag_${type.name.lowercase()}"),
        shape = RoundedCornerShape(15.dp),
        color = palette.background,
        contentColor = palette.foreground,
        border = BorderStroke(strokeWidth, palette.stroke)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = paddingX, vertical = paddingY),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(iconSpacing)
        ) {
            Icon(
                imageVector = palette.icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = palette.foreground
            )
            Text(
                text = text,
                style = typography.label.copy(fontSize = tagTextSize),
                color = palette.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun userStatusTagPalette(type: UserStatusTagType): UserStatusTagPalette = when (type) {
    UserStatusTagType.DISTANCE -> UserStatusTagPalette(
        icon = Icons.Filled.LocationOn,
        foreground = colorResource(DsR.color.tag_location_fg),
        background = colorResource(DsR.color.tag_location_bg),
        stroke = colorResource(DsR.color.tag_location_stroke)
    )

    UserStatusTagType.FAVORITE -> UserStatusTagPalette(
        icon = Icons.Filled.Star,
        foreground = colorResource(DsR.color.tag_favorite_fg),
        background = colorResource(DsR.color.tag_favorite_bg),
        stroke = colorResource(DsR.color.tag_favorite_stroke)
    )

    UserStatusTagType.BLOCKED_BY_ME,
    UserStatusTagType.HAS_BLOCKED_ME -> UserStatusTagPalette(
        icon = Icons.Filled.Block,
        foreground = colorResource(DsR.color.tag_blocked_fg),
        background = colorResource(DsR.color.tag_blocked_bg),
        stroke = colorResource(DsR.color.tag_blocked_stroke)
    )

    UserStatusTagType.SILENCED -> UserStatusTagPalette(
        icon = Icons.Filled.NotificationsOff,
        foreground = colorResource(DsR.color.tag_silenced_fg),
        background = colorResource(DsR.color.tag_silenced_bg),
        stroke = colorResource(DsR.color.tag_silenced_stroke)
    )

    UserStatusTagType.GROUP_MATCH -> UserStatusTagPalette(
        icon = Icons.Filled.Groups,
        foreground = colorResource(DsR.color.tag_group_match_fg),
        background = colorResource(DsR.color.tag_group_match_bg),
        stroke = colorResource(DsR.color.tag_group_match_stroke)
    )
}

private data class UserStatusTagPalette(
    val icon: ImageVector,
    val foreground: Color,
    val background: Color,
    val stroke: Color
)
