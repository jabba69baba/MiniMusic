package com.example.minimusic.playback

import com.example.minimusic.data.model.Song

/** A single occurrence of a song in the active queue. */
data class QueueEntry(
    val entryId: Long,
    val song: Song
)

/**
 * Authoritative user-visible queue order. The active item is identified by entryId,
 * never by a position that can change during reorder.
 */
data class QueueSnapshot(
    /** Canonical Media3 timeline order used for mutations. */
    val entries: List<QueueEntry> = emptyList(),
    val currentPosition: Int = -1,
    val currentEntryId: Long? = null,
    /** Entries that have already played in oldest-to-newest order. */
    val historyEntries: List<QueueEntry> = emptyList(),
    /** Playback order shown by the queue UI: current item followed by actual upcoming items. */
    val visibleEntries: List<QueueEntry> = entries
) {
    init {
        require(currentPosition in -1 until entries.size)
        require(entries.map { it.entryId }.distinct().size == entries.size)
        require(currentEntryId == null || entries.any { it.entryId == currentEntryId })
        require(historyEntries.map { it.entryId }.distinct().size == historyEntries.size)
        require(historyEntries.all { history -> entries.any { it.entryId == history.entryId } })
        require(visibleEntries.map { it.entryId }.distinct().size == visibleEntries.size)
        require(visibleEntries.all { visible -> entries.any { it.entryId == visible.entryId } })
        require(historyEntries.none { history -> visibleEntries.any { it.entryId == history.entryId } })
    }

    val resolvedCurrentPosition: Int
        get() = currentEntryId?.let(::positionOf)?.takeIf { it >= 0 } ?: currentPosition

    val currentEntry: QueueEntry?
        get() = currentEntryId?.let(::entry) ?: entries.getOrNull(currentPosition)

    val resolvedVisiblePosition: Int
        get() = visibleEntries.indexOfFirst { it.entryId == currentEntryId }

    fun positionOf(entryId: Long): Int = entries.indexOfFirst { it.entryId == entryId }

    fun visiblePositionOf(entryId: Long): Int = visibleEntries.indexOfFirst { it.entryId == entryId }

    fun entry(entryId: Long): QueueEntry? = entries.firstOrNull { it.entryId == entryId }

    fun moveEntry(entryId: Long, destinationPosition: Int): QueueSnapshot {
        val sourcePosition = positionOf(entryId)
        if (sourcePosition < 0 || entries.isEmpty()) return this
        val destination = destinationPosition.coerceIn(entries.indices)
        if (sourcePosition == destination) return this

        val movedEntries = entries.toMutableList().apply {
            add(destination, removeAt(sourcePosition))
        }
        val movedPosition = when {
            currentPosition < 0 -> -1
            currentPosition == sourcePosition -> destination
            sourcePosition < currentPosition && destination >= currentPosition -> currentPosition - 1
            sourcePosition > currentPosition && destination <= currentPosition -> currentPosition + 1
            else -> currentPosition
        }
        val movedVisibleEntries = if (visibleEntries.map { it.entryId } == entries.map { it.entryId }) {
            movedEntries
        } else {
            visibleEntries
        }
        return copy(
            entries = movedEntries,
            currentPosition = movedPosition,
            visibleEntries = movedVisibleEntries
        )
    }

    fun removeEntry(entryId: Long): QueueSnapshot {
        val removedPosition = positionOf(entryId)
        if (removedPosition < 0) return this
        val remaining = entries.toMutableList().apply { removeAt(removedPosition) }
        if (remaining.isEmpty()) {
            return copy(
                entries = emptyList(),
                currentPosition = -1,
                currentEntryId = null,
                visibleEntries = emptyList()
            )
        }

        val nextPosition = when {
            currentPosition < 0 -> -1
            removedPosition < currentPosition -> currentPosition - 1
            removedPosition == currentPosition -> currentPosition.coerceAtMost(remaining.lastIndex)
            else -> currentPosition
        }
        val nextCurrentId = when {
            currentEntryId != null && currentEntryId != entryId -> currentEntryId
            nextPosition >= 0 -> remaining[nextPosition].entryId
            else -> null
        }
        val remainingVisibleEntries = visibleEntries.filterNot { it.entryId == entryId }
        return copy(
            entries = remaining,
            currentPosition = nextPosition,
            currentEntryId = nextCurrentId,
            visibleEntries = remainingVisibleEntries
        )
    }
}
