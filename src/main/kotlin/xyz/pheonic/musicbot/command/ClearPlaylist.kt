package xyz.pheonic.musicbot.command

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class ClearPlaylist : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "clear"

    override fun help(): String {
        return "clear - Clears the queue, doesn't clear the currently playing song."
    }

    override fun execute(event: MessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got clearPlaylist ${event.debugString()}" }
        musicManager.scheduler.clear()
        sendMessage(logger, event.channel, codeBlock("Cleared queue"))
    }
}
