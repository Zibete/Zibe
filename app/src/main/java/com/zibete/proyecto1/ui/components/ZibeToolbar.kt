package com.zibete.proyecto1.ui.components

import com.zibete.proyecto1.core.designsystem.R as DsR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
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

    val zibeExtendedColors = LocalZibeExtendedColors.current

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
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.content_description_back)
                )
            }
        },
        actions = {
            if (showSkipButton) {
                TextButton(onClick = onSkipClick) {
                    Text(
                        text = stringResource(R.string.action_skip),
                        style = LocalZibeTypography.current.actionLabel,
                        color = zibeExtendedColors.lightText
                    )
                }
            }

            if (menuItems.isNotEmpty()) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = zibeExtendedColors.lightText
                        )
                    }

                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(
                            extraSmall = RoundedCornerShape(ZibeMenuDefaults.Corner)
                        )
                    ) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(
                                    colorResource(DsR.color.zibe_surface).copy(
                                        alpha = ZibeMenuDefaults.BgAlpha
                                    )
                                )
                                .padding(vertical = 4.dp),
                            offset = ZibeMenuDefaults.Offset,
                            containerColor = Color.Transparent,
                            shadowElevation = ZibeMenuDefaults.Shadow
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
                                            color = zibeExtendedColors.lightText
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
                                                tint = zibeExtendedColors.lightText.copy(alpha = 0.7f)
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
            titleContentColor = zibeExtendedColors.lightText,
            navigationIconContentColor = zibeExtendedColors.lightText,
            actionIconContentColor = zibeExtendedColors.lightText
        )
    )
}

data class ZibeMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null
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


