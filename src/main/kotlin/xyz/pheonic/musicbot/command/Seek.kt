package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class Seek : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "seek"

    override fun help(): String {
        return "seek amountOfTime - goes forward or backwards by the amount of time provided in seconds, negative numbers go back"
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got seek ${event.debugString()}")
        val seekAmount = event.message.contentDisplay.substringAfter(' ').toIntOrNull() ?: 0
        val currentTrack = musicManager.nowPlaying()
        currentTrack?.let {
            it.position = (it.position + seekAmount * 1000).coerceIn(0, it.duration)
            sendMessage(
                logger,
                event.channel,
                codeBlock("Track set to ${millisToTime(currentTrack.position)} / ${millisToTime(currentTrack.duration)}")
            )
        }
    }
}
