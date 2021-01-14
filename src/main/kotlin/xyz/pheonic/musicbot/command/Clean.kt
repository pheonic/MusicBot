package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.Config
import xyz.pheonic.musicbot.GuildMusicManager

class Clean(private val commands: Map<String, Command>, private val botId: Long) : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "clean"

    override fun help(): String {
        return "clean - Deletes the bots messages and if the bot has the manage message permission will delete the messages sent to it."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got clean ${event.debugString()}")
        Thread {
            for (message in event.channel.iterableHistory) {
                if (message.isPinned) continue
                if (message.contentDisplay.startsWith(Config.prefix)) {
                    val command = message.contentDisplay.split(' ')[0].substring(Config.prefix.length)
                    if (command in commands.keys) {
                        message.delete().reason("MusicBot Clean command").queue()
                    }
                }
                if (message.author.idLong == botId) {
                    message.delete().reason("MusicBot Clean command").queue()
                }
                Thread.sleep(500)
            }
            logger.info("Finished cleaning")
        }.start()
    }
}
