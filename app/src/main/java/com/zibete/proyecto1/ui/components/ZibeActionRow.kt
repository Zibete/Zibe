package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ActionRow(
    title: String,
    subtitle: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val arrowDown = painterResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
    val zibeBtnCorner = dimensionResource(DsR.dimen.zibe_btn_corner)

    val zibeExtendedColors = LocalZibeExtendedColors.current

    ListItem(
        headlineContent = { Text(text = title, color = zibeExtendedColors.lightText) },
        supportingContent = {
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = zibeExtendedColors.hintText)
            }
        },
        trailingContent = {
            Icon(
                painter = arrowDown,
                contentDescription = null,
                tint = zibeExtendedColors.lightText
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clip(shape = RoundedCornerShape(size = zibeBtnCorner))
            .background(color = zibeExtendedColors.contentLightBg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null
            ) { onClick() }
    )
}

@Preview(name = "Action Row with Subtitle", showBackground = true)
@Composable
fun ActionRowWithSubtitlePreview() {
    ZibeTheme {
        ActionRow(
            title = "Action Title",
            subtitle = "Action Subtitle",
            enabled = true,
            onClick = {}
        )
    }
}

@Preview(name = "Action Row without Subtitle", showBackground = true)
@Composable
fun ActionRowWithoutSubtitlePreview() {
    ZibeTheme {
        ActionRow(
            title = "Action Title",
            subtitle = null,
            enabled = true,
            onClick = {}
        )
    }
}



