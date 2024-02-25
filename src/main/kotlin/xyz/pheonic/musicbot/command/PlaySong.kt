package xyz.pheonic.musicbot.command

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

open class PlaySong : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "play"

    override fun help(): String {
        return "play url - Adds this url to the queue."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got playSong ${event.debugString()}" }
        if (!event.guild.audioManager.isConnected) {
            Summon().execute(event, musicManager)
        }
        val trackUrl = event.message.contentDisplay.substringAfter(' ').substringBefore(' ')
        musicManager.addItemToQueue(
            trackUrl,
            CustomAudioLoadResultHandler(event, musicManager, trackUrl)
        )
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
                musicManager.scheduler.queue(it)
            }
        }

        override fun noMatches() {
            sendMessage(logger, event.channel, codeBlock("Nothing found at $trackUrl"))
        }

        override fun playlistLoaded(playlist: AudioPlaylist?) {
            if (playlist?.selectedTrack != null) {
                sendMessage(logger, event.channel, codeBlock("Adding ${playlist.selectedTrack.info?.title} to queue"))
                musicManager.scheduler.queue(playlist.selectedTrack)
            } else {
                val tracks = playlist?.tracks ?: throw Exception("Playlist without tracks")
                sendMessage(logger, event.channel, codeBlock("Adding ${tracks.size} tracks to the queue"))
                for (track in tracks) {
                    musicManager.scheduler.queue(track)
                }
            }
        }
    }
}
