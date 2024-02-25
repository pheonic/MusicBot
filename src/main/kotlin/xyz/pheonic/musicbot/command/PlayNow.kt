package xyz.pheonic.musicbot.command

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class PlayNow : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "playnow"

    override fun help(): String {
        return "playnow url - Immediately plays a song, bumping the currently playing song down into the queue"
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got playNow ${event.debugString()}" }
        val currentTrack = musicManager.nowPlaying()?.makeClone()
        val trackUrl = event.message.contentDisplay.substringAfter(' ').substringBefore(' ')
        musicManager.addItemToQueue(
            trackUrl,
            CustomAudioLoadResultHandler(event, musicManager, trackUrl)
        )
        currentTrack?.let { musicManager.scheduler.push(it.makeClone()) }
    }

    inner class CustomAudioLoadResultHandler(
        private val event: GuildMessageReceivedEvent,
        private val musicManager: GuildMusicManager,
        private val trackUrl: String
    ) : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException?) {
            sendMessage(logger, event.channel, codeBlock("Could not play: ${exception?.message}"))
        }

        override fun trackLoaded(track: AudioTrack?) {
            track?.let {
                sendMessage(logger, event.channel, codeBlock("Adding ${it.info.title} to queue"))
                if (musicManager.nowPlaying() == null) {
                    musicManager.scheduler.push(it)
                } else {
                    musicManager.scheduler.push(it)
                    musicManager.scheduler.skip()
                }
            }
        }

        override fun noMatches() {
            sendMessage(logger, event.channel, codeBlock("Nothing found at $trackUrl"))
        }

        override fun playlistLoaded(playlist: AudioPlaylist?) {
            sendMessage(logger, event.channel, codeBlock("Playlists not supported for playnow command"))
        }
    }
}
