package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

class GuildMusicManager(manager: AudioPlayerManager) {
    private val player: AudioPlayer = manager.createPlayer()
    val scheduler: TrackScheduler = TrackScheduler(player)
    var isPaused: Boolean
        get() = player.isPaused
        set(value) {
            player.isPaused = value
        }
    var volume: Int
        get() = player.volume
        set(value) {
            player.volume = value
        }

    init {
        player.addListener(scheduler)
    }

    fun nowPlaying(): AudioTrack = player.playingTrack

    fun audioProvider() = AudioProvider(player)
}
