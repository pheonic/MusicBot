package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class ShowQueue : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "queue"

    override fun help(): String {
        return "queue - Displays a message with the songs currently in the queue. Due to discord's character limit this may" +
                " not be all of the songs but the message will say the total amount of songs that can't be displayed" +
                " as well as the total time of all of the songs in the queue."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got showQueue ${event.debugString()}")
        var i = 1
        val currentTrack = musicManager.nowPlaying()
        if (currentTrack == null) {
            sendMessage(logger, event.channel, codeBlock("There are no songs in queue."))
            return
        }
        var totalTime = currentTrack.duration - currentTrack.position
        val stringBuilder = StringBuilder()
        stringBuilder.append(
            "Currently playing: ${currentTrack.info.title} " +
                    "(${millisToTime(currentTrack.position)} / ${millisToTime(currentTrack.duration)})\n\n"
        )
        val iterator = musicManager.scheduler.iterator()
        while (iterator.hasNext()) {
            val nextTrack = iterator.next()
            totalTime += nextTrack.duration
            if (stringBuilder.length < 1500) {
                stringBuilder.append("$i. ${nextTrack.info.title} (${millisToTime(nextTrack.duration)})\n")
                i++
            }
        }
        if (i < musicManager.scheduler.size()) {
            stringBuilder.append("\nAnd ${musicManager.scheduler.size() - i} more songs.")
        }
        stringBuilder.append("\nTotal queue time: ${millisToTime(totalTime)}")
        sendMessage(logger, event.channel, codeBlock(stringBuilder.toString()))
    }
}
