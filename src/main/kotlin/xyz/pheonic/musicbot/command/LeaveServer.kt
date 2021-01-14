package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class LeaveServer(
    private val musicManagers: MutableMap<Long, GuildMusicManager>
) : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "disconnect"

    override fun help(): String {
        return "disconnect - Disconnects the bot from the voice channel it is in."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got leaveServer ${event.debugString()}")
        sendMessage(logger, event.channel, ":wave:")
        event.guild.audioManager.closeAudioConnection()
        musicManager.scheduler.clearAll()
        musicManagers.remove(event.guild.idLong)
    }
}
