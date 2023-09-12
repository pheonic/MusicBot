package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import org.junit.Rule
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class TrackSchedulerTest {

    @Test
    fun remove() {
        val player = mockk<AudioPlayer>(relaxed = true)
        val scheduler = TrackScheduler(player)
        (1..10).forEach { i -> scheduler.queue(audioTrack(i)) }
        scheduler.remove(listOf(1, 2, 3))
        assertEquals(listOf(4, 5, 6, 7, 8, 9, 10), trackNameList(scheduler))
        scheduler.remove(listOf(1, 10))
        assertEquals(listOf(5, 6, 7, 8, 9, 10), trackNameList(scheduler))
        scheduler.remove(listOf(2, 6))
        assertEquals(listOf(5, 7, 8, 9), trackNameList(scheduler))
        scheduler.remove(listOf(3))
        assertEquals(listOf(5, 7, 9), trackNameList(scheduler))
        scheduler.remove(listOf(0))
        assertEquals(listOf(5, 7, 9), trackNameList(scheduler))
        scheduler.remove(listOf(10))
        assertEquals(listOf(5, 7, 9), trackNameList(scheduler))
        scheduler.remove(listOf(-1))
        assertEquals(listOf(5, 7, 9), trackNameList(scheduler))
        scheduler.remove(listOf(-1, 0, 5))
        assertEquals(listOf(5, 7, 9), trackNameList(scheduler))
    }

    private fun trackNameList(scheduler: TrackScheduler): List<Int> {
        val trackNames = mutableListOf<Int>()
        scheduler.iterator().forEach { t -> trackNames.add(t.info.title.toInt()) }
        return trackNames
    }

    private fun audioTrack(i: Int): AudioTrack {
        val track = mockk<AudioTrack>(relaxed = true)
        val info = AudioTrackInfoBuilder.empty()
            .setTitle("$i")
            .build()
        every { track.info } returns info
        return track
    }
}