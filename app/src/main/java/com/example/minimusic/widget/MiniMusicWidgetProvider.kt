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
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.BitmapShader
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.minimusic.MainActivity
import com.example.minimusic.R
import com.example.minimusic.playback.MusicService
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors

/**
 * Offline home-screen music widget. The launcher supplies the actual cell bounds;
 * the RemoteViews layout keeps the artwork square and scales the controls for larger spans.
 */
class MiniMusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId, newOptions)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREVIOUS, ACTION_PLAY_PAUSE, ACTION_NEXT -> dispatchPlaybackCommand(context, intent.action!!)
            ACTION_OPEN_APP -> {
                // The PendingIntent attached to the widget handles this action directly.
            }
        }
    }

    companion object {
        const val ACTION_PREVIOUS = "com.example.minimusic.widget.PREVIOUS"
        const val ACTION_PLAY_PAUSE = "com.example.minimusic.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.minimusic.widget.NEXT"
        const val ACTION_OPEN_APP = "com.example.minimusic.widget.OPEN_APP"

        private val executor = Executors.newSingleThreadExecutor()

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, MiniMusicWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) updateWidgets(context, ids)
        }

        private fun updateWidgets(context: Context, ids: IntArray) {
            val manager = AppWidgetManager.getInstance(context)
            ids.forEach { id ->
                updateWidget(context, manager, id, manager.getAppWidgetOptions(id))
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            options: Bundle
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music)
            val widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val expanded = widthDp >= 180 || heightDp >= 180
            views.setViewVisibility(R.id.widget_metadata, if (expanded) View.VISIBLE else View.GONE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent(context, ACTION_OPEN_APP, 1))
            views.setOnClickPendingIntent(R.id.widget_play, pendingIntent(context, ACTION_PLAY_PAUSE, 2))
            views.setOnClickPendingIntent(R.id.widget_previous, pendingIntent(context, ACTION_PREVIOUS, 3))
            views.setOnClickPendingIntent(R.id.widget_next, pendingIntent(context, ACTION_NEXT, 4))
            manager.updateAppWidget(widgetId, views)

            val token = SessionToken(context, ComponentName(context, MusicService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                try {
                    val controller = future.get()
                    val metadata = controller.mediaMetadata
                    val title = metadata.title?.toString().orEmpty()
                    val artist = metadata.artist?.toString().orEmpty()
                    views.setTextViewText(R.id.widget_title, title.ifBlank { context.getString(R.string.app_name) })
                    views.setTextViewText(R.id.widget_artist, artist.ifBlank { context.getString(R.string.widget_no_song) })
                    views.setImageViewResource(
                        R.id.widget_play,
                        if (controller.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                    )
                    val artUri = metadata.artworkUri
                    if (artUri != null) {
                        executor.execute {
                            val bitmap = loadCircularArtwork(context, artUri)
                            if (bitmap != null) {
                                views.setImageViewBitmap(R.id.widget_art, bitmap)
                                manager.updateAppWidget(widgetId, views)
                            }
                        }
                    }
                    controller.release()
                    manager.updateAppWidget(widgetId, views)
                } catch (_: Exception) {
                    views.setImageViewResource(R.id.widget_art, R.drawable.widget_note)
                    manager.updateAppWidget(widgetId, views)
                }
            }, MoreExecutors.directExecutor())
        }

        private fun dispatchPlaybackCommand(context: Context, action: String) {
            val token = SessionToken(context, ComponentName(context, MusicService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                try {
                    val controller = future.get()
                    when (action) {
                        ACTION_PREVIOUS -> controller.seekToPreviousMediaItem()
                        ACTION_PLAY_PAUSE -> if (controller.isPlaying) controller.pause() else controller.play()
                        ACTION_NEXT -> controller.seekToNextMediaItem()
                    }
                    controller.release()
                } catch (_: Exception) {
                    // The widget remains usable when playback is not connected yet.
                }
            }, MoreExecutors.directExecutor())
        }

        private fun pendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MiniMusicWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun loadCircularArtwork(context: Context, uri: android.net.Uri): Bitmap? {
            return try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val source = BitmapFactory.decodeStream(input) ?: return null
                    val side = minOf(source.width, source.height)
                    val crop = Bitmap.createBitmap(
                        source,
                        (source.width - side) / 2,
                        (source.height - side) / 2,
                        side,
                        side
                    )
                    val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(output)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = BitmapShader(crop, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                    }
                    canvas.drawCircle(side / 2f, side / 2f, side / 2f, paint)
                    output
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
