package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.Config
import xyz.pheonic.musicbot.GuildMusicManager

class Help(private val commands: Map<String, Command>) : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "musicbot-help"

    override fun help(): String {
        return "musicbot-help - Prints this help message."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got help ${event.debugString()}")
        var helpMessage = """
            All commands start with: ${Config.prefix}. For example ${Config.prefix}summon.
            The music bot can handle these formats: youtube, soundcloud, bandcamp, vimeo and direct links to music files.
            In the case of direct links it can handle these file types: mp3, flac, wav, webm, mp4, ogg, aac, m3u and pls.
            Commands:
        """.trimIndent() + "\n"
        for (command in commands.values) {
            helpMessage += "\t" + command.help() + "\n"
        }
        sendMessage(logger, event.channel, codeBlock(helpMessage))
    }
}
