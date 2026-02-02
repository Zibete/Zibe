package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZibeToolbar(
    title: String,
    onBack: () -> Unit,
    showSkipButton: Boolean = false,
    onSkipClick: () -> Unit = {},
    menuItems: List<ZibeMenuItem> = emptyList()
) {
    var showMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = LocalZibeTypography.current.brandSubtitle
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = stringResource(R.string.content_description_back)
                )
            }
        },
        actions = {
            if (showSkipButton) {
                TextButton(onClick = onSkipClick) {
                    Text(
                        text = "Saltar",
                        style = LocalZibeTypography.current.actionLabel,
                        color = colorResource(R.color.zibe_text_light)
                    )
                }
            }

            if (menuItems.isNotEmpty()) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = colorResource(R.color.zibe_text_light)
                        )
                    }

                    // Menú con diseño moderno. 
                    // Se usa MaterialTheme.shapes para evitar el fondo cuadrado por defecto de Material 3
                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(colorResource(R.color.zibe_surface).copy(alpha = 0.92f))
                                .padding(vertical = 4.dp),
                            containerColor = Color.Transparent, // Evita el doble fondo
                            shadowElevation = 8.dp
                        ) {
                            menuItems.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item.label,
                                            style = LocalZibeTypography.current.label.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = colorResource(R.color.zibe_text_light)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        item.onClick()
                                    },
                                    leadingIcon = item.icon?.let {
                                        {
                                            Icon(
                                                imageVector = it,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = colorResource(R.color.zibe_text_light).copy(alpha = 0.7f)
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = colorResource(R.color.zibe_text_light),
            navigationIconContentColor = colorResource(R.color.zibe_text_light),
            actionIconContentColor = colorResource(R.color.zibe_text_light)
        )
    )
}

data class ZibeMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

@Preview(name = "Toolbar with Skip", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ZibeToolbarSkipPreview() {
    ZibeTheme {
        ZibeToolbar(
            title = "Editar Perfil",
            onBack = {},
            showSkipButton = true
        )
    }
}

@Preview(name = "Toolbar with Menu", showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ZibeToolbarMenuPreview() {
    ZibeTheme {
        ZibeToolbar(
            title = "Zibe App",
            onBack = {},
            menuItems = listOf(
                ZibeMenuItem("Ajustes", {}, Icons.Default.MoreVert), // Ejemplo con icono
                ZibeMenuItem("Cerrar Sesión", {})
            )
        )
    }
}
