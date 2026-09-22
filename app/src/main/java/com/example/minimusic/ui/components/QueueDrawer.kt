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
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.example.minimusic.ui.components.LocalMiniMusicHaptics
import com.example.minimusic.ui.components.performMiniMusicHaptic
import com.example.minimusic.R
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import com.example.minimusic.data.model.Song
import com.example.minimusic.playback.QueueEntry
import com.example.minimusic.playback.QueueSnapshot
import com.example.minimusic.ui.theme.ArtColorRoles
import com.example.minimusic.ui.theme.LocalMiniMusicReducedMotion
import com.example.minimusic.ui.theme.MiniMusicMotion
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Height of the always-visible collapsed bar (handle + "Queue" label). */
val QueueDrawerCollapsedHeight = 48.dp

/** How much of the screen the open drawer covers. */
private const val OPEN_FRACTION = 0.82f

/**
 * The two places landscape's queue lives. They are separate slots by design —
 * the bar is a 72dp item at the bottom of the controls column so the player's
 * layout never moves, and the sheet is a pane-filling overlay — so each call
 * renders one of them and they animate together (see [LandscapeQueueContent]).
 */
enum class LandscapeQueueSlot { BAR, SHEET }

@Composable
private fun QueueActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            modifier = Modifier.padding(start = 7.dp)
        )
    }
}

@Composable
fun BoxWithConstraintsScope.QueueDrawer(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEntryClick: (Long) -> Unit,
    onReorderEntry: (Long, Int) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onClearQueue: () -> Unit,
    isLandscape: Boolean = false
) {
    if (isLandscape) {
        LandscapeQueueBottomSheet(
            snapshot = snapshot,
            artColors = artColors,
            isOpen = isOpen,
            onOpenChange = onOpenChange,
            onEntryClick = onEntryClick,
            onReorderEntry = onReorderEntry,
            onRemoveEntry = onRemoveEntry,
            onClearQueue = onClearQueue
        )
        return
    }

    // Portrait retains the existing bottom-sheet queue implementation.
    QueueDrawerBottomSheet(
        snapshot = snapshot,
        artColors = artColors,
        isOpen = isOpen,
        onOpenChange = onOpenChange,
        onEntryClick = onEntryClick,
        onReorderEntry = onReorderEntry,
        onRemoveEntry = onRemoveEntry,
        onClearQueue = onClearQueue
    )
}

