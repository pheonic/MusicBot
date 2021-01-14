package xyz.pheonic.musicbot.command

import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import xyz.pheonic.musicbot.GuildMusicManager

class Summon : Command {
    private val logger = KotlinLogging.logger { }
    override val internalName: String
        get() = "summon"

    override fun help(): String {
        return "summon - Summons the bot to the voice channel you are in."
    }

    override fun execute(event: GuildMessageReceivedEvent, musicManager: GuildMusicManager) {
        logger.debug("Got summon ${event.debugString()}")
        val summoner = event.member
        val audioManager = event.guild.audioManager
        val channel = summoner?.voiceState?.channel
        if (channel != null) {
            audioManager.openAudioConnection(channel)
        } else {
            sendMessage(logger, event.channel, codeBlock("${summoner?.effectiveName} is not in a voice channel"))
        }
    }
}
