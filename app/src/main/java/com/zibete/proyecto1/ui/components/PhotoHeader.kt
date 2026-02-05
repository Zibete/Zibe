package com.zibete.proyecto1.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.zibete.proyecto1.R

@Composable
fun PhotoHeader(
    photoUrl: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val painter = rememberAsyncImagePainter(model = photoUrl)
    val loading = isLoading || painter.state is AsyncImagePainter.State.Loading

    ZibeCard(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clickable { onClick() },
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.content_description_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (loading) {
                ZibeCircularProgress()
            }
        }
    }
}
