package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

class GuildMusicManager(manager: AudioPlayerManager, config: Config) {
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
    var repeatMode: RepeatMode
        get() = scheduler.repeatMode
        set(value) {
            scheduler.repeatMode = value
        }

    init {
        player.addListener(scheduler)
        volume = config.startVolume
    }

    fun nowPlaying(): AudioTrack? = player.playingTrack ?: null

    fun getSendHandler(): AudioPlayerSendHandler {
        return AudioPlayerSendHandler(player)
    }
}
