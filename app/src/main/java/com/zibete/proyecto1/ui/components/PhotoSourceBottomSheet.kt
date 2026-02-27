package com.zibete.proyecto1.ui.components

import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.foundation.BorderStroke
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

            SheetHeader(
                title = stringResource(id = R.string.edit_picture),
                subtitle = stringResource(R.string.content_description_edit_photo)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(DsR.dimen.element_spacing_xs))
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

            onDeleteClick?.let {
                Spacer(modifier = Modifier.height(dimensionResource(DsR.dimen.element_spacing_xs)))
                DeletePhotoAction(
                    onClick = {
                        it()
                        onDismiss()
                    }
                )
            }
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
        border = BorderStroke(
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
                modifier = Modifier.size(40.dp),
                tint = zibeExtendedColors.lightText
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
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = stringResource(R.string.action_delete_photo),
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


