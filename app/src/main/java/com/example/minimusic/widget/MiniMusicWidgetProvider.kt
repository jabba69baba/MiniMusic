package com.example.minimusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.BitmapShader
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.media3.common.Player
import com.example.minimusic.MainActivity
import com.example.minimusic.R
import com.example.minimusic.playback.MusicService
import java.util.LinkedHashMap
import java.util.concurrent.Executors

/** Offline launcher widget with compact and expanded RemoteViews size classes. */
class MiniMusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (MusicService.isRunning) {
            runCatching {
                context.startService(Intent(context, MusicService::class.java).setAction(MusicService.ACTION_UPDATE_WIDGET))
            }
        } else {
            ids.forEach { updateDefault(context, manager, it, manager.getAppWidgetOptions(it)) }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, manager, widgetId, newOptions)
        if (MusicService.isRunning) {
            runCatching {
                context.startService(Intent(context, MusicService::class.java).setAction(MusicService.ACTION_UPDATE_WIDGET))
            }
        } else {
            updateDefault(context, manager, widgetId, newOptions)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREVIOUS, ACTION_PLAY_PAUSE, ACTION_NEXT -> {
                if (MusicService.isRunning) {
                    runCatching {
                        context.startService(Intent(context, MusicService::class.java).setAction(intent.action))
                    }
                } else {
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_PREVIOUS = "com.example.minimusic.widget.PREVIOUS"
        const val ACTION_PLAY_PAUSE = "com.example.minimusic.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.minimusic.widget.NEXT"
        private const val MAX_ARTWORK_EDGE = 256

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MiniMusicWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            if (MusicService.isRunning) {
                runCatching {
                    context.startService(Intent(context, MusicService::class.java).setAction(MusicService.ACTION_UPDATE_WIDGET))
                }
            } else {
                ids.forEach { updateDefault(context, manager, it, manager.getAppWidgetOptions(it)) }
            }
        }
        private val artworkExecutor = Executors.newSingleThreadExecutor()
        private val artworkCache = object : LinkedHashMap<String, Bitmap>(6, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 4
        }

        fun updateFromPlayer(context: Context, player: Player) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MiniMusicWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { widgetId ->
                val options = manager.getAppWidgetOptions(widgetId)
                val views = newViews(context, options, player)
                manager.updateAppWidget(widgetId, views)
                val artUri = player.mediaMetadata.artworkUri
                if (artUri != null) {
                    artworkExecutor.execute {
                        val artwork = loadCircularArtwork(context, artUri)
                        if (artwork != null) {
                            views.setImageViewBitmap(R.id.widget_art, artwork)
                            manager.updateAppWidget(widgetId, views)
                        }
                    }
                }
            }
        }

        private fun updateDefault(context: Context, manager: AppWidgetManager, widgetId: Int, options: Bundle) {
            val views = RemoteViews(context.packageName, layoutFor(options))
            views.setTextViewText(R.id.widget_title, context.getString(R.string.app_name))
            views.setTextViewText(R.id.widget_artist, context.getString(R.string.widget_no_song))
            views.setImageViewResource(R.id.widget_art, R.drawable.widget_note)
            views.setImageViewResource(R.id.widget_play, R.drawable.ic_widget_play)
            configureClicks(context, views)
            manager.updateAppWidget(widgetId, views)
        }

        private fun newViews(context: Context, options: Bundle, player: Player): RemoteViews {
            val views = RemoteViews(context.packageName, layoutFor(options))
            val metadata = player.mediaMetadata
            views.setTextViewText(R.id.widget_title, metadata.title?.toString().orEmpty().ifBlank { context.getString(R.string.app_name) })
            views.setTextViewText(R.id.widget_artist, metadata.artist?.toString().orEmpty().ifBlank { context.getString(R.string.widget_no_song) })
            views.setImageViewResource(
                R.id.widget_play,
                if (player.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            configureClicks(context, views)
            return views
        }

        private fun configureClicks(context: Context, views: RemoteViews) {
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
            views.setOnClickPendingIntent(R.id.widget_play, broadcastPendingIntent(context, ACTION_PLAY_PAUSE, 2))
            views.setOnClickPendingIntent(R.id.widget_previous, broadcastPendingIntent(context, ACTION_PREVIOUS, 3))
            views.setOnClickPendingIntent(R.id.widget_next, broadcastPendingIntent(context, ACTION_NEXT, 4))
        }

        private fun openAppPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun broadcastPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, MiniMusicWidgetProvider::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun layoutFor(options: Bundle): Int {
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            return if (width >= 180 || height >= 180) R.layout.widget_music_expanded else R.layout.widget_music
        }

        private fun loadCircularArtwork(context: Context, uri: Uri): Bitmap? {
            val key = uri.toString()
            synchronized(artworkCache) { artworkCache[key]?.let { return it } }
            return try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                val largest = maxOf(bounds.outWidth, bounds.outHeight)
                val sample = Integer.highestOneBit((largest / MAX_ARTWORK_EDGE).coerceAtLeast(1)).coerceAtLeast(1)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val source = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOptions)
                } ?: return null
                val side = minOf(source.width, source.height)
                val crop = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
                val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
                Canvas(output).drawCircle(
                    side / 2f,
                    side / 2f,
                    side / 2f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = BitmapShader(crop, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    }
                )
                source.recycle()
                if (crop !== source) crop.recycle()
                synchronized(artworkCache) { artworkCache[key] = output }
                output
            } catch (_: Exception) {
                null
            }
        }
    }
}
