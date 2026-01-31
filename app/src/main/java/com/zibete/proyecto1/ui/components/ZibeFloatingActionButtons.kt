package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

/**
 * ZibePrimaryFAB: Extended Floating Action Button que utiliza los colores principales
 * de la marca (fondo acento/pink y texto claro).
 */
@Composable
fun ZibePrimaryFAB(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val extendedColors = LocalZibeExtendedColors.current
    ZibeBaseFAB(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = extendedColors.accent,
        contentColor = extendedColors.lightText
    )
}

/**
 * ZibeSecondaryFAB: Extended Floating Action Button con un esquema basado en "surface"
 * para acciones menos prominentes o contrastantes.
 */
@Composable
fun ZibeSecondaryFAB(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ZibeBaseFAB(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * Componente base para los FABs de Zibe, asegurando consistencia en dimensiones y formas.
 */
@Composable
private fun ZibeBaseFAB(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color,
    contentColor: Color
) {
    val extendedColors = LocalZibeExtendedColors.current
    
    // Aplicamos transparencia al contenedor y color muted al contenido si está deshabilitado
    val finalContainerColor = if (enabled) containerColor else containerColor.copy(alpha = 0.24f)
    val finalContentColor = if (enabled) contentColor else extendedColors.hintText

    ExtendedFloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .heightIn(min = dimensionResource(R.dimen.zibe_btn_height))
            .widthIn(min = dimensionResource(R.dimen.zibe_extfab_min_width)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.zibe_btn_corner)),
        containerColor = finalContainerColor,
        contentColor = finalContentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (enabled) dimensionResource(R.dimen.zibe_btn_elevation) else 0.dp,
            pressedElevation = if (enabled) dimensionResource(R.dimen.zibe_btn_elevation_pressed) else 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.zibe_extfab_icon_padding)))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ZibeFABsPreview() {
    ZibeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ZibePrimaryFAB(
                text = "Guardar Cambios",
                icon = Icons.Default.Save,
                onClick = {}
            )
            ZibePrimaryFAB(
                text = "Deshabilitado",
                icon = Icons.Default.Save,
                onClick = {},
                enabled = false
            )
            ZibeSecondaryFAB(
                text = "Añadir Foto",
                icon = Icons.Default.Add,
                onClick = {}
            )
            ZibeSecondaryFAB(
                text = "Deshabilitado",
                icon = Icons.Default.Add,
                onClick = {},
                enabled = false
            )
        }
    }
}
