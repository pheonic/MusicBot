package xyz.pheonic.musicbot

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageEmbedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class Listener(client: JDA, private val config: Config) : ListenerAdapter() {
    private val musicBot = MusicBot(client, config)
    private val commands = listOf(
        "summon",
        "disconnect",
        "play",
        "skip",
        "pause",
        "resume",
        "clear",
        "queue",
        "shuffle",
        "volume",
        "clean",
        "musicbot-help",
        "repeat",
        "remove",
        "restart",
        "clear-all"
    )

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val command = event.message.contentDisplay
        if (!command.startsWith(config.prefix)) return
        if (event.channel.idLong !in config.channels) return
        // When discord receives a message with a url in it it receives one event when the message is initially sent
        // and then another when the embed of the link loads, we ignore the second one since both events will have the
        // command.
        if (event is MessageEmbedEvent) return
        if (event is MessageDeleteEvent) return
        if (event is MessageUpdateEvent) return
        val action = command.split(' ')[0].removePrefix(config.prefix)
        when (action) {
            "summon" -> musicBot.summon(event)
            "disconnect" -> musicBot.leaveServer(event)
            "play" -> musicBot.playSong(event)
            "skip" -> musicBot.skipSong(event)
            "pause" -> musicBot.pauseSong(event)
            "resume" -> musicBot.resumeSong(event)
            "clear" -> musicBot.clearPlaylist(event)
            "clear-all" -> musicBot.clearAll(event)
            "queue" -> musicBot.showQueue(event)
            "shuffle" -> musicBot.shuffleQueue(event)
            "volume" -> musicBot.changeVolume(event)
            "clean" -> musicBot.clean(event, commands)
            "musicbot-help" -> musicBot.help(event)
            "repeat" -> musicBot.repeat(event)
            "remove" -> musicBot.remove(event)
            "restart" -> musicBot.restart(event)
            else -> musicBot.notACommand(event)
        }
    }
}
