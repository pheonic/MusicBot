package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import xyz.pheonic.musicbot.command.PlaySong

class GuildMusicManager(private val manager: AudioPlayerManager, guild: Guild, musicBot: MusicBot, config: Config) {
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
        player.addListener(GuildEventNotifier(guild, musicBot))
        volume = config.startVolume
    }

    fun nowPlaying(): AudioTrack? = player.playingTrack

    fun getSendHandler(): AudioPlayerSendHandler {
        return AudioPlayerSendHandler(player)
    }

    fun addItemToQueue(
        trackUrl: String,
        customAudioLoadResultHandler: AudioLoadResultHandler
    ) {
        manager.loadItemOrdered(
            this,
            trackUrl,
            customAudioLoadResultHandler
        )
    }

    inner class GuildEventNotifier(private val guild: Guild, private val musicBot: MusicBot) : AudioEventAdapter() {
        override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
            musicBot.sendNowPlayingMessage(guild, track)
        }

        override fun onTrackException(player: AudioPlayer?, track: AudioTrack?, exception: FriendlyException?) {
            musicBot.sendTrackExceptionMessage(guild, track, exception)
        }
    }

}
