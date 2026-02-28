package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ZibeSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supportingText: String? = null
) {
    val zibeExtendedColors = LocalZibeExtendedColors.current
    val corner = dimensionResource(DsR.dimen.corner_medium)
    val horizontal = dimensionResource(DsR.dimen.element_spacing_medium)
    val vertical = dimensionResource(DsR.dimen.element_spacing_small)

    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(zibeExtendedColors.contentLightBg)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null
            ) { onCheckedChange(!checked) }
            .padding(horizontal = horizontal, vertical = vertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = Color.White
            )
            if (!supportingText.isNullOrBlank()) {
                Spacer(Modifier.width(0.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = zibeExtendedColors.hintText
                )
            }
        }

        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ZibeSwitchRowPreview() {
    ZibeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ZibeSwitchRow(
                title = "Notifications",
                checked = true,
                enabled = true,
                onCheckedChange = {}
            )
            ZibeSwitchRow(
                title = "Dark Mode",
                checked = false,
                enabled = true,
                onCheckedChange = {}
            )
            ZibeSwitchRow(
                title = "Bio",
                supportingText = "Show your bio on your profile",
                checked = true,
                enabled = true,
                onCheckedChange = {}
            )
            ZibeSwitchRow(
                title = "Disabled Option",
                checked = false,
                enabled = false,
                onCheckedChange = {}
            )
        }
    }
}



