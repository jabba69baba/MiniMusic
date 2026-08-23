package com.example.minimusic.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.core.content.res.ResourcesCompat
import com.example.minimusic.R
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
private fun QueueActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    artColors: ArtColorRoles,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(artColors.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = artColors.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = artColors.onPrimaryContainer,
            modifier = Modifier.padding(start = 7.dp)
        )
    }
}

/** Dedicated queue destination; the list is laid out below a fixed header. */
@Composable
fun QueueScreen(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    onBack: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onReorderEntries: (List<Long>) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onClearQueue: () -> Unit
) {
    BackHandler(onBack = onBack)
    var locateRequest by remember { mutableStateOf(0) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        color = artColors.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .background(
                            artColors.onSurfaceVariant.copy(alpha = 0.55f),
                            RoundedCornerShape(50)
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = onClearQueue,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Clear queue",
                            modifier = Modifier.size(21.dp),
                            tint = artColors.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.size(28.dp))
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
                    Spacer(modifier = Modifier.size(28.dp))
                    androidx.compose.material3.IconButton(
                        onClick = { locateRequest++ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Locate current song",
                            modifier = Modifier.size(21.dp),
                            tint = artColors.onSurfaceVariant
                        )
                    }
                }
            }
            QueueDrawerList(
                snapshot = snapshot,
                artColors = artColors,
                onEntryClick = onEntryClick,
                onReorderEntries = onReorderEntries,
                onRemoveEntry = onRemoveEntry,
                locateRequest = locateRequest,
                queueTopRequest = 0
            )
        }
    }
}

