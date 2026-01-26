package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ZibeButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    buttonColors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = colorResource(R.color.zibe_btn_primary),
        contentColor = colorResource(R.color.white)
    )
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
            .heightIn(min = buttonHeight),
        shape = MaterialTheme.shapes.medium,
        // Eliminamos padding vertical para que el icono no empuje la altura
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = buttonElevation,
            pressedElevation = pressedElevation,
            disabledElevation = 0.dp
        ),
        colors = buttonColors
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
                    // Dejamos el tinte null para Facebook/Google (colores originales) o usamos el proporcionado
                    tint = iconTint ?: Color.Unspecified,
                    modifier = Modifier.size(40.dp) // Tamaño original de los assets de redes sociales
                )
                Spacer(modifier = Modifier.width(spacingSmall))
            }
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ZibeButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    ZibeButtonPrimary(
        text = text,
        onClick = onClick,
        modifier = modifier,
        iconRes = iconRes,
        iconTint = iconTint,
        enabled = enabled,
        isLoading = isLoading,
        buttonColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun ZibeButtonOutlined(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    buttonColors: ButtonColors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface
    )
) {
    val spacingSmall = dimensionResource(R.dimen.element_spacing_small)
    val buttonHeight = dimensionResource(R.dimen.zibe_btn_height)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = buttonHeight),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        colors = buttonColors
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
                    tint = iconTint ?: Color.Unspecified,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(spacingSmall))
            }
            Text(
                text = text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
fun ZibeButtonsComparisonPreview() {
    ZibeTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .width(300.dp)
        ) {
            Text("Botón sin Icono (Referencia)", style = MaterialTheme.typography.labelSmall)
            ZibeButtonPrimary(
                text = "ENTRAR",
                onClick = {}
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text("Botones con Iconos (40dp)", style = MaterialTheme.typography.labelSmall)
            ZibeButtonOutlined(
                text = "Continuar con Google",
                onClick = {},
                iconRes = R.drawable.ic_google
            )
            
            Spacer(Modifier.height(8.dp))
            
            ZibeButtonOutlined(
                text = "Continuar con Facebook",
                onClick = {},
                iconRes = R.drawable.ic_facebook
            )
        }
    }
}

@Composable
@Preview
fun ZibeButtonPrimaryPreview() {
    ZibeTheme {
        ZibeButtonPrimary(
            text = "Zibe Button",
            onClick = { /* Acción de prueba */ }
        )
    }
}

@Composable
@Preview
fun ZibeButtonSecondaryPreview() {
    ZibeTheme {
        ZibeButtonSecondary(
            text = "Secondary Button",
            onClick = {}
        )
    }
}

@Composable
@Preview
fun ZibeButtonOutlinedPreview() {
    ZibeTheme {
        ZibeButtonOutlined(
            text = "Outlined Button",
            onClick = {}
        )
    }
}
