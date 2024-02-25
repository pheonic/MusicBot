package xyz.pheonic.musicbot.command

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class ResumeSong : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "resume"

    override fun help(): String {
        return "resume - Resumes the currently playing song."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got resumeSong ${event.debugString()}" }
        musicManager.isPaused = false
    }
}
