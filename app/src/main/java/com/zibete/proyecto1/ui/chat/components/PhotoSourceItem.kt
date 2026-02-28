package com.zibete.proyecto1.ui.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zibete.proyecto1.core.designsystem.R as DsR
import com.zibete.proyecto1.ui.theme.ZibeTheme

@Composable
fun PhotoSourceItem(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = colorResource(DsR.color.zibe_night_start)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = colorResource(DsR.color.blanco)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colorResource(DsR.color.blanco)
            )
        }
    }
}

@Preview(name = "PhotoSourceItem_Camera", showBackground = true)
@Composable
private fun PhotoSourceItemPreviewCamera() {
    ZibeTheme {
        PhotoSourceItem(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.CameraAlt,
            label = "Camera",
            onClick = {}
        )
    }
}

@Preview(name = "PhotoSourceItem_Gallery", showBackground = true)
@Composable
private fun PhotoSourceItemPreviewGallery() {
    ZibeTheme {
        PhotoSourceItem(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.PhotoLibrary,
            label = "Gallery",
            onClick = {}
        )
    }
}



