package xyz.pheonic.musicbot.command.admin

import net.dv8tion.jda.api.entities.User
import xyz.pheonic.musicbot.Config
import xyz.pheonic.musicbot.command.Command

interface BotMasterOnlyCommand : Command {
    fun isBotMaster(user: User): Boolean {
        return user.idLong == Config.botMaster
    }
}
