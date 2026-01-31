package com.zibete.proyecto1.ui.editprofile

import LocalZibeExtendedColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.SheetHeader
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.components.ZibeCard
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
    val zibeExtendedColors = LocalZibeExtendedColors.current

    ZibeBottomSheet(
        isOpen = isOpen,
        onCancel = onDismiss,
        sheetState = sheetState,
        content = {

            SheetHeader(
                title = stringResource(id = R.string.edit_picture),
                subtitle = "Elegí una fuente para subir tu foto de perfil"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.element_spacing_small)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDeleteClick != null) {
                    PhotoSourceItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Delete,
                        label = stringResource(id = R.string.delete),
                        onClick = {
                            onDeleteClick()
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.element_spacing_small)))
                }

                PhotoSourceItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CameraAlt,
                    label = stringResource(id = R.string.camera),
                    onClick = {
                        onCameraClick()
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.element_spacing_small)))

                PhotoSourceItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PhotoLibrary,
                    label = stringResource(id = R.string.gallery),
                    onClick = {
                        onGalleryClick()
                        onDismiss()
                    }
                )
            }
        }
    )
}

@Composable
private fun PhotoSourceItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val zibeExtendedColors = LocalZibeExtendedColors.current

    ZibeCard(
        modifier = modifier.clickable { onClick() },
        containerColor = zibeExtendedColors.contentLightBg,
        elevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = zibeExtendedColors.lightText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = zibeExtendedColors.lightText
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PhotoSourceItemPreview() {
    ZibeTheme {
        PhotoSourceItem(
            modifier = Modifier
                .padding(16.dp)
                .width(120.dp),
            icon = Icons.Default.CameraAlt,
            label = "Cámara",
            onClick = {}
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
