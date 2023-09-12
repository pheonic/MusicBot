package xyz.pheonic.musicbot.command

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class Remove : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "remove"

    override fun help(): String {
        return "remove number - Removes the track at this point in the queue."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got remove ${event.debugString()}")
        var trackRanges = event.message.contentDisplay.substringAfter(' ')
            .replace(" ", "")
            .split(",")
            .map { s -> convertStringRangeToNumbers(s) }
            .flatten()
        if (trackRanges.isNotEmpty()) {
            val removed = musicManager.scheduler.remove(trackRanges)
            sendMessage(
                logger,
                event.channel,
                codeBlock("${removedEnsuringDiscordLimit(removed)}")
            )
        }
    }

    private fun removedEnsuringDiscordLimit(removed: List<AudioTrack>): String {
        if (removed.size == 1) {
            return "Removing ${removed[0].info.title} from the queue."
        }
        val stringBuilder = StringBuilder()
        stringBuilder.append("Removing:\n")
        var i = 0
        for (it in removed) {
            stringBuilder.append("${it.info.title}\n")
            i++
            if (stringBuilder.length > 1500) {
                break
            }
        }
        if (i < removed.size) {
            stringBuilder.append("And ${removed.size - i} more from the queue.")
        } else {
            stringBuilder.append("From the queue.")
        }
        return stringBuilder.toString()
    }

    private fun convertStringRangeToNumbers(s: String): List<Int> {
        if (s.matches("^(\\d+)-(\\d+)$".toRegex())) {
            val start = s.substringBefore("-")
            val end = s.substringAfter("-")
            if (start.all { c -> c.isDigit() } && end.all { c -> c.isDigit() }) {
                return if (start.toInt() < end.toInt()) (start.toInt()..end.toInt()).toList()
                else (end.toInt()..start.toInt()).toList()
            }
        } else if (s.all { c -> c.isDigit() }) {
            return listOf(s.toInt())
        }
        return emptyList()
    }
}
