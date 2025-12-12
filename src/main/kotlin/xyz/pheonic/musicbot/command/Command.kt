package xyz.pheonic.musicbot.command

import io.github.oshai.kotlinlogging.KLogger
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import xyz.pheonic.musicbot.GuildMusicManager
import java.time.Duration
import java.time.temporal.ChronoUnit

interface Command {
    val internalName: String
    fun help(): String
    fun execute(event: MessageReceivedEvent, musicManager: GuildMusicManager)

    fun MessageReceivedEvent.debugString() =
        "Event [id=${this.messageId}, author=${this.author.name}, content=${this.message.contentDisplay}]"

    fun codeBlock(s: String) = "```$s```"

    fun sendMessage(logger: KLogger, channel: MessageChannelUnion, message: String) {
        try {
            channel.sendMessage(message).complete()
        } catch (_: InsufficientPermissionException) {
            logger.warn { "Don't have permission to post in ${channel.name}" }
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
