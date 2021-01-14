package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class Remove : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "remove"

    override fun help(): String {
        return "remove number - Removes the track at this point in the queue."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got remove ${event.debugString()}")
        val trackNum = event.message.contentDisplay.substringAfter(' ').toIntOrNull()
        trackNum?.let {
            val removed = musicManager.scheduler.remove(it)
            sendMessage(logger, event.channel, codeBlock("Removing ${removed?.info?.title} from the queue."))
        }
    }
}
