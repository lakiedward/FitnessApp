package com.example.fitnessapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest

@Composable
fun GifImage(
    resId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current

    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
        }
        .build()

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(resId)
            .build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = modifier
    )
}