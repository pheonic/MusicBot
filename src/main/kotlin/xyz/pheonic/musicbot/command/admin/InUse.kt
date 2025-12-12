package xyz.pheonic.musicbot.command.admin

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class InUse(private val musicManagers: HashMap<Long, GuildMusicManager>, private val client: JDA) :
    BotMasterOnlyCommand {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "in-use"

    override fun help(): String {
        return "in-use - Checks if the bot is in use and if so in which servers."
    }

    override fun execute(event: MessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.info { "Got in-use ${event.debugString()}" }
        val user = event.author
        if (!isBotMaster(user)) {
            logger.warn { "User ${user.name} is not the bot master" }
            return
        }
        var message = String()
        for ((guildId, manager) in musicManagers) {
            if (manager.nowPlaying() != null || manager.scheduler.size() > 0) {
                val guild = client.getGuildById(guildId)
                message += "Guild ${guild?.name} currently has activity with ${manager.scheduler.size()} items in the queue. User ${guild?.owner?.asMention} is the server owner."
            }
        }
        if (message.isEmpty()) {
            message = "There is currently no activity."
        }
        val dm = user.openPrivateChannel().complete()
        dm.sendMessage(message).complete()
    }
}
