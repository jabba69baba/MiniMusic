package com.example.minimusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.media3.common.Player
import com.example.minimusic.MainActivity
import com.example.minimusic.R
import com.example.minimusic.playback.MusicService
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** Offline launcher widget with compact and expanded RemoteViews size classes. */
class MiniMusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (MusicService.isRunning) {
            runCatching {
                context.startService(Intent(context, MusicService::class.java).setAction(MusicService.ACTION_UPDATE_WIDGET))
            }
        } else {
            ids.forEach { widgetId -> updateDefault(context, manager, widgetId, manager.getAppWidgetOptions(widgetId)) }
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
                    context.startActivity(openAppIntent(context, openPlayer = false).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }

    companion object {
        const val ACTION_PREVIOUS = "com.example.minimusic.widget.PREVIOUS"
        const val ACTION_PLAY_PAUSE = "com.example.minimusic.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.minimusic.widget.NEXT"
        private const val MAX_ARTWORK_EDGE = 512
        private val artworkExecutor = Executors.newSingleThreadExecutor()
        private val updateGeneration = ConcurrentHashMap<Int, Long>()
        private val generationCounter = AtomicLong(0L)
        private val artworkCache = object : LinkedHashMap<String, Bitmap>(6, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > 4
        }

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
                ids.forEach { widgetId -> updateDefault(context, manager, widgetId, manager.getAppWidgetOptions(widgetId)) }
            }
        }

        fun updateFromPlayer(context: Context, player: Player) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MiniMusicWidgetProvider::class.java)
            val artworkUri = player.mediaMetadata.artworkUri
            manager.getAppWidgetIds(component).forEach { widgetId ->
                val generation = generationCounter.incrementAndGet()
                updateGeneration[widgetId] = generation
                val options = manager.getAppWidgetOptions(widgetId)
                val views = newViews(context, options, player)
                manager.updateAppWidget(widgetId, views)
                if (artworkUri != null && player.currentMediaItem != null) {
                    artworkExecutor.execute {
                        val artwork = loadArtwork(context, artworkUri)
                        if (artwork != null && updateGeneration[widgetId] == generation) {
                            views.setImageViewBitmap(R.id.widget_art, artwork)
                            manager.updateAppWidget(widgetId, views)
                        }
                    }
                }
            }
        }

        private fun updateDefault(context: Context, manager: AppWidgetManager, widgetId: Int, options: Bundle) {
            updateGeneration[widgetId] = generationCounter.incrementAndGet()
            val views = RemoteViews(context.packageName, layoutFor(options))
            views.setImageViewResource(R.id.widget_art, R.drawable.widget_idle_logo)
            views.setImageViewResource(R.id.widget_play, R.drawable.ic_widget_play)
            configureClicks(context, views, openPlayer = false)
            manager.updateAppWidget(widgetId, views)
        }

        private fun newViews(context: Context, options: Bundle, player: Player): RemoteViews {
            val views = RemoteViews(context.packageName, layoutFor(options))
            val active = player.currentMediaItem != null
            views.setImageViewResource(R.id.widget_art, if (active) R.drawable.widget_note else R.drawable.widget_idle_logo)
            views.setImageViewResource(
                R.id.widget_play,
                if (player.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            configureClicks(context, views, openPlayer = active)
            return views
        }

        private fun configureClicks(context: Context, views: RemoteViews, openPlayer: Boolean) {
            val openIntent = openAppPendingIntent(context, openPlayer)
            views.setOnClickPendingIntent(R.id.widget_root, openIntent)
            views.setOnClickPendingIntent(R.id.widget_art, openIntent)
            views.setOnClickPendingIntent(R.id.widget_play, broadcastPendingIntent(context, ACTION_PLAY_PAUSE, 2))
            views.setOnClickPendingIntent(R.id.widget_previous, broadcastPendingIntent(context, ACTION_PREVIOUS, 3))
            views.setOnClickPendingIntent(R.id.widget_next, broadcastPendingIntent(context, ACTION_NEXT, 4))
        }

        private fun openAppPendingIntent(context: Context, openPlayer: Boolean): PendingIntent = PendingIntent.getActivity(
            context,
            if (openPlayer) 11 else 12,
            openAppIntent(context, openPlayer),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun openAppIntent(context: Context, openPlayer: Boolean): Intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_PLAYER, openPlayer)
            .putExtra(MainActivity.EXTRA_WIDGET_OPEN, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

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

        private fun loadArtwork(context: Context, uri: Uri): Bitmap? {
            val key = uri.toString()
            synchronized(artworkCache) { artworkCache[key]?.let { return it } }
            return try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_ARTWORK_EDGE) sample *= 2
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val source = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: return null
                val side = minOf(source.width, source.height)
                val cropped = Bitmap.createBitmap(source, (source.width - side) / 2, (source.height - side) / 2, side, side)
                val output = if (cropped.width == MAX_ARTWORK_EDGE) cropped else Bitmap.createScaledBitmap(cropped, MAX_ARTWORK_EDGE, MAX_ARTWORK_EDGE, true)
                if (cropped !== output) cropped.recycle()
                source.recycle()
                synchronized(artworkCache) { artworkCache[key] = output }
                output
            } catch (_: Exception) {
                null
            }
        }
    }
}
