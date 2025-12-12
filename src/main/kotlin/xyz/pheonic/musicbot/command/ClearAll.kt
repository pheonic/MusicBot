package xyz.pheonic.musicbot.command

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class ClearAll : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "clear-all"

    override fun help(): String {
        return "clear-all - Clears the queue and the currently playing song. (Bypasses repeat)"
    }

    override fun execute(event: MessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got clear-all ${event.debugString()}" }
        musicManager.scheduler.clearAll()
        sendMessage(logger, event.channel, codeBlock("Cleared all"))
    }
}
