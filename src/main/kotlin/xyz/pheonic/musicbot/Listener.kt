package xyz.pheonic.musicbot

import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEmbedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent


class Listener(client: IDiscordClient, private val config: Config) {
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
        "repeat-one",
        "repeat-all",
        "repeat-mode"
    )

    @EventSubscriber
    fun handleEvent(event: MessageEvent) {
        val command = event.message.content
        if (!command.startsWith(config.prefix)) return
        if (event.channel.longID !in config.channels) return
        // When discord receives a message with a url in it it receives one event when the message is initially sent
        // and then another when the embed of the link loads, we ignore the second one since both events will have the
        // command.
        if (event is MessageEmbedEvent) return
        if (event is MessageDeleteEvent) return
        val action = command.split(' ')[0].removePrefix(config.prefix)
        when (action) {
            "summon" -> musicBot.summon(event)
            "disconnect" -> musicBot.leaveServer(event)
            "play" -> musicBot.playSong(event)
            "skip" -> musicBot.nextSong(event)
            "pause" -> musicBot.pauseSong(event)
            "resume" -> musicBot.resumeSong(event)
            "clear" -> musicBot.clearPlaylist(event)
            "queue" -> musicBot.showQueue(event)
            "shuffle" -> musicBot.shuffleQueue(event)
            "volume" -> musicBot.changeVolume(event)
            "clean" -> musicBot.clean(event, commands)
            "musicbot-help" -> musicBot.help(event)
            "repeat-one" -> musicBot.repeat(event, RepeatMode.ONE)
            "repeat-all" -> musicBot.repeat(event, RepeatMode.ALL)
            "repeat-off" -> musicBot.repeat(event, RepeatMode.OFF)
            "repeat-mode" -> musicBot.repeatMode(event)
            else -> musicBot.notACommand(event)
        }
    }
}
