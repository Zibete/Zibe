package com.zibete.proyecto1.ui.components

import LocalZibeExtendedColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.ZibeTheme

/**
 * ZibePrimaryFAB: Extended Floating Action Button que utiliza los colores principales
 * de la marca (fondo acento/pink y texto claro).
 */
@Composable
fun ZibePrimaryFAB(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val extendedColors = LocalZibeExtendedColors.current
    ZibeBaseFAB(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
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
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    ZibeBaseFAB(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * Componente base para los FABs de Zibe, asegurando consistencia en dimensiones y formas.
 */
@Composable
private fun ZibeBaseFAB(
    text: @Composable (() -> Unit),
    icon: @Composable (() -> Unit),
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color,
    contentColor: Color
) {
    val extendedColors = LocalZibeExtendedColors.current
    
    // El FAB se considera deshabilitado si enabled es false o si está cargando
    val isEffectivelyEnabled = enabled && !isLoading
    
    // Aplicamos transparencia al contenedor y color muted al contenido si está deshabilitado
    val finalContainerColor = if (isEffectivelyEnabled) containerColor else containerColor.copy(alpha = 0.24f)
    val finalContentColor = if (isEffectivelyEnabled) contentColor else extendedColors.hintText

    ExtendedFloatingActionButton(
        onClick = { if (isEffectivelyEnabled) onClick() },
        modifier = modifier
            .heightIn(min = dimensionResource(R.dimen.zibe_btn_height))
            .widthIn(min = dimensionResource(R.dimen.zibe_extfab_min_width)),
        shape = RoundedCornerShape(dimensionResource(R.dimen.zibe_btn_corner)),
        containerColor = finalContainerColor,
        contentColor = finalContentColor,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = if (isEffectivelyEnabled) dimensionResource(R.dimen.zibe_btn_elevation) else 0.dp,
            pressedElevation = if (isEffectivelyEnabled) dimensionResource(R.dimen.zibe_btn_elevation_pressed) else 0.dp
        ),
        icon = {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = finalContentColor
                )
            } else {
                icon()
            }
        },
        text = text
    )
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
                text = { Text("Guardar Cambios") },
                icon = { Icon(Icons.Default.Save, null) },
                onClick = {}
            )
            ZibePrimaryFAB(
                text = { Text("Guardando...") },
                icon = { Icon(Icons.Default.Save, null) },
                onClick = {},
                isLoading = true
            )
            ZibePrimaryFAB(
                text = { Text("Deshabilitado") },
                icon = { Icon(Icons.Default.Save, null) },
                onClick = {},
                enabled = false
            )
            ZibeSecondaryFAB(
                text = { Text("Añadir Foto") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = {}
            )
            ZibeSecondaryFAB(
                text = { Text("Cargando...") },
                icon = { Icon(Icons.Default.Add, null) },
                onClick = {},
                isLoading = true
            )
        }
    }
}
