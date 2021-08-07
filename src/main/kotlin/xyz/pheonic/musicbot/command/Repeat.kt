package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager
import xyz.pheonic.musicbot.RepeatMode

class Repeat : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "repeat"

    override fun help(): String {
        return "repeat [off|one|all] - Sets the current repeat mode or prints repeat mode if no argument is provided." +
                " NB: If repeat mode is set to one then skip becomes restart song. If you want the" +
                " song to not play again either change the repeat mode or do clear-all."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got repeat ${event.debugString()}")
        val value = event.message.contentDisplay.substringAfter(' ').substringBefore(' ').uppercase()
        if (value.isBlank()) {
            sendMessage(logger, event.channel, codeBlock("Current repeat mode is: ${musicManager.repeatMode}"))
        } else {
            val repeatMode = try {
                RepeatMode.valueOf(value)
            } catch (e: IllegalArgumentException) {
                null
            }
            if (repeatMode != null) {
                musicManager.repeatMode = repeatMode
                sendMessage(logger, event.channel, codeBlock("Set repeat mode to: $repeatMode"))
            } else {
                sendMessage(
                    logger,
                    event.channel,
                    codeBlock("Cannot find valid mode for $value. Use one of off, one, all")
                )
            }
        }
    }
}
