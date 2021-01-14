package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class SkipSong : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "skip"

    override fun help(): String {
        return "skip - Skips the currently playing song."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got skipSong ${event.debugString()}")
        musicManager.scheduler.skip()
    }
}
