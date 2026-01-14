package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R

@Composable
fun ZibeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val buttonHeight = dimensionResource(R.dimen.zibe_btn_height)
    val buttonElevation = dimensionResource(R.dimen.zibe_btn_elevation)
    val pressedElevation = dimensionResource(R.dimen.zibe_btn_elevation_pressed)
    val spacingSmall = dimensionResource(R.dimen.element_spacing_small)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight),
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = buttonElevation,
            pressedElevation = pressedElevation,
            disabledElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = colorResource(R.color.zibe_btn_primary),
            contentColor = colorResource(R.color.white)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
                color = LocalContentColor.current
            )
        } else {
            iconRes?.let { res ->
                Icon(
                    painter = painterResource(id = res),
                    contentDescription = null,
                    tint = iconTint ?: Color.Unspecified
                )
                Spacer(modifier = Modifier.width(spacingSmall))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
@Preview
fun ZibeButtonPreview() {
    ZibeButton(
        text = "Zibe Button",
        onClick = { /* Acción de prueba */ }
    )
}
