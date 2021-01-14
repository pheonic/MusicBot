package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class ChangeVolume : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "volume"

    override fun help(): String {
        return "volume [number] - Sets the volume to the given number. If a number is not given will display the current volume level."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got changeVolume ${event.debugString()}")
        val volume = event.message.contentDisplay.substringAfter(' ').toIntOrNull()
        volume?.let { musicManager.volume = it }
        sendMessage(logger, event.channel, codeBlock("Volume set to ${musicManager.volume}"))
    }
}
