package com.zibete.proyecto1.ui.editprofile

import LocalZibeExtendedColors
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.SheetHeader
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.theme.LocalZibeTypography
import com.zibete.proyecto1.ui.theme.ZibeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoSourceBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    ZibeBottomSheet(
        isOpen = isOpen,
        onCancel = onDismiss,
        sheetState = sheetState,
        showCancelButton = false,
        content = {
            val zibeTypography = LocalZibeTypography.current
            
            SheetHeader(
                title = stringResource(id = R.string.edit_picture),
                subtitle = stringResource(R.string.content_description_edit_photo),
                titleStyle = zibeTypography.h1,
                subtitleStyle = zibeTypography.subtitle
            )

            // Grid de 2 columnas solo para las fuentes de imagen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_spacing_xs))
            ) {
                PhotoSourceMainItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CameraAlt,
                    label = stringResource(id = R.string.camera),
                    onClick = {
                        onCameraClick()
                        onDismiss()
                    }
                )
                PhotoSourceMainItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PhotoLibrary,
                    label = stringResource(id = R.string.gallery),
                    onClick = {
                        onGalleryClick()
                        onDismiss()
                    }
                )
            }

            // Opción de eliminar: separada, más pequeña y sutil
            onDeleteClick?.let {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.element_spacing_xs)))
                DeletePhotoAction(
                    onClick = {
                        it()
                        onDismiss()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    )
}

@Composable
private fun PhotoSourceMainItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val zibeExtendedColors = LocalZibeExtendedColors.current
    val zibeTypography = LocalZibeTypography.current

    ZibeCard(
        modifier = modifier,
        onClick = onClick,
        containerColor = zibeExtendedColors.contentLightBg,
        elevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            zibeExtendedColors.accent.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = zibeExtendedColors.accent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = zibeTypography.actionLabel,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DeletePhotoAction(
    onClick: () -> Unit
) {
    val zibeColors = LocalZibeExtendedColors.current
    val zibeTypography = LocalZibeTypography.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = zibeColors.snackRed.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.delete),
            style = zibeTypography.label,
            color = zibeColors.snackRed.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PhotoSourceBottomSheetPreview() {
    ZibeTheme {
        PhotoSourceBottomSheet(
            isOpen = true,
            onDismiss = {},
            onCameraClick = {},
            onGalleryClick = {},
            onDeleteClick = {},
            sheetState = rememberModalBottomSheetState()
        )
    }
}
