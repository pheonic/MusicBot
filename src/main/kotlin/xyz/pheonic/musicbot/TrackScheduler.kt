package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(private val player: AudioPlayer) : AudioEventAdapter() {
    private val queue: BlockingQueue<AudioTrack> = LinkedBlockingQueue()
    var repeatMode: RepeatMode = RepeatMode.OFF

    fun queue(track: AudioTrack) {
        if (!player.startTrack(track, true)) {
            queue.offer(track)
        }
    }

    private fun next() {
        player.startTrack(queue.poll(), false)
    }

    fun skip() {
        if (repeatMode == RepeatMode.ONE) {
            val track = player.playingTrack?.makeClone()
            player.stopTrack()
            player.startTrack(track, false)
        } else {
            next()
        }
    }

    fun clearAll() {
        queue.clear()
        player.stopTrack()
    }

    fun iterator(): Iterator<AudioTrack> {
        return queue.iterator()
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        // Stopped means the player should completely stop even if there are other songs in the queue
        if (endReason == AudioTrackEndReason.STOPPED) {
            return
        }
        when (repeatMode) {
            RepeatMode.OFF -> if (endReason?.mayStartNext == true) {
                next()
            }
            RepeatMode.ONE -> this.player.startTrack(track?.makeClone(), true)
            RepeatMode.ALL -> {
                if (endReason?.mayStartNext == true) {
                    next()
                }
                track?.makeClone()?.let { queue(it) }
            }
        }
    }

    fun clear() {
        queue.clear()
    }

    fun shuffle() {
        //TODO this can probably be improved/may not be optimal
        val list = queue.toMutableList()
        queue.clear()
        list.shuffle()
        queue.addAll(list)
    }

    fun size(): Int {
        return queue.size
    }

    fun remove(i: Int): AudioTrack? {
        val list = queue.toMutableList()
        val removed = list.removeAt(i - 1)
        queue.clear()
        queue.addAll(list)
        return removed
    }
}
