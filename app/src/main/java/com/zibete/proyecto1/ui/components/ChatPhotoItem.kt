package com.zibete.proyecto1.ui.components

import com.zibete.proyecto1.ui.theme.LocalZibeExtendedColors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.zibete.proyecto1.R

@Composable
fun ChatPhotoItem(
    url: String,
    onClick: () -> Unit
) {
    val zibeColors = LocalZibeExtendedColors.current
    Card(
        onClick = onClick,
        modifier = Modifier.size(96.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, zibeColors.accent.copy(alpha = 0.18f))
    ) {
        val painter = rememberAsyncImagePainter(model = url)
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.photo_received),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}