package xyz.pheonic.musicbot.command

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class LeaveServer(private val musicManagers: MutableMap<Long, GuildMusicManager>) : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "disconnect"

    override fun help(): String {
        return "disconnect - Disconnects the bot from the voice channel it is in."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got leaveServer ${event.debugString()}" }
        sendMessage(logger, event.channel, ":wave:")
        leaveServer(event, musicManager)
    }

    fun execute(event: GenericGuildVoiceUpdateEvent, musicManager: GuildMusicManager) {
        leaveServer(event, musicManager)
    }

    private fun leaveServer(event: GenericGuildEvent, musicManager: GuildMusicManager) {
        event.guild.audioManager.closeAudioConnection()
        musicManager.scheduler.clearAll()
        musicManagers.remove(event.guild.idLong)
    }
}
