package xyz.pheonic.musicbot.command

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class ShufflePlaylist : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "shuffle"

    override fun help(): String {
        return "shuffle - Shuffles the songs in the queue."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got shuffleQueue ${event.debugString()}" }
        musicManager.scheduler.shuffle()
        sendMessage(logger, event.channel, codeBlock("Queue shuffled"))
    }
}