@Composable
fun LandscapeQueueContent(
    modifier: Modifier = Modifier,
    slot: LandscapeQueueSlot,
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEntryClick: (Long) -> Unit,
    onReorderEntry: (Long, Int) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onClearQueue: () -> Unit
) {
    // One spring drives both slots. They are separate composables because they
    // occupy different places, so each owns an Animatable with the same spec;
    // both are created or retargeted on the same frame, and a spring is a
    // deterministic function of elapsed time, so the bar slides out of its slot
    // exactly as fast as the sheet rises over the pane. That is what turns the
    // old bar-vanishes/sheet-appears swap into one motion.
    val progress = remember { Animatable(if (isOpen) 1f else 0f) }
    LaunchedEffect(isOpen) {
        progress.animateTo(
            if (isOpen) 1f else 0f,
            animationSpec = MiniMusicMotion.defaultSpatial()
        )
    }

    when (slot) {
        LandscapeQueueSlot.BAR -> {
            val density = LocalDensity.current
            val barHeightPx = with(density) { 72.dp.toPx() }
            // Always composed, so its animation state survives the whole open
            // and close; while the sheet owns the pane the bar is clipped out
            // of its slot, fully transparent and inert to touch.
            Box(modifier = modifier.clipToBounds()) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = progress.value * barHeightPx
                            alpha = 1f - progress.value
                        }
                        .clickable(enabled = !isOpen) { onOpenChange(true) },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    // Opaque, like the rest of the drawer: the bar sits on the
                    // player canvas but is a solid surface, not a see-through
                    // overlay.
                    color = artColors.surfaceContainer,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .size(width = 36.dp, height = 4.dp)
                                .background(artColors.onSurfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(50))
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
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
        }

        LandscapeQueueSlot.SHEET -> {
            // The sheet is composed from the moment the drawer starts opening
            // until its own travel has finished, so it is still on screen to
            // ride out during the close instead of vanishing on the first frame.
            val sheetPresent by remember {
                derivedStateOf { isOpen || progress.value > 0.001f }
            }
            if (sheetPresent) {
                BoxWithConstraints(modifier = modifier) {
                    LandscapeQueueBottomSheet(
                        snapshot = snapshot,
                        artColors = artColors,
                        isOpen = isOpen,
                        onOpenChange = onOpenChange,
                        onEntryClick = onEntryClick,
                        onReorderEntry = onReorderEntry,
                        onRemoveEntry = onRemoveEntry,
                        onClearQueue = onClearQueue
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxWithConstraintsScope.LandscapeQueueBottomSheet(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEntryClick: (Long) -> Unit,
    onReorderEntry: (Long, Int) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onClearQueue: () -> Unit
) {
    val density = LocalDensity.current
    // PlayerScreen bounds this composable to the controls-side pane. The sheet
    // therefore fills that pane and slides vertically from its bottom edge.
    val panelHeight = maxHeight
    // The sheet retracts fully below the pane: landscape's collapsed state is
    // the bar's slot, not a strip of this sheet, so nothing of it may linger.
    val closedOffset = panelHeight
    val offsetY = remember(panelHeight) { Animatable(closedOffset.value) }
    val scope = rememberCoroutineScope()
    var locateRequest by remember { mutableStateOf(0) }
    val hapticView = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current
    var openRequest by remember { mutableStateOf(0) }

    LaunchedEffect(isOpen, panelHeight) {
        offsetY.animateTo(
            if (isOpen) 0f else closedOffset.value,
            animationSpec = MiniMusicMotion.defaultSpatial()
        )
    }

    // A new open cycle asks the list to place the current song. This counter
    // was declared and threaded through but never incremented, so the list's
    // locate-on-open branch had never once run — the drawer relied entirely on
    // a one-shot scroll issued while it was being created (see QueueDrawerList).
    LaunchedEffect(isOpen) {
        if (isOpen) openRequest++
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        0,
                        with(density) { offsetY.value.dp.toPx().roundToInt() }
                    )
                },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = artColors.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(102.dp)
                        .clickable(enabled = !isOpen) { onOpenChange(true) }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                val next = (offsetY.value + delta / density.density)
                                    .coerceIn(0f, closedOffset.value)
                                scope.launch { offsetY.snapTo(next) }
                            },
                            startDragImmediately = false,
                            onDragStopped = { velocity ->
                                val shouldOpen = if (abs(velocity) > 800f) {
                                    velocity < 0f
                                } else {
                                    offsetY.value < closedOffset.value / 2f
                                }
                                // One animator owns the settle, as in portrait:
                                // when the dragged state changes, the isOpen
                                // effect animates it; otherwise this hitches
                                // the sheet back open.
                                if (shouldOpen == isOpen) {
                                    scope.launch {
                                        offsetY.animateTo(
                                            if (shouldOpen) 0f else closedOffset.value,
                                            animationSpec = MiniMusicMotion.defaultSpatial()
                                        )
                                    }
                                } else {
                                    onOpenChange(shouldOpen)
                                }
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .background(artColors.onSurfaceVariant.copy(alpha = 0.55f), RoundedCornerShape(50))
                    )
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
                                .clickable {
                                    if (hapticsEnabled) hapticView.performMiniMusicHaptic()
                                    // The collapsed bar opens the drawer; the open
                                    // cycle itself re-centres on the active song.
                                    // The old "pin to top" jump was removed: the
                                    // active song now sits mid-viewport on open.
                                    if (!isOpen) onOpenChange(true)
                                }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Destructive = error tones; locate = the secondary
                        // family, so it never competes with the current song's
                        // primary-container row.
                        QueueActionPill(
                            icon = Icons.Filled.Delete,
                            label = "Clear",
                            contentDescription = "Clear queue",
                            containerColor = artColors.errorContainer,
                            contentColor = artColors.onErrorContainer,
                            onClick = { if (hapticsEnabled) hapticView.performMiniMusicHaptic(); onClearQueue() },
                            modifier = Modifier.weight(1f)
                        )
                        QueueActionPill(
                            icon = Icons.Filled.MyLocation,
                            label = "Locate",
                            contentDescription = "Locate current song",
                            containerColor = artColors.secondaryContainer,
                            contentColor = artColors.onSecondaryContainer,
                            onClick = { if (hapticsEnabled) hapticView.performMiniMusicHaptic(); locateRequest++ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isOpen) {
                    QueueDrawerList(
                        snapshot = snapshot,
                        artColors = artColors,
                        onEntryClick = onEntryClick,
                        onReorderEntry = onReorderEntry,
                        onRemoveEntry = onRemoveEntry,
                        locateRequest = locateRequest,
                        openRequest = openRequest,
                        onTopCloseDrag = {},
                        onTopCloseDragEnd = {}
                    )
                }
            }
        }

    }
}

@Composable
private fun BoxWithConstraintsScope.QueueDrawerBottomSheet(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onEntryClick: (Long) -> Unit,
    onReorderEntry: (Long, Int) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onClearQueue: () -> Unit
) {
    val density = LocalDensity.current
    val fullHeightPx = with(density) { maxHeight.toPx() }
    val collapsedBarHeightPx = with(density) { QueueDrawerCollapsedHeight.toPx() }
    val navBarHeightPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    // The panel is anchored to the bottom edge of the screen and its own height
    // does the travelling. That is what keeps the collapsed state honest: the
    // bar is a 48dp panel with a navigation-bar strip under it and literally no
    // room for the list, so no rows can peek below the bar, and the surface can
    // never be partly the player canvas while a drawer is meant to be closed.
    val collapsedPanelHeightPx = collapsedBarHeightPx + navBarHeightPx
    val openPanelHeightPx = (fullHeightPx * OPEN_FRACTION)
        .coerceAtLeast(collapsedPanelHeightPx + 1f)
    val panelHeight = remember { Animatable(collapsedPanelHeightPx) }
    var locateRequest by remember { mutableStateOf(0) }
    val hapticView = LocalView.current
    val hapticsEnabled = LocalMiniMusicHaptics.current
    var openRequest by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isOpen, fullHeightPx) {
        panelHeight.animateTo(
            if (isOpen) openPanelHeightPx else collapsedPanelHeightPx,
            animationSpec = MiniMusicMotion.defaultSpatial()
        )
    }

    // A new open cycle asks the list to place the current song. This counter
    // was declared and threaded through but never incremented, so the list's
    // locate-on-open branch had never once run — the drawer relied entirely on
    // a one-shot scroll issued while it was being created (see QueueDrawerList).
    LaunchedEffect(isOpen) {
        if (isOpen) openRequest++
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            // Opaque in both states. The drawer used to hand the collapsed bar
            // a 70% translucent art tone (matching the player's capsule track),
            // which let the player canvas read through the queue surface and
            // made the drawer look like it was still opening. One solid tone
            // for the whole surface instead.
            color = artColors.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { panelHeight.value.toDp() })
                .clipToBounds()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // One header recipe for both states. The bar grows from the
                // collapsed height to the open height on the same spring the
                // panel travels on, and its rows are revealed by the clip
                // instead of appearing in place — so opening and closing read
                // as one motion and nothing changes hue on the way.
                val headerHeight by animateDpAsState(
                    targetValue = if (isOpen) 102.dp else QueueDrawerCollapsedHeight,
                    animationSpec = MiniMusicMotion.defaultSpatial(),
                    label = "queueHeaderHeight"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .clipToBounds()
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                val newValue = (panelHeight.value - delta)
                                    .coerceIn(collapsedPanelHeightPx, openPanelHeightPx)
                                scope.launch { panelHeight.snapTo(newValue) }
                            },
                            // Let child action pills receive taps; the drawer still
                            // begins dragging as soon as the pointer moves.
                            startDragImmediately = false,
                            onDragStopped = { velocity ->
                                val shouldOpen = if (abs(velocity) > 800f) {
                                    velocity < 0f
                                } else {
                                    panelHeight.value >
                                        (collapsedPanelHeightPx + openPanelHeightPx) / 2f
                                }
                                // Only one animator may own the settle: when
                                // the dragged state actually changes, the
                                // isOpen effect animates it; otherwise the
                                // release snaps back here. Two concurrent
                                // animateTo calls on the same value made a
                                // drag-close look faster than a back-close.
                                if (shouldOpen == isOpen) {
                                    scope.launch {
                                        panelHeight.animateTo(
                                            if (shouldOpen) openPanelHeightPx else collapsedPanelHeightPx,
                                            animationSpec = MiniMusicMotion.defaultSpatial()
                                        )
                                    }
                                } else {
                                    onOpenChange(shouldOpen)
                                }
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 4.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .background(
                                artColors.onSurfaceVariant.copy(alpha = 0.55f),
                                RoundedCornerShape(50)
                            )
                    )
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
                                .clickable {
                                    if (hapticsEnabled) hapticView.performMiniMusicHaptic()
                                    // The collapsed bar opens the drawer; the open
                                    // cycle itself re-centres on the active song.
                                    // The old "pin to top" jump was removed: the
                                    // active song now sits mid-viewport on open.
                                    if (!isOpen) onOpenChange(true)
                                }
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
                            containerColor = artColors.errorContainer,
                            contentColor = artColors.onErrorContainer,
                            onClick = { if (hapticsEnabled) hapticView.performMiniMusicHaptic(); onClearQueue() },
                            modifier = Modifier.weight(1f)
                        )
                        QueueActionPill(
                            icon = Icons.Filled.MyLocation,
                            label = "Locate",
                            contentDescription = "Locate current song",
                            containerColor = artColors.secondaryContainer,
                            contentColor = artColors.onSecondaryContainer,
                            onClick = { if (hapticsEnabled) hapticView.performMiniMusicHaptic(); locateRequest++ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // The list appears with the panel's first pixel of growth and
                // leaves when the panel is fully collapsed again, so it is
                // created with a viewport to lay out in: the list's own
                // initial scroll to the current song needs a non-zero height to
                // work, which is why composing it early left the drawer opening
                // at the queue's first row.
                val listVisible by remember {
                    derivedStateOf { panelHeight.value > collapsedPanelHeightPx + 0.5f }
                }
                if (listVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clipToBounds()
                    ) {
                        // QueueDrawerList is a ColumnScope extension: the inner
                        // Column gives it that scope inside the clipping box.
                        Column(modifier = Modifier.fillMaxSize()) {
                            QueueDrawerList(
                                snapshot = snapshot,
                                artColors = artColors,
                                onEntryClick = onEntryClick,
                                onReorderEntry = onReorderEntry,
                                onRemoveEntry = onRemoveEntry,
                                locateRequest = locateRequest,
                                        openRequest = openRequest,
                                onTopCloseDrag = { deltaY ->
                                    scope.launch {
                                        panelHeight.snapTo(
                                            (panelHeight.value - deltaY)
                                                .coerceIn(collapsedPanelHeightPx, openPanelHeightPx)
                                        )
                                    }
                                },
                                onTopCloseDragEnd = { totalDistance ->
                                    val shouldClose = totalDistance > with(density) { 48.dp.toPx() }
                                    if (shouldClose) {
                                        // Single owner: the isOpen effect animates
                                        // the sheet closed, this drag only decides
                                        // the outcome.
                                        onOpenChange(false)
                                    } else {
                                        scope.launch {
                                            panelHeight.animateTo(
                                                openPanelHeightPx,
                                                animationSpec = MiniMusicMotion.defaultSpatial()
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                // Clearance for the system navigation bar, inside the panel and
                // below the list, in both states.
                Spacer(modifier = Modifier.height(with(density) { navBarHeightPx.toDp() }))
            }
        }
    }
}

@Composable
private fun ColumnScope.QueueDrawerList(
    snapshot: QueueSnapshot,
    artColors: ArtColorRoles,
    onEntryClick: (Long) -> Unit,
    onReorderEntry: (Long, Int) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    locateRequest: Int,
    openRequest: Int,
    onTopCloseDrag: (Float) -> Unit,
    onTopCloseDragEnd: (Float) -> Unit
) {
    val context = LocalContext.current
    val latestOnEntryClick by rememberUpdatedState(onEntryClick)
    val latestOnReorderEntry by rememberUpdatedState(onReorderEntry)
    val latestOnRemoveEntry by rememberUpdatedState(onRemoveEntry)
    val latestOnTopCloseDrag by rememberUpdatedState(onTopCloseDrag)
    val latestOnTopCloseDragEnd by rememberUpdatedState(onTopCloseDragEnd)
    val adapter = remember { PracticalQueueAdapter(context) }
    val latestSnapshot by rememberUpdatedState(snapshot)
    var previousLocateRequest by remember { mutableStateOf(locateRequest) }
    var previousOpenRequest by remember { mutableStateOf(0) }

    // Pending while the list still owes the current song a placement; set again
    // by every new open cycle, and on creation because a fresh list always means
    // a drawer is opening. The placement deliberately waits for a viewport at
    // least one row tall: the drawer grows its panel frame by frame, so this list
    // is created with a sliver of space, and a scroll issued then is swallowed by
    // the next layout — which is why the drawer used to open on the queue's first
    // row.
    var locatePending by remember { mutableStateOf(true) }
    // Rows are laid out 72dp tall in the adapter below.
    val rowHeightPx = remember(context) { 72f * context.resources.displayMetrics.density }
    // The glide below is a view-system animation, so it does not follow
    // Compose's own animation scale; reduced motion has to be honoured here
    // explicitly. See placeQueueInstantly.
    val reducedMotion = LocalMiniMusicReducedMotion.current
    // Follows the queue while the drawer is open: a song change re-centres on
    // the new now-playing row, so the drawer stays truthful without the user
    // touching it. Comparing entry ids (not positions) keeps a reorder or a
    // position tick from triggering it.
    var previousCurrentEntryId by remember { mutableStateOf(snapshot.currentEntryId) }

    /**
     * Offset that lands a row in the middle of the viewport, but never invents
     * a gap above the queue's own first row: the list can only show as much
     * history as it actually has, so a current song near the top still starts
     * flush.
     */
    fun centeredAnchorOffset(view: RecyclerView, position: Int): Int =
        queueAnchorOffset(view.height, rowHeightPx, adapter.itemCount, position)

    fun locateCurrentEntryIfReady(view: RecyclerView) {
        if (!locatePending || view.height < rowHeightPx) return
        val layout = view.layoutManager as? LinearLayoutManager ?: return
        val position = latestSnapshot.resolvedVisiblePosition
        if (position < 0) return
        locatePending = false
        view.stopScroll()
        layout.scrollToPositionWithOffset(position, centeredAnchorOffset(view, position))
    }

    // Keep the RecyclerView visually below the fixed drawer header; this
    // boundary prevents rows from painting over the Queue title or controls.
    Spacer(modifier = Modifier.height(8.dp))
    androidx.compose.material3.HorizontalDivider(
        color = artColors.outlineVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    adapter.onEntryClick = latestOnEntryClick
    adapter.onReorderEntry = latestOnReorderEntry
    adapter.onRemoveEntry = latestOnRemoveEntry
    adapter.artColors = artColors
    adapter.submitSnapshot(snapshot)

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clipToBounds(),
        factory = { viewContext ->

            val recyclerView = QueueRecyclerView(
                context = viewContext,
                onTopCloseDrag = latestOnTopCloseDrag,
                onTopCloseDragEnd = latestOnTopCloseDragEnd
            ).apply {
                layoutManager = LinearLayoutManager(viewContext)
                setHasFixedSize(true)
                setItemViewCacheSize(8)
                overScrollMode = View.OVER_SCROLL_NEVER
                clipToPadding = true
                itemAnimator = DefaultItemAnimator().apply {
                    removeDuration = 120L
                    moveDuration = 180L
                    addDuration = 140L
                    changeDuration = 140L
                    supportsChangeAnimations = false
                }
            }
            // Scroll to the current song only once this view is tall enough to
            // hold a row. Asking earlier is what silently failed: with no usable
            // viewport the layout manager consumes the pending position and lays
            // out nothing, so the list settles at row 0 and stays there as the
            // drawer finishes opening.
            recyclerView.addOnLayoutChangeListener { view, _, top, _, bottom, _, _, _, _ ->
                if (view is RecyclerView && bottom - top > 0) locateCurrentEntryIfReady(view)
            }
            val touchHelper = ItemTouchHelper(adapter.MoveCallback())
            adapter.startDrag = { holder -> touchHelper.startDrag(holder) }
            recyclerView.adapter = adapter
            touchHelper.attachToRecyclerView(recyclerView)
            recyclerView
        },
        update = { recyclerView ->
            adapter.submitSnapshot(snapshot)
            // Do not reposition on ordinary snapshot updates or while the
            // user is scrolling. The queue is located explicitly on open or
            // through the Locate action only.
            if (openRequest != previousOpenRequest) {
                previousOpenRequest = openRequest
                recyclerView.stopScroll()
                locatePending = true
                // If the drawer is already tall enough (a drag-open that has
                // grown past a row, or a reopen), place it now; otherwise the
                // layout listener above does it as soon as the panel allows.
                recyclerView.post { locateCurrentEntryIfReady(recyclerView) }
            }
            if (locateRequest != previousLocateRequest) {
                previousLocateRequest = locateRequest
                recyclerView.stopScroll()
                recyclerView.post {
                    recyclerView.stopScroll()
                    val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
                    val currentPosition = snapshot.resolvedVisiblePosition
                    if (currentPosition >= 0) {
                        if (reducedMotion) {
                            placeQueueInstantly(recyclerView, currentPosition, rowHeightPx)
                        } else {
                            animateQueueScroll(recyclerView, currentPosition, rowHeightPx)
                        }
                    }
                }
            }
            if (snapshot.currentEntryId != previousCurrentEntryId) {
                previousCurrentEntryId = snapshot.currentEntryId
                val currentPosition = snapshot.resolvedVisiblePosition
                // Only when the drawer is in repose: never yank the list out
                // from under a scroll or a drag the user is performing.
                if (currentPosition >= 0 &&
                    recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE
                ) {
                    recyclerView.post {
                        if (reducedMotion) {
                            placeQueueInstantly(recyclerView, currentPosition, rowHeightPx)
                        } else {
                            animateQueueScroll(recyclerView, currentPosition, rowHeightPx)
                        }
                    }
                }
            }
        }
    )
}

/**
 * Where a row's top should sit inside the queue viewport so it reads as
 * "centred", clamped to positions the list can actually reach.
 *
 * A row can only be pushed down the viewport as far as it has history above it
 * (otherwise the glide would try to scroll before the queue's first row) and
 * only as far as it has songs below it (otherwise it would try to scroll past
 * the last row). Without those two clamps the smooth scroller chases an
 * unreachable position forever and jams wherever it runs out of content, which
 * is exactly what left the bottom of the queue unreachable.
 */
private fun queueAnchorOffset(
    viewportPx: Int,
    rowHeightPx: Float,
    itemCount: Int,
    position: Int
): Int {
    if (viewportPx <= 0 || rowHeightPx <= 0f || itemCount <= 0 || position < 0) return 0
    val centered = ((viewportPx - rowHeightPx) / 2f).toInt()
    // Highest the row's top may sit, given the rows that exist above it.
    val highest = (position * rowHeightPx).toInt().coerceAtLeast(0)
    // Lowest it may sit and still have the rest of the queue fill the viewport
    // below it.
    val rowsBelow = ((itemCount - position) * rowHeightPx).toInt()
    val lowest = (viewportPx - rowsBelow).coerceIn(0, highest)
    return centered.coerceIn(lowest, highest)
}

/**
 * Matches the library locate motion — stage long jumps, then glide the final
 * tail — but settles the target row in the middle of the viewport rather than
 * at its top edge, so the drawer shows the current song with the songs already
 * played above it and what is coming below. The scroller clamps itself at the
 * list's own ends, so near the top or bottom the glide simply stops flush.
 */
/**
 * The reduced-motion counterpart of [animateQueueScroll]: the same destination,
 * reached without travelling. A RecyclerView smooth scroll is a view-system
 * animation, so unlike the Compose motion in this drawer it does not follow the
 * platform's animation scale — without this branch the queue would keep sweeping
 * across long lists even with reduced motion requested.
 */
private fun placeQueueInstantly(
    recyclerView: RecyclerView,
    targetPosition: Int,
    rowHeightPx: Float
) {
    val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return
    val viewportPx = recyclerView.height - recyclerView.paddingTop - recyclerView.paddingBottom
    val offset = queueAnchorOffset(
        viewportPx = viewportPx,
        rowHeightPx = rowHeightPx,
        itemCount = recyclerView.adapter?.itemCount ?: 0,
        position = targetPosition
    )
    layout.scrollToPositionWithOffset(targetPosition, offset)
}

private fun animateQueueScroll(
    recyclerView: RecyclerView,
    targetPosition: Int,
    rowHeightPx: Float
) {
    val layout = recyclerView.layoutManager as? LinearLayoutManager ?: return
    val first = layout.findFirstVisibleItemPosition()
    fun glideToTarget() {
        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            private fun itemCountOrZero(): Int =
                recyclerView.adapter?.itemCount ?: 0

            override fun getVerticalSnapPreference(): Int = SNAP_TO_START

            // Where the target row's top should end up, in viewport
            // coordinates. Everything here is a delta *to* that position:
            // SmoothScroller negates whatever this returns to get its scroll
            // direction, so an inverted sign would walk the row away from the
            // offset instead of onto it — which is what sent Locate past the
            // end of the queue and Queue to a random row mid-list.
            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                val offset = queueAnchorOffset(
                    viewportPx = boxEnd - boxStart,
                    rowHeightPx = rowHeightPx,
                    itemCount = itemCountOrZero(),
                    position = targetPosition
                )
                return (boxStart + offset) - viewStart
            }
        }
        scroller.targetPosition = targetPosition
        layout.startSmoothScroll(scroller)
    }
    if (first < 0 || abs(targetPosition - first) <= 10) {
        glideToTarget()
        return
    }
    val staged = (targetPosition + if (targetPosition > first) -6 else 6)
        .coerceIn(0, (recyclerView.adapter?.itemCount ?: 1) - 1)
    layout.scrollToPositionWithOffset(staged, 0)
    recyclerView.post { glideToTarget() }
}

private class QueueRecyclerView(
    context: Context,
    private val onTopCloseDrag: (Float) -> Unit,
    private val onTopCloseDragEnd: (Float) -> Unit
) : RecyclerView(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var interceptingTopClose = false
    private var lastY = 0f
    private var totalDownDistance = 0f

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                interceptingTopClose = false
                lastY = event.y
                totalDownDistance = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - lastY
                if (!interceptingTopClose &&
                    deltaY > touchSlop &&
                    !canScrollVertically(-1)
                ) {
                    interceptingTopClose = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interceptingTopClose) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val deltaY = (event.y - lastY).coerceAtLeast(0f)
                if (deltaY > 0f) {
                    totalDownDistance += deltaY
                    onTopCloseDrag(deltaY)
                }
                lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onTopCloseDragEnd(totalDownDistance)
                interceptingTopClose = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }
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
    private var pendingSnapshot: QueueSnapshot? = null
    private var snapshotPostPending = false
    private var attachedRecyclerView: RecyclerView? = null
    private var dragStartEntries: List<QueueEntry> = emptyList()
    private var releaseSubmitted = false
    var artColors: ArtColorRoles? = null
    var onEntryClick: (Long) -> Unit = {}
    var onReorderEntry: (Long, Int) -> Unit = { _, _ -> }
    var onRemoveEntry: (Long) -> Unit = {}
    var startDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    init {
        setHasStableIds(true)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        if (attachedRecyclerView === recyclerView) {
            attachedRecyclerView = null
            pendingSnapshot = null
            snapshotPostPending = false
        }
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun submitSnapshot(snapshot: QueueSnapshot) {
        if (isDragging) {
            deferredSnapshot = snapshot
            return
        }
        pendingSnapshot = snapshot
        val recyclerView = attachedRecyclerView
        if (recyclerView != null) {
            if (!snapshotPostPending) {
                snapshotPostPending = true
                recyclerView.post {
                    snapshotPostPending = false
                    val nextSnapshot = pendingSnapshot ?: return@post
                    pendingSnapshot = null
                    if (isDragging) {
                        deferredSnapshot = nextSnapshot
                    } else {
                        applySnapshot(nextSnapshot)
                    }
                }
            }
            return
        }
        pendingSnapshot = null
        applySnapshot(snapshot)
    }

    private fun applySnapshot(snapshot: QueueSnapshot) {
        val displayEntries = snapshot.visibleEntries
        val oldEntries = entries
        val oldIds = oldEntries.map { it.entryId }
        val newIds = displayEntries.map { it.entryId }
        val orderChanged = oldIds != newIds
        val stateChanged = currentEntryId != snapshot.currentEntryId ||
            currentPosition != snapshot.resolvedVisiblePosition
        entries = displayEntries
        currentEntryId = snapshot.currentEntryId
        currentPosition = snapshot.resolvedVisiblePosition
        if (orderChanged) {
            // DiffUtil emits a real remove operation, allowing RecyclerView's
            // animator to fade the dismissed card instead of snapping the whole
            // list with notifyDataSetChanged(). It also preserves stable row
            // identity during the single-move reorder flow.
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldEntries.size
                override fun getNewListSize(): Int = displayEntries.size
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldEntries[oldItemPosition].entryId == displayEntries[newItemPosition].entryId
                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                    oldEntries[oldItemPosition] == displayEntries[newItemPosition]
            }).dispatchUpdatesTo(this)
        } else if (stateChanged && entries.isNotEmpty()) {
            notifyItemRangeChanged(0, entries.size, PAYLOAD_STATE)
        }
    }

    override fun getItemId(position: Int): Long = entries[position].entryId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(context, artColors)

    private fun bindHolder(holder: Holder, position: Int, loadArtwork: Boolean) {
        val entry = entries[position]
        holder.bind(
            entry = entry,
            isCurrent = entry.entryId == currentEntryId ||
                (currentEntryId == null && position == currentPosition),
            isHistory = currentPosition >= 0 && position < currentPosition,
            colors = artColors,
            loadArtwork = loadArtwork,
            onClick = { onEntryClick(entry.entryId) },
            onRemove = { onRemoveEntry(entry.entryId) },
            onStartDrag = { startDrag?.invoke(holder) }
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) =
        bindHolder(holder, position, loadArtwork = true)

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) =
        bindHolder(holder, position, loadArtwork = payloads.isEmpty())

    override fun getItemCount(): Int = entries.size

    fun beginDrag(holder: RecyclerView.ViewHolder) {
        isDragging = true
        draggedEntryId = holder.bindingAdapterPosition
            .takeIf { it != RecyclerView.NO_POSITION }
            ?.let { position -> entries.getOrNull(position)?.entryId }
        deferredSnapshot = null
        pendingSnapshot = null
        dragStartEntries = entries.toList()
        releaseSubmitted = false
    }

    fun endDrag() {
        isDragging = false
        draggedEntryId = null
        // The deferred snapshots describe the queue before the released order
        // is committed. Drop them; the controller publishes one authoritative
        // snapshot after the deferred reorder completes.
        deferredSnapshot = null
        pendingSnapshot = null
        dragStartEntries = emptyList()
    }

    private fun moveLocal(from: Int, to: Int) {
        if (from !in entries.indices || to !in entries.indices || from == to) return
        val moved = entries.toMutableList().apply { add(to, removeAt(from)) }
        entries = moved
        notifyItemMoved(from, to)
    }

    inner class MoveCallback : ItemTouchHelper.Callback() {
        private var activeRecyclerView: RecyclerView? = null
        private var activeViewHolder: RecyclerView.ViewHolder? = null
        private var draggedTopPx = 0f
        private var draggedBottomPx = 0f
        private var autoScrollRunning = false
        private val autoScrollRunnable = object : Runnable {
            override fun run() {
                val recyclerView = activeRecyclerView
                val holder = activeViewHolder
                if (!isDragging || recyclerView == null || holder == null || !recyclerView.isAttachedToWindow) {
                    autoScrollRunning = false
                    return
                }
                val layout = recyclerView.layoutManager as? LinearLayoutManager
                val firstVisible = layout?.findViewByPosition(layout.findFirstVisibleItemPosition())
                val lastVisible = layout?.findViewByPosition(layout.findLastVisibleItemPosition())
                // The trigger is a reachable card-height band. Upward scrolling
                // is eligible across the first visible row; downward scrolling
                // is eligible across the last visible row. The second row is
                // therefore outside the upper band instead of auto-scrolling on
                // long-press alone.
                val upBandTop = (firstVisible?.top ?: recyclerView.paddingTop).toFloat()
                val upBandBottom = (firstVisible?.bottom ?: recyclerView.paddingTop).toFloat()
                // Give downward dragging the requested lower 20% of the
                // viewport while leaving the upper portion free for precise
                // reordering.
                val lowerTriggerHeight = (recyclerView.height * 0.20f).roundToInt()
                val downBandTop = (recyclerView.height - lowerTriggerHeight).toFloat()
                val downBandBottom = recyclerView.height.toFloat()
                val top = if (draggedBottomPx > draggedTopPx) draggedTopPx else holder.itemView.top.toFloat()
                val bottom = if (draggedBottomPx > draggedTopPx) draggedBottomPx else holder.itemView.bottom.toFloat()
                val overlapsFirstBand = bottom > upBandTop && top < upBandBottom
                val overlapsLastBand = bottom > downBandTop && top < downBandBottom
                val delta = when {
                    overlapsFirstBand -> -context.dp(14)
                    overlapsLastBand -> context.dp(14)
                    else -> 0
                }
                if (delta != 0 && recyclerView.canScrollVertically(if (delta > 0) 1 else -1)) {
                    recyclerView.scrollBy(0, delta)
                }
                recyclerView.postOnAnimation(this)
            }
        }

        private fun startAutoScroll(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            activeRecyclerView = recyclerView
            activeViewHolder = viewHolder
            draggedTopPx = viewHolder.itemView.top.toFloat()
            draggedBottomPx = viewHolder.itemView.bottom.toFloat()
            if (!autoScrollRunning) {
                autoScrollRunning = true
                recyclerView.postOnAnimation(autoScrollRunnable)
            }
        }

        private fun stopAutoScroll() {
            activeRecyclerView?.removeCallbacks(autoScrollRunnable)
            activeRecyclerView = null
            activeViewHolder = null
            draggedTopPx = 0f
            draggedBottomPx = 0f
            autoScrollRunning = false
        }

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
                animateDragLift(viewHolder.itemView, lifted = true)
                beginDrag(viewHolder)
                attachedRecyclerView?.let { startAutoScroll(it, viewHolder) }
            } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                activeViewHolder?.itemView?.let { animateDragLift(it, lifted = false) }
                stopAutoScroll()
            }
        }

        private fun animateDragLift(view: View, lifted: Boolean) {
            view.animate().cancel()
            view.animate()
                .scaleX(if (lifted) 1.015f else 1f)
                .scaleY(if (lifted) 1.015f else 1f)
                .setDuration(MiniMusicMotion.selectionDurationMillis.toLong())
                .setInterpolator(FastOutSlowInEasingInterpolator)
                .start()
            view.elevation = if (lifted) context.dp(4).toFloat() else 0f
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            animateDragLift(viewHolder.itemView, lifted = false)
            stopAutoScroll()
            super.clearView(recyclerView, viewHolder)
            if (!releaseSubmitted) {
                releaseSubmitted = true
                val movedEntryId = draggedEntryId
                val fromIndex = movedEntryId?.let { id ->
                    dragStartEntries.indexOfFirst { it.entryId == id }
                } ?: -1
                val toIndex = movedEntryId?.let { id ->
                    entries.indexOfFirst { it.entryId == id }
                } ?: -1
                if (movedEntryId != null && fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex) {
                    // Keep clearView visual-only; the one logical move is posted
                    // after ItemTouchHelper has finished its release callback.
                    viewHolder.itemView.post { onReorderEntry(movedEntryId, toIndex) }
                }
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

        override fun onChildDraw(
            canvas: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder === activeViewHolder) {
                draggedTopPx = viewHolder.itemView.top + dY
                draggedBottomPx = viewHolder.itemView.bottom + dY
            }
            super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun getBoundingBoxMargin(): Int = context.dp(48)

        // The continuous runnable above owns edge scrolling. Returning zero here
        // prevents ItemTouchHelper from running a second competing scroll loop.
        override fun interpolateOutOfBoundsScroll(
            recyclerView: RecyclerView,
            viewSize: Int,
            viewSizeOutOfBounds: Int,
            totalSize: Int,
            msSinceStartScroll: Long
        ): Int = 0

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
        private val imageLoader = context.imageLoader

        init {
            handle.layoutParams = LinearLayout.LayoutParams(context.dp(32), ViewGroup.LayoutParams.MATCH_PARENT)
            root.addView(handle)

            artwork.layoutParams = LinearLayout.LayoutParams(context.dp(44), context.dp(44)).apply {
                marginEnd = context.dp(8)
            }
            artwork.scaleType = ImageView.ScaleType.CENTER_CROP
            artwork.background = GradientDrawable().apply {
                setColor(android.graphics.Color.TRANSPARENT)
                cornerRadius = context.dp(10).toFloat()
            }
            artwork.clipToOutline = true
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
            loadArtwork: Boolean,
            onClick: () -> Unit,
            onRemove: () -> Unit,
            onStartDrag: () -> Unit
        ) {
            val resolved = colors ?: return
            // Keep every state fully opaque, and keep each state inside one
            // role family so the scheme guarantees its contrast. The current
            // song uses the primary container — the same family the library
            // marks its playing row with — ordinary rows sit on the surface
            // role, and history recedes toward a container tone without alpha.
            val rowArgb = if (isCurrent) {
                resolved.primaryContainer.toArgb()
            } else {
                resolved.surface.toArgb()
            }
            val opaqueHistoryColor = if (isHistory) {
                ColorUtils.blendARGB(
                    resolved.surface.toArgb(),
                    resolved.surfaceContainerHigh.toArgb(),
                    0.55f
                )
            } else {
                rowArgb
            }
            val card = GradientDrawable().apply {
                setColor(opaqueHistoryColor)
                cornerRadius = root.context.dp(16).toFloat()
            }
            root.background = InsetDrawable(
                card,
                root.context.dp(8),
                root.context.dp(4),
                root.context.dp(8),
                root.context.dp(4)
            )
            // The current row is identified by its restrained opaque highlight;
            // history is conveyed only by subdued opaque colors, never alpha.
            root.alpha = 1f
            title.text = entry.song.title
            artist.text = entry.song.artist
            title.setTypeface(ResourcesCompat.getFont(root.context, R.font.google_sans_flex_medium))
            artist.setTypeface(ResourcesCompat.getFont(root.context, R.font.google_sans_flex_regular))
            val titleArgb = if (isHistory) {
                ColorUtils.blendARGB(resolved.onSurface.toArgb(), opaqueHistoryColor, 0.28f)
            } else if (isCurrent) {
                resolved.onPrimaryContainer.toArgb()
            } else {
                resolved.onSurface.toArgb()
            }
            val artistArgb = if (isHistory) {
                ColorUtils.blendARGB(resolved.onSurfaceVariant.toArgb(), opaqueHistoryColor, 0.36f)
            } else if (isCurrent) {
                ColorUtils.blendARGB(
                    resolved.onPrimaryContainer.toArgb(),
                    resolved.onSurfaceVariant.toArgb(),
                    0.18f
                )
            } else {
                resolved.onSurfaceVariant.toArgb()
            }
            // Art tiles stay in their row's family: the current song on the
            // primary container, every other row on a neutral container.
            (artwork.background as? GradientDrawable)?.setColor(
                if (isCurrent) resolved.primaryContainer.toArgb() else resolved.surfaceContainerHigh.toArgb()
            )
            title.setTextColor(titleArgb)
            artist.setTextColor(artistArgb)
            title.textSize = 16f
            artist.textSize = 14f
            if (loadArtwork) {
                artwork.setImageResource(android.R.drawable.ic_menu_gallery)
                entry.song.albumArtUri?.let { uri ->
                    imageLoader.enqueue(
                        ImageRequest.Builder(root.context)
                            .data(uri)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .crossfade(false)
                            .target(artwork)
                            .build()
                    )
                }
            }
            handle.dotColor = artistArgb
            handle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag()
                false
            }
            close.setColorFilter(artistArgb)
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

private val FastOutSlowInEasingInterpolator = android.view.animation.DecelerateInterpolator()

private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