@Composable
fun BoxWithConstraintsScope.QueueDrawer(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEntryClick: (Long) -> Unit,
    onReorderEntries: (List<Long>) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onClearQueue: () -> Unit
) {
    val density = LocalDensity.current
    val fullHeightPx = with(density) { maxHeight.toPx() }
    val collapsedBarHeightPx = with(density) { QueueDrawerCollapsedHeight.toPx() }
    val navBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val openOffsetPx = fullHeightPx * (1f - OPEN_FRACTION)
    val closedOffsetPx = fullHeightPx - collapsedBarHeightPx - navBarHeightPx
    val offsetY = remember { Animatable(closedOffsetPx) }
    var locateRequest by remember { mutableStateOf(0) }
    var queueTopRequest by remember { mutableStateOf(0) }
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
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val headerHeight = if (isOpen) 102.dp else QueueDrawerCollapsedHeight
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                val newValue = (offsetY.value + delta).coerceIn(openOffsetPx, closedOffsetPx)
                                scope.launch { offsetY.snapTo(newValue) }
                            },
                            // Let child action pills receive taps; the drawer still
                            // begins dragging as soon as the pointer moves.
                            startDragImmediately = false,
                            onDragStopped = { velocity ->
                                val shouldOpen = if (abs(velocity) > 800f) {
                                    velocity < 0f
                                } else {
                                    offsetY.value < (openOffsetPx + closedOffsetPx) / 2f
                                }
                                val target = if (shouldOpen) openOffsetPx else closedOffsetPx
                                scope.launch {
                                    offsetY.animateTo(
                                        target,
                                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                                    )
                                }
                                onOpenChange(shouldOpen)
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 4.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .background(artColors.onSurfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(50))
                    )
                    if (isOpen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { queueTopRequest++ }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QueueActionPill(
                                icon = Icons.Filled.Delete,
                                label = "Clear",
                                contentDescription = "Clear queue",
                                artColors = artColors,
                                onClick = onClearQueue,
                                modifier = Modifier.weight(1f)
                            )
                            QueueActionPill(
                                icon = Icons.Filled.MyLocation,
                                label = "Locate",
                                contentDescription = "Locate current song",
                                artColors = artColors,
                                onClick = { locateRequest++ },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenChange(true) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                    }
                }

                if (isOpen) {
                    QueueDrawerList(
                        snapshot = snapshot,
                        artColors = artColors,
                        onEntryClick = onEntryClick,
                        onReorderEntries = onReorderEntries,
                        onRemoveEntry = onRemoveEntry,
                        locateRequest = locateRequest,
                        queueTopRequest = queueTopRequest
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.QueueDrawerList(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    onEntryClick: (Long) -> Unit,
    onReorderEntries: (List<Long>) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    locateRequest: Int,
    queueTopRequest: Int
) {
    val context = LocalContext.current
    val latestOnEntryClick by rememberUpdatedState(onEntryClick)
    val latestOnReorderEntries by rememberUpdatedState(onReorderEntries)
    val latestOnRemoveEntry by rememberUpdatedState(onRemoveEntry)
    val adapter = remember { PracticalQueueAdapter(context) }
    var previousCurrentEntryId by remember { mutableStateOf<Long?>(null) }
    var previousLocateRequest by remember { mutableStateOf(0) }
    var previousQueueTopRequest by remember { mutableStateOf(0) }

    // Keep the RecyclerView visually below the fixed drawer header; this
    // boundary prevents rows from painting over the Queue title or controls.
    Spacer(modifier = Modifier.height(8.dp))
    androidx.compose.material3.HorizontalDivider(
        color = artColors.onSurfaceVariant.copy(alpha = 0.28f)
    )
    Spacer(modifier = Modifier.height(8.dp))

    adapter.onEntryClick = latestOnEntryClick
    adapter.onReorderEntries = latestOnReorderEntries
    adapter.onRemoveEntry = latestOnRemoveEntry
    adapter.artColors = artColors
    adapter.submitSnapshot(snapshot)

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clipToBounds(),
        factory = { viewContext ->

            val recyclerView = RecyclerView(viewContext).apply {
                layoutManager = LinearLayoutManager(viewContext)
                setHasFixedSize(true)
                overScrollMode = View.OVER_SCROLL_NEVER
                clipToPadding = true
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
            if (snapshot.currentEntryId != previousCurrentEntryId) {
                previousCurrentEntryId = snapshot.currentEntryId
                recyclerView.post {
                    val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
                    val currentPosition = snapshot.resolvedVisiblePosition
                    if (currentPosition < 0) return@post
                    val lastVisible = layout.findLastVisibleItemPosition()
                    if (currentPosition > 0 && currentPosition >= lastVisible) {
                        layout.scrollToPositionWithOffset(currentPosition, 0)
                    }
                }
            }
            if (locateRequest != previousLocateRequest) {
                previousLocateRequest = locateRequest
                recyclerView.post {
                    val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
                    val currentPosition = snapshot.resolvedVisiblePosition
                    if (currentPosition >= 0) {
                        layout.scrollToPositionWithOffset(currentPosition, 0)
                    }
                }
            }
            if (queueTopRequest != previousQueueTopRequest) {
                previousQueueTopRequest = queueTopRequest
                recyclerView.post {
                    val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
                    if (adapter.itemCount > 0) {
                        layout.scrollToPositionWithOffset(0, 0)
                    }
                }
            }
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
    private var isDragging = false
    private var draggedEntryId: Long? = null
    private var deferredSnapshot: QueueSnapshot? = null
    private var awaitingReorderIds: List<Long>? = null
    private var releaseSubmitted = false
    var artColors: ArtColorRoles? = null
    var onEntryClick: (Long) -> Unit = {}
    var onReorderEntries: (List<Long>) -> Unit = {}
    var onRemoveEntry: (Long) -> Unit = {}
    var startDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    fun submitSnapshot(snapshot: QueueSnapshot) {
        val incomingIds = snapshot.visibleEntries.map { it.entryId }
        awaitingReorderIds?.let { expectedIds ->
            if (incomingIds != expectedIds) return
            awaitingReorderIds = null
        }
        if (isDragging) {
            deferredSnapshot = snapshot
            return
        }
        val displayEntries = snapshot.visibleEntries
        val oldIds = entries.map { it.entryId }
        val newIds = displayEntries.map { it.entryId }
        val orderChanged = oldIds != newIds
        val stateChanged = currentEntryId != snapshot.currentEntryId ||
            currentPosition != snapshot.resolvedVisiblePosition
        entries = displayEntries
        currentEntryId = snapshot.currentEntryId
        currentPosition = snapshot.resolvedVisiblePosition
        if (orderChanged) notifyDataSetChanged()
        else if (stateChanged && entries.isNotEmpty()) {
            notifyItemRangeChanged(0, entries.size, PAYLOAD_STATE)
        }
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

    fun beginDrag(holder: RecyclerView.ViewHolder) {
        isDragging = true
        draggedEntryId = holder.bindingAdapterPosition
            .takeIf { it != RecyclerView.NO_POSITION }
            ?.let { position -> entries.getOrNull(position)?.entryId }
        deferredSnapshot = null
        releaseSubmitted = false
    }

    fun endDrag() {
        isDragging = false
        draggedEntryId = null
        // The deferred snapshots describe the queue before the released order
        // is committed. Drop them; the controller publishes one authoritative
        // snapshot after the deferred reorder completes.
        deferredSnapshot = null
    }

    private fun moveLocal(from: Int, to: Int) {
        if (from !in entries.indices || to !in entries.indices || from == to) return
        val moved = entries.toMutableList().apply { add(to, removeAt(from)) }
        entries = moved
        notifyItemMoved(from, to)
    }

    inner class MoveCallback : ItemTouchHelper.Callback() {
        private var autoScrollPosted = false

        override fun isLongPressDragEnabled(): Boolean = false
        override fun isItemViewSwipeEnabled(): Boolean = false

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                viewHolder.setIsRecyclable(false)
                beginDrag(viewHolder)
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            if (!releaseSubmitted) {
                releaseSubmitted = true
                val submittedIds = entries.map { it.entryId }
                awaitingReorderIds = submittedIds
                onReorderEntries(submittedIds)
                // If the controller rejects the request because the queue changed
                // concurrently, do not leave the adapter waiting forever.
                viewHolder.itemView.postDelayed({
                    if (awaitingReorderIds == submittedIds) awaitingReorderIds = null
                }, 750L)
            }
            viewHolder.setIsRecyclable(true)
            endDrag()
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = draggedEntryId?.let { id ->
                entries.indexOfFirst { it.entryId == id }
            } ?: viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition
            if (from !in entries.indices || to !in entries.indices) return false
            moveLocal(from, to)
            return true
        }

        override fun getBoundingBoxMargin(): Int = context.dp(72)

        override fun onChildDraw(
            canvas: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            if (actionState != ItemTouchHelper.ACTION_STATE_DRAG || !isCurrentlyActive || autoScrollPosted) {
                return
            }

            val edge = context.dp(72)
            val projectedTop = viewHolder.itemView.top + dY
            val projectedBottom = viewHolder.itemView.bottom + dY
            val scrollDistance = when {
                projectedBottom > recyclerView.height - edge -> {
                    ((projectedBottom - (recyclerView.height - edge)) / 3f)
                        .roundToInt()
                        .coerceIn(context.dp(8), context.dp(36))
                }
                projectedTop < edge -> {
                    -((edge - projectedTop) / 3f)
                        .roundToInt()
                        .coerceIn(context.dp(8), context.dp(36))
                }
                else -> 0
            }
            if (scrollDistance == 0 || !recyclerView.canScrollVertically(if (scrollDistance > 0) 1 else -1)) {
                return
            }

            autoScrollPosted = true
            recyclerView.post {
                autoScrollPosted = false
                if (isDragging && recyclerView.isAttachedToWindow) {
                    recyclerView.scrollBy(0, scrollDistance)
                }
            }
        }

        override fun interpolateOutOfBoundsScroll(
            recyclerView: RecyclerView,
            viewSize: Int,
            viewSizeOutOfBounds: Int,
            totalSize: Int,
            msSinceStartScroll: Long
        ): Int {
            if (viewSizeOutOfBounds == 0) return 0
            val direction = if (viewSizeOutOfBounds > 0) 1 else -1
            val distance = abs(viewSizeOutOfBounds).coerceAtLeast(context.dp(12))
            val step = (context.dp(12) + distance / 5).coerceAtMost(context.dp(48))
            return direction * step
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
    }

    class Holder(
        context: Context,
        initialColors: ArtColorRoles?
    ) : RecyclerView.ViewHolder(LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.dp(72)
        )
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

            close.layoutParams = LinearLayout.LayoutParams(context.dp(40), context.dp(40))
            close.setImageResource(R.drawable.ic_queue_remove)
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
            val card = GradientDrawable().apply {
                setColor(rowColor.toArgb())
                cornerRadius = root.context.dp(16).toFloat()
            }
            root.background = InsetDrawable(
                card,
                root.context.dp(8),
                root.context.dp(4),
                root.context.dp(8),
                root.context.dp(4)
            )
            // Played history rows fade as a single visual unit. The current row
            // remains fully opaque and is identified only by its highlighted card.
            root.alpha = if (isHistory) 0.58f else 1f
            title.text = entry.song.title
            artist.text = entry.song.artist
            title.setTypeface(ResourcesCompat.getFont(root.context, R.font.google_sans_flex_medium))
            artist.setTypeface(ResourcesCompat.getFont(root.context, R.font.google_sans_flex_regular))
            title.setTextColor(resolved.onSurface.toArgb())
            artist.setTextColor(resolved.onSurfaceVariant.toArgb())
            title.textSize = 16f
            artist.textSize = 14f
            artwork.setImageResource(android.R.drawable.ic_menu_gallery)
            entry.song.albumArtUri?.let { uri ->
                imageLoader.enqueue(
                    ImageRequest.Builder(root.context)
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
