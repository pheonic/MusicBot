package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageEmbedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import xyz.pheonic.musicbot.command.*
import java.time.Duration
import java.time.temporal.ChronoUnit

class MusicBot(private val client: JDA, private val config: Config) : ListenerAdapter() {
    private val logger = KotlinLogging.logger { }

    private fun GuildMessageReceivedEvent.debugString() =
        "Event [id=${this.messageId}, author=${this.author.name}, content=${this.message.contentDisplay}]"

    private fun GenericGuildVoiceUpdateEvent.debugString() =
        "Event [channelLeft=${this.channelLeft}]"

    private var playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager>
    private val commands: Map<String, Command>

    init {
        musicManagers = HashMap()
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
        commands = LinkedHashMap()
        commands["summon"] = Summon()
        commands["disconnect"] = LeaveServer(musicManagers)
        commands["play"] = PlaySong()
        commands["pause"] = PauseSong()
        commands["resume"] = ResumeSong()
        commands["skip"] = SkipSong()
        commands["shuffle"] = ShufflePlaylist()
        commands["clear"] = ClearPlaylist()
        commands["volume"] = ChangeVolume()
        commands["queue"] = ShowQueue()
        commands["clear-all"] = ClearAll()
        commands["repeat"] = Repeat()
        commands["remove"] = Remove()
        commands["elevate"] = Elevate()
        commands["seek"] = Seek()
        commands["playnow"] = PlayNow()
        commands["playnext"] = PlayNext()
        commands["musicbot-help"] = Help(commands)
        commands["clean"] = Clean(commands, client.selfUser.idLong)
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        val command = event.message.contentDisplay
        if (!command.startsWith(config.prefix)) return
        if (event.channel.idLong !in config.channels) return
        // When discord receives a message with a url in it, it receives one event when the message is initially sent
        // and then another when the embed of the link loads, we ignore the second one since both events will have the
        // command.
        if (event is MessageEmbedEvent) return
        if (event is MessageDeleteEvent) return
        if (event is MessageUpdateEvent) return
        val action = command.split(' ')[0].removePrefix(config.prefix)
        commands[action]?.let {
            it.execute(event, guildAudioPlayer(event.guild))
            return
        }
        notACommand(event)
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        leaveChannelIfAlone(event)
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        leaveChannelIfAlone(event)
    }

    private fun leaveChannelIfAlone(event: GenericGuildVoiceUpdateEvent) {
        val guild = event.guild
        if (event.channelLeft == null) {
            return
        }
        logger.trace("Someone left/moved from a channel ${event.debugString()}")
        val channelId = event.channelLeft!!.idLong
        val channel = getConnectedVoiceChannelInGuild(guild)

        if (channel?.idLong == channelId) {
            logger.trace("Someone left my channel ${channel.name}")
            if (channel.members.size == 1) {
                logger.trace("I'm alone, leaving in ${config.botAloneTimer}ms")
                Thread {
                    Thread.sleep(config.botAloneTimer)
                    logger.trace("Checking to see if i'm still alone")
                    val threadChannel = getConnectedVoiceChannelInGuild(guild)
                    if (threadChannel?.idLong == channelId) {
                        if (threadChannel.members.size == 1) {
                            logger.trace("I'm alone, leaving voice channel")
                            val leaveServer: LeaveServer = commands["disconnect"] as LeaveServer
                            leaveServer.execute(event, guildAudioPlayer(guild))
                        }
                    }
                }.start()
            }
        }
    }

    private fun getConnectedVoiceChannelInGuild(guild: Guild): VoiceChannel? {
        for (audioManager in client.audioManagers) {
            if (audioManager.guild.idLong == guild.idLong && audioManager.isConnected) {
                return audioManager.connectedChannel
            }
        }
        return null
    }

    private fun notACommand(event: GuildMessageReceivedEvent) {
        logger.debug("Got notACommand ${event.debugString()}")
    }

    @Synchronized
    private fun guildAudioPlayer(guild: Guild): GuildMusicManager {
        val guildId = guild.idLong
        val musicManager = musicManagers.getOrDefault(
            guildId,
            GuildMusicManager(playerManager, guild, this, config)
        )
        musicManagers[guildId] = musicManager
        guild.audioManager.sendingHandler = musicManager.getSendHandler()
        return musicManager
    }

    private fun millisToTime(millis: Long): String {
        var duration = Duration.of(millis, ChronoUnit.MILLIS)
        val minutes = duration.toMinutes()
        duration = duration.minusMinutes(minutes)
        val seconds = duration.seconds
        return "%02d:%02d".format(minutes, seconds)
    }

    fun sendNowPlayingMessage(guild: Guild, track: AudioTrack?) {
        guild.textChannels.filter { it.idLong in config.channels }.forEach {
            val duration = millisToTime(track?.duration ?: 0)
            try {
                it.sendMessage("```Now playing: ${track?.info?.title} ($duration)```").complete()
            } catch (e: Exception) {
                logger.warn("Don't have permission to post in ${it.name}")
            }
        }
    }
}
