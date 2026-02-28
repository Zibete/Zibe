package com.zibete.proyecto1.ui.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.components.SheetHeader
import com.zibete.proyecto1.ui.components.ZibeBottomSheet
import com.zibete.proyecto1.ui.theme.ZibeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPhotoSourceSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    ZibeBottomSheet(
        isOpen = isOpen,
        onCancel = onDismiss,
        showCancelButton = false
    ) {
        SheetHeader(title = stringResource(R.string.send_photo))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(DsR.dimen.element_spacing_xs))
        ) {
            PhotoSourceItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CameraAlt,
                label = stringResource(R.string.camera),
                onClick = {
                    onCameraClick()
                    onDismiss()
                }
            )
            PhotoSourceItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PhotoLibrary,
                label = stringResource(R.string.gallery),
                onClick = {
                    onGalleryClick()
                    onDismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "ChatPhotoSourceSheet_Open", showBackground = true)
@Composable
private fun ChatPhotoSourceSheetPreview() {
    ZibeTheme {
        ChatPhotoSourceSheet(
            isOpen = true,
            onDismiss = {},
            onCameraClick = {},
            onGalleryClick = {}
        )
    }
}



