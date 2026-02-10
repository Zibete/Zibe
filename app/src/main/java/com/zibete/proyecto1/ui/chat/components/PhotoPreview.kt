package com.zibete.proyecto1.ui.chat.components

import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.zibete.proyecto1.R
import com.zibete.proyecto1.ui.ResizableImageView
import com.zibete.proyecto1.ui.components.ZibeCard
import com.zibete.proyecto1.ui.components.ZibeCircularProgress
import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun ZibePhotoPreview(
    uri: Uri,
    isUploading: Boolean,
    onRemove: () -> Unit
) {

    ZibeCard(
        contentPadding = PaddingValues(0.dp),
        border = null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                factory = { context ->
                    ResizableImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                },
                update = { view ->
                    Glide.with(view)
                        .load(uri)
                        .into(view)
                }
            )

            if (isUploading) {
                ZibeCircularProgress(
                    modifier = Modifier.align(Alignment.Center),
                    strokeWidth = 2.dp
                )
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    tint = LocalZibeExtendedColors.current.lightText,
                    contentDescription = stringResource(R.string.action_cancel)
                )
            }
        }
    }
}

@Preview(name = "PhotoPreview_Ready", showBackground = true)
@Composable
private fun ZibePhotoPreviewReady() {
    ZibeTheme {
        ZibePhotoPreview(
            uri = Uri.parse("content://preview/photo"),
            isUploading = false,
            onRemove = {}
        )
    }
}

@Preview(name = "PhotoPreview_Uploading", showBackground = true)
@Composable
private fun ZibePhotoPreviewUploading() {
    ZibeTheme {
        ZibePhotoPreview(
            uri = Uri.parse("content://preview/photo"),
            isUploading = true,
            onRemove = {}
        )
    }
}
