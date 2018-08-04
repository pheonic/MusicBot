package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import sx.blah.discord.handle.obj.IGuild

class GuildMusicManager(manager: AudioPlayerManager, guild: IGuild, musicBot: MusicBot, config: Config) {
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
        player.addListener(GuildEventNotifier(guild, musicBot))
        volume = config.startVolume
    }

    fun nowPlaying(): AudioTrack? = player.playingTrack ?: null
    fun audioProvider() = AudioProvider(player)
    inner class GuildEventNotifier(private val guild: IGuild, private val musicBot: MusicBot) : AudioEventAdapter() {
        override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
            musicBot.sendNowPlayingMessage(guild, track)
        }
    }
}
