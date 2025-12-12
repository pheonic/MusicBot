package xyz.pheonic.musicbot.command

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class PlayNext : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "playnext"

    override fun help(): String {
        return "playnext url - Adds a song to the top of the queue"
    }

    override fun execute(event: MessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got playNow ${event.debugString()}" }
        val trackUrl = event.message.contentDisplay.substringAfter(' ').substringBefore(' ')
        musicManager.addItemToQueue(
            trackUrl,
            CustomAudioLoadResultHandler(event, musicManager, trackUrl)
        )
    }

    inner class CustomAudioLoadResultHandler(
        private val event: MessageReceivedEvent,
        private val musicManager: GuildMusicManager,
        private val trackUrl: String
    ) : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException?) {
            sendMessage(logger, event.channel, codeBlock("Could not play: ${exception?.message}"))
        }

        override fun trackLoaded(track: AudioTrack?) {
            track?.let {
                sendMessage(logger, event.channel, codeBlock("Adding ${it.info.title} to queue"))
                musicManager.scheduler.push(it)
            }
        }

        override fun noMatches() {
            sendMessage(logger, event.channel, codeBlock("Nothing found at $trackUrl"))
        }

        override fun playlistLoaded(playlist: AudioPlaylist?) {
            sendMessage(logger, event.channel, codeBlock("Playlists not supported for playnext command"))
        }
    }
}
