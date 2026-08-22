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
