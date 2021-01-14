package xyz.pheonic.musicbot.command

import mu.KLogger
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager
import java.time.Duration
import java.time.temporal.ChronoUnit

interface Command {
    val internalName: String
    fun help(): String
    fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager)

    fun GuildMessageReceivedEvent.debugString() =
        "Event [id=${this.messageId}, author=${this.author.name}, content=${this.message.contentDisplay}]"

    fun codeBlock(s: String) = "```$s```"

    fun sendMessage(logger: KLogger, channel: TextChannel, message: String) {
        try {
            channel.sendMessage(message).complete()
        } catch (e: Exception) {
            logger.warn("Don't have permission to post in ${channel.name}")
        }
    }

    fun millisToTime(millis: Long): String {
        var duration = Duration.of(millis, ChronoUnit.MILLIS)
        val minutes = duration.toMinutes()
        duration = duration.minusMinutes(minutes)
        val seconds = duration.seconds
        return "%02d:%02d".format(minutes, seconds)
    }
}
