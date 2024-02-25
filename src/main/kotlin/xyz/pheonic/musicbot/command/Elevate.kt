package xyz.pheonic.musicbot.command

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class Elevate : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "elevate"

    override fun help(): String {
        return "elevate number - Elevates the track at this point in the queue to the top."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got elevate ${event.debugString()}" }
        val trackNum = event.message.contentDisplay.substringAfter(' ').toIntOrNull()
        trackNum?.let {
            val elevated = musicManager.scheduler.elevate(it)
            sendMessage(logger, event.channel, codeBlock("Elevated ${elevated?.info?.title} to the top of the queue."))
        }
    }
}
