package com.example.minimusic.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension

/**
 * Serves albumart content URIs (media/external/audio/albumart rows) from the
 * OS thumbnail cache instead of decoding multi-megabyte embedded art per row.
 *
 * This is the Gramophone pattern, reimplemented for our Coil 2 stack: below
 * [MaxThumbEdgePx], [android.content.ContentResolver.loadThumbnail] returns
 * the system's pre-scaled, pre-cached bitmap (sub-millisecond after first
 * generation) rather than parsing the whole audio file. List flings stop
 * hitching on cold decodes; the upfront app-open warmup fills the cache
 * before the first scroll instead of trickling for minutes.
 *
 * Anything else — full-size player art, non-album URIs, pre-29 devices —
 * returns null so the default Coil chain handles it untouched.
 */
class AlbumThumbFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        if (!isAlbumArtUri(uri)) return null
        val size = options.size
        val widthPx = (size.width as? Dimension.Pixels)?.px ?: return null
        val heightPx = (size.height as? Dimension.Pixels)?.px ?: return null
        // Showcase-size requests keep the full decode path: thumbnails would
        // visibly soften the full-bleed player art.
        if (widthPx <= 0 || heightPx <= 0 || minOf(widthPx, heightPx) > MaxThumbEdgePx) return null
        return try {
            val bitmap: Bitmap = context.contentResolver.loadThumbnail(
                uri,
                Size(widthPx, heightPx),
                /* cancellationSignal= */ null
            )
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } catch (_: Exception) {
            // Thumbnail generation can fail for missing/odd providers; fall
            // through to the default chain rather than showing a blank tile.
            null
        }
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher =
            AlbumThumbFetcher(options.context, data, options)
    }

    companion object {
        const val MaxThumbEdgePx = 768

        fun isAlbumArtUri(uri: Uri): Boolean =
            uri.scheme == "content" &&
                uri.authority == "media" &&
                (uri.pathSegments.firstOrNull() == "external") &&
                uri.pathSegments.getOrNull(1) == "audio" &&
                uri.pathSegments.getOrNull(2) == "albumart"
    }
}

/**
 * App-owned Coil stack with the album-thumbnail fetcher registered. Everything
 * that shows or warms artwork must go through here so list, grid, player and
 * warmup requests share one memory cache with matching size keys.
 */
object MiniMusicImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(appContext)
                .components { add(AlbumThumbFetcher.Factory()) }
                .crossfade(false)
                .allowHardware(true)
                .build()
                .also { instance = it }
        }
    }
}
