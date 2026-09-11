package com.example.minimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun AlbumArtImage(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape,
    iconSize: Dp = 28.dp,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    // No bitmap fades anywhere in the app: art swaps the instant it decodes
    // and motion comes from slides/scales. List rows land pre-warmed, so the
    // swap is rarely even visible.
    crossfadeMillis: Int = 0,
    requestSizePx: Int? = null
) {
    val context = LocalContext.current
    // App-owned stack: album rows resolve from the OS thumbnail cache with
    // matching size keys, so list, grid and warmup share one memory cache.
    val imageLoader = remember(context) { MiniMusicImageLoader.get(context) }
    val request = remember(model) {
        ImageRequest.Builder(context)
            .data(model)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .apply {
                requestSizePx?.let { size(it) }
                if (crossfadeMillis > 0) crossfade(crossfadeMillis) else crossfade(false)
            }
            .build()
    }
    var imageFailed by remember(model) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (model != null) {
            AsyncImage(
                model = request,
                imageLoader = imageLoader,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) imageFailed = true
                }
            )
        }
        if (model == null || imageFailed) {
            MissingAlbumArtIcon(iconSize)
        }
    }
}

@Composable
private fun MissingAlbumArtIcon(size: Dp) {
    Icon(
        imageVector = Icons.Filled.MusicNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(size)
    )
}
