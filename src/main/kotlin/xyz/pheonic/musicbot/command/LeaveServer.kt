package xyz.pheonic.musicbot.command

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class LeaveServer(private val musicManagers: MutableMap<Long, GuildMusicManager>) : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "disconnect"

    override fun help(): String {
        return "disconnect - Disconnects the bot from the voice channel it is in."
    }

    override fun execute(event: MessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got leaveServer ${event.debugString()}" }
        sendMessage(logger, event.channel, ":wave:")
        leaveServer(event, musicManager)
    }

    fun leaveServer(event: GuildVoiceUpdateEvent, musicManager: GuildMusicManager) {
        leaveServer(event, musicManager)
    }

    private fun leaveServer(event: MessageReceivedEvent, musicManager: GuildMusicManager) {
        event.guild.audioManager.closeAudioConnection()
        musicManager.scheduler.clearAll()
        musicManagers.remove(event.guild.idLong)
    }
}
