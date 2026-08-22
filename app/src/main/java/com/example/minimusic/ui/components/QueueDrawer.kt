package com.example.minimusic.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.QueueEntry
import com.example.minimusic.playback.QueueSnapshot
import com.example.minimusic.ui.theme.ArtColorRoles
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Height of the always-visible collapsed bar (handle + "Queue" label). */
val QueueDrawerCollapsedHeight = 48.dp
private const val OPEN_FRACTION = 0.82f

@Composable
fun BoxWithConstraintsScope.QueueDrawer(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEntryClick: (Long) -> Unit,
    onMoveEntry: (Long, Int) -> Unit,
    onRemoveEntry: (Long) -> Unit
) {
    val density = LocalDensity.current
    val fullHeightPx = with(density) { maxHeight.toPx() }
    val collapsedBarHeightPx = with(density) { QueueDrawerCollapsedHeight.toPx() }
    val navBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val openOffsetPx = fullHeightPx * (1f - OPEN_FRACTION)
    val closedOffsetPx = fullHeightPx - collapsedBarHeightPx - navBarHeightPx
    val offsetY = remember { Animatable(closedOffsetPx) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isOpen, fullHeightPx) {
        val target = if (isOpen) openOffsetPx else closedOffsetPx
        offsetY.animateTo(target, animationSpec = tween(280, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = artColors.surfaceVariant,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val newValue = (offsetY.value + delta).coerceIn(openOffsetPx, closedOffsetPx)
                        scope.launch { offsetY.snapTo(newValue) }
                    },
                    startDragImmediately = true,
                    onDragStopped = { velocity ->
                        val shouldOpen = if (abs(velocity) > 800f) {
                            velocity < 0f
                        } else {
                            offsetY.value < (openOffsetPx + closedOffsetPx) / 2f
                        }
                        val target = if (shouldOpen) openOffsetPx else closedOffsetPx
                        scope.launch {
                            offsetY.animateTo(target, animationSpec = tween(240, easing = FastOutSlowInEasing))
                        }
                        onOpenChange(shouldOpen)
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChange(!isOpen) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 8.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .background(artColors.onSurfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(50))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.QueueMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = artColors.onSurface
                        )
                        Text(
                            text = "Queue",
                            style = MaterialTheme.typography.titleMedium,
                            color = artColors.onSurface,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                val isSubstantiallyOpen = offsetY.value < closedOffsetPx - collapsedBarHeightPx / 2f
                if (isSubstantiallyOpen) {
                    QueueDrawerList(
                        snapshot = snapshot,
                        artColors = artColors,
                        onEntryClick = {
                            onEntryClick(it)
                            onOpenChange(false)
                        },
                        onMoveEntry = onMoveEntry,
                        onRemoveEntry = onRemoveEntry
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueDrawerList(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    onEntryClick: (Long) -> Unit,
    onMoveEntry: (Long, Int) -> Unit,
    onRemoveEntry: (Long) -> Unit
) {
    val context = LocalContext.current
    val latestOnEntryClick by rememberUpdatedState(onEntryClick)
    val latestOnMoveEntry by rememberUpdatedState(onMoveEntry)
    val latestOnRemoveEntry by rememberUpdatedState(onRemoveEntry)
    val adapter = remember { PracticalQueueAdapter(context) }

    adapter.onEntryClick = latestOnEntryClick
    adapter.onMoveEntry = latestOnMoveEntry
    adapter.onRemoveEntry = latestOnRemoveEntry
    adapter.artColors = artColors
    adapter.submitSnapshot(snapshot)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { viewContext ->
            val recyclerView = RecyclerView(viewContext).apply {
                layoutManager = LinearLayoutManager(viewContext)
                setHasFixedSize(true)
                overScrollMode = View.OVER_SCROLL_NEVER
            }
            val touchHelper = ItemTouchHelper(adapter.MoveCallback())
            adapter.startDrag = { holder -> touchHelper.startDrag(holder) }
            recyclerView.adapter = adapter
            touchHelper.attachToRecyclerView(recyclerView)
            recyclerView
        },
        update = { recyclerView ->
            recyclerView.itemAnimator = null
            adapter.submitSnapshot(snapshot)
        }
    )
}

private class PracticalQueueAdapter(
    private val context: Context
) : RecyclerView.Adapter<PracticalQueueAdapter.Holder>() {
    private var entries: List<QueueEntry> = emptyList()
    private var currentEntryId: Long? = null
    private var currentPosition: Int = -1
    private var isPlaying = false
    var artColors: ArtColorRoles? = null
    var onEntryClick: (Long) -> Unit = {}
    var onMoveEntry: (Long, Int) -> Unit = { _, _ -> }
    var onRemoveEntry: (Long) -> Unit = {}
    var startDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    fun submitSnapshot(snapshot: QueueSnapshot) {
        val oldIds = entries.map { it.entryId }
        val newIds = snapshot.entries.map { it.entryId }
        val orderChanged = oldIds != newIds
        entries = snapshot.entries
        currentEntryId = snapshot.currentEntryId
        currentPosition = snapshot.resolvedCurrentPosition
        if (orderChanged) notifyDataSetChanged()
        else if (entries.isNotEmpty()) notifyItemRangeChanged(0, entries.size, PAYLOAD_STATE)
    }

    override fun getItemId(position: Int): Long = entries[position].entryId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(context, artColors)

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = entries[position]
        holder.bind(
            entry = entry,
            isCurrent = entry.entryId == currentEntryId ||
                (currentEntryId == null && position == currentPosition),
            isHistory = currentPosition >= 0 && position < currentPosition,
            colors = artColors,
            onClick = { onEntryClick(entry.entryId) },
            onRemove = { onRemoveEntry(entry.entryId) },
            onStartDrag = { startDrag?.invoke(holder) }
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int = entries.size

    private fun moveLocal(from: Int, to: Int) {
        if (from !in entries.indices || to !in entries.indices || from == to) return
        val moved = entries.toMutableList().apply { add(to, removeAt(from)) }
        entries = moved
        notifyItemMoved(from, to)
    }

    inner class MoveCallback : ItemTouchHelper.Callback() {
        override fun isLongPressDragEnabled(): Boolean = false
        override fun isItemViewSwipeEnabled(): Boolean = false

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
            val entryId = entries.getOrNull(from)?.entryId ?: return false
            moveLocal(from, to)
            onMoveEntry(entryId, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
    }

    class Holder(
        context: Context,
        initialColors: ArtColorRoles?
    ) : RecyclerView.ViewHolder(LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = context.dp(64)
        setPadding(context.dp(8), context.dp(4), context.dp(8), context.dp(4))
    }) {
        private val root = itemView as LinearLayout
        private val handle = QueueHandleView(context)
        private val artwork = ImageView(context)
        private val title = TextView(context)
        private val artist = TextView(context)
        private val close = ImageButton(context)
        private val textColumn = LinearLayout(context)
        private val imageLoader = ImageLoader(context)

        init {
            handle.layoutParams = LinearLayout.LayoutParams(context.dp(32), ViewGroup.LayoutParams.MATCH_PARENT)
            root.addView(handle)

            artwork.layoutParams = LinearLayout.LayoutParams(context.dp(44), context.dp(44)).apply {
                marginEnd = context.dp(8)
            }
            artwork.scaleType = ImageView.ScaleType.CENTER_CROP
            root.addView(artwork)

            textColumn.orientation = LinearLayout.VERTICAL
            textColumn.gravity = Gravity.CENTER_VERTICAL
            textColumn.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            title.maxLines = 1
            title.ellipsize = TextUtils.TruncateAt.END
            artist.maxLines = 1
            artist.ellipsize = TextUtils.TruncateAt.END
            textColumn.addView(title)
            textColumn.addView(artist)
            root.addView(textColumn)

            close.layoutParams = LinearLayout.LayoutParams(context.dp(44), context.dp(44))
            close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            close.contentDescription = "Remove from queue"
            close.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            root.addView(close)
        }

        fun bind(
            entry: QueueEntry,
            isCurrent: Boolean,
            isHistory: Boolean,
            colors: ArtColorRoles?,
            onClick: () -> Unit,
            onRemove: () -> Unit,
            onStartDrag: () -> Unit
        ) {
            val resolved = colors ?: return
            val rowColor = if (isCurrent) resolved.primaryContainer else resolved.surface
            root.background = GradientDrawable().apply {
                setColor(rowColor.toArgb())
                cornerRadius = context.dp(16).toFloat()
            }
            title.text = if (isCurrent) "▶ ${entry.song.title}" else entry.song.title
            artist.text = entry.song.artist
            title.setTextColor(resolved.onSurface.toArgb())
            artist.setTextColor(resolved.onSurfaceVariant.copy(alpha = if (isHistory) 0.65f else 1f).toArgb())
            title.textSize = 16f
            artist.textSize = 14f
            artwork.setImageResource(android.R.drawable.ic_menu_gallery)
            entry.song.albumArtUri?.let { uri ->
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(uri)
                        .target(artwork)
                        .build()
                )
            }
            handle.dotColor = resolved.onSurfaceVariant.toArgb()
            handle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag()
                false
            }
            close.setColorFilter(resolved.onSurfaceVariant.toArgb())
            close.setOnClickListener { onRemove() }
            root.setOnClickListener { onClick() }
        }
    }

    private companion object {
        const val PAYLOAD_STATE = "queue_state"
    }
}

private class QueueHandleView(context: Context) : View(context) {
    var dotColor: Int = android.graphics.Color.GRAY
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paint.color = dotColor
        val radius = context.dp(2).toFloat()
        val xGap = context.dp(8).toFloat()
        val yGap = context.dp(7).toFloat()
        val startX = width / 2f - xGap / 2f
        val startY = height / 2f - yGap
        repeat(3) { row ->
            repeat(2) { column ->
                canvas.drawCircle(startX + column * xGap, startY + row * yGap, radius, paint)
            }
        }
    }
}

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
