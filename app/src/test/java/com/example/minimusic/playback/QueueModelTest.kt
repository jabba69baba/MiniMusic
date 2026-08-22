package com.example.minimusic.playback

import android.net.Uri
import com.example.minimusic.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QueueModelTest {
    private val songs = listOf(
        song(1L, "Alpha"),
        song(2L, "Bravo"),
        song(3L, "Charlie"),
        song(4L, "Delta")
    )

    @Test
    fun `entry identity is distinct even for duplicate songs`() {
        val duplicate = songs.first()
        val snapshot = QueueSnapshot(
            entries = listOf(QueueEntry(10L, duplicate), QueueEntry(11L, duplicate)),
            currentPosition = 0
        )
        assertEquals(listOf(10L, 11L), snapshot.entries.map { it.entryId })
    }

    @Test
    fun `repeated downward movement preserves current entry`() {
        val entries = songs.mapIndexed { index, song -> QueueEntry(index.toLong(), song) }
        var snapshot = QueueSnapshot(entries, currentPosition = 1, currentEntryId = 1L)
        snapshot = snapshot.moveEntry(0L, 1)
        snapshot = snapshot.moveEntry(0L, 2)
        snapshot = snapshot.moveEntry(0L, 3)

        assertEquals(listOf(1L, 2L, 3L, 0L), snapshot.entries.map { it.entryId })
        assertEquals(0, snapshot.resolvedCurrentPosition)
        assertEquals(1L, snapshot.currentEntry?.entryId)
    }

    @Test
    fun `repeated upward movement preserves current entry`() {
        val entries = songs.mapIndexed { index, song -> QueueEntry(index.toLong(), song) }
        var snapshot = QueueSnapshot(entries, currentPosition = 2, currentEntryId = 2L)
        snapshot = snapshot.moveEntry(3L, 2)
        snapshot = snapshot.moveEntry(3L, 1)
        snapshot = snapshot.moveEntry(3L, 0)

        assertEquals(listOf(3L, 0L, 1L, 2L), snapshot.entries.map { it.entryId })
        assertEquals(3, snapshot.resolvedCurrentPosition)
        assertEquals(2L, snapshot.currentEntry?.entryId)
    }

    @Test
    fun `removing current selects next entry`() {
        val entries = songs.mapIndexed { index, song -> QueueEntry(index.toLong(), song) }
        val snapshot = QueueSnapshot(entries, currentPosition = 1, currentEntryId = 1L)
        val remaining = snapshot.removeEntry(1L)

        assertEquals(listOf(0L, 2L, 3L), remaining.entries.map { it.entryId })
        assertEquals(1L, remaining.currentEntryId)
        assertEquals(2L, remaining.currentEntry?.entryId)
    }

    @Test
    fun `visible positions follow actual upcoming order`() {
        val entries = songs.mapIndexed { index, song -> QueueEntry(index.toLong(), song) }
        val visible = listOf(entries[2], entries[0], entries[3], entries[1])
        val snapshot = QueueSnapshot(
            entries = entries,
            currentPosition = 2,
            currentEntryId = 2L,
            visibleEntries = visible
        )

        assertEquals(listOf(2L, 0L, 3L, 1L), snapshot.visibleEntries.map { it.entryId })
        assertEquals(0, snapshot.resolvedVisiblePosition)
        assertEquals(3, snapshot.visiblePositionOf(1L))
    }

    @Test
    fun `history is the complete prefix when current is selected deep in queue`() {
        val entries = songs.mapIndexed { index, song -> QueueEntry(index.toLong(), song) }
        val snapshot = QueueSnapshot(
            entries = entries,
            currentPosition = 4,
            currentEntryId = 4L,
            historyEntries = entries.take(4),
            visibleEntries = entries
        )

        assertEquals(listOf(0L, 1L, 2L, 3L), snapshot.historyEntries.map { it.entryId })
        assertEquals(listOf(0L, 1L, 2L, 3L, 4L), snapshot.visibleEntries.map { it.entryId })
        assertEquals(4, snapshot.resolvedVisiblePosition)
    }

    @Test
    fun `moving an entry across current keeps every entry and recomputes history`() {
        val entries = songs.mapIndexed { index, song -> QueueEntry(index.toLong(), song) }
        val snapshot = QueueSnapshot(
            entries = entries,
            currentPosition = 2,
            currentEntryId = 2L,
            historyEntries = entries.take(2),
            visibleEntries = entries
        )

        val moved = snapshot.moveEntry(0L, 4)

        assertEquals(listOf(1L, 2L, 3L, 4L, 0L), moved.entries.map { it.entryId })
        assertEquals(1, moved.resolvedCurrentPosition)
        assertEquals(listOf(1L), moved.historyEntries.map { it.entryId })
        assertEquals(moved.entries, moved.visibleEntries)
    }

    @Test
    fun `history is separate from visible playback order`() {
        val entries = songs.mapIndexed { index, song -> QueueEntry(index.toLong(), song) }
        val snapshot = QueueSnapshot(
            entries = entries,
            currentPosition = 2,
            currentEntryId = 2L,
            historyEntries = listOf(entries[0], entries[1]),
            visibleEntries = listOf(entries[2], entries[3])
        )

        assertEquals(listOf(0L, 1L), snapshot.historyEntries.map { it.entryId })
        assertEquals(listOf(2L, 3L), snapshot.visibleEntries.map { it.entryId })
        assertEquals(-1, snapshot.visiblePositionOf(0L))
        assertEquals(0, snapshot.visiblePositionOf(2L))
    }

    @Test
    fun `invalid entry changes are no-ops`() {
        val entries = songs.mapIndexed { index, song -> QueueEntry(index.toLong(), song) }
        val snapshot = QueueSnapshot(entries, currentPosition = 1)
        assertEquals(snapshot, snapshot.moveEntry(99L, 0))
        assertEquals(snapshot, snapshot.removeEntry(99L))
        assertNull(snapshot.entry(99L))
    }

    private companion object {
        fun song(id: Long, title: String) = Song(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
            albumId = 1L,
            durationMs = 180_000L,
            trackNumber = id.toInt(),
            contentUri = Uri.EMPTY,
            albumArtUri = null,
            dateAddedSeconds = id
        )
    }
}
