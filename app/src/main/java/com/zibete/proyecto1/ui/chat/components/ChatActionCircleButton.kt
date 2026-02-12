package com.zibete.proyecto1.ui.chat.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.ZibeCircularProgress
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ChatActionCircleButton(
    iconVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColorOverride: Color? = null
) {

    val zibeExtendedColors = LocalZibeExtendedColors.current
    val containerColor = containerColorOverride ?: zibeExtendedColors.contentDarkBg
    val contentColor = zibeExtendedColors.lightText
    val chatComponentsHeight = dimensionResource(R.dimen.zibe_btn_height)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(chatComponentsHeight),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        if (isLoading) {
            ZibeCircularProgress(size = 20.dp, strokeWidth = 2.dp)
        } else {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatActionCircleButtonPreview() {
    ZibeTheme {
        ChatActionCircleButton(
            iconVector = Icons.AutoMirrored.Filled.Send,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatActionCircleButtonLoadingPreview() {
    ZibeTheme {
        ChatActionCircleButton(
            iconVector = Icons.AutoMirrored.Filled.Send,
            onClick = {},
            isLoading = true,
            enabled = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatActionCircleButtonDisabledPreview() {
    ZibeTheme {
        ChatActionCircleButton(
            iconVector = Icons.AutoMirrored.Filled.Send,
            onClick = {},
            enabled = false
        )
    }
}
