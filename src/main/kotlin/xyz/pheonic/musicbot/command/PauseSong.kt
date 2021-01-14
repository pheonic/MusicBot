package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class PauseSong : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "pause"

    override fun help(): String {
        return "pause - Pauses the currently playing song."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got pauseSong ${event.debugString()}")
        musicManager.isPaused = true
    }
}
