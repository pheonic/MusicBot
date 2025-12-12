package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.lavalink.youtube.YoutubeAudioSourceManager
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import xyz.pheonic.musicbot.command.*
import xyz.pheonic.musicbot.command.admin.InUse
import java.time.Duration
import java.time.temporal.ChronoUnit

class MusicBot(private val client: JDA, private val config: Config) : ListenerAdapter() {
    private val logger = KotlinLogging.logger { }

    private fun MessageReceivedEvent.debugString() =
        "Event [id=${this.messageId}, author=${this.author.name}, content=${this.message.contentDisplay}]"

    private fun GuildVoiceUpdateEvent.debugString() =
        "Event [channelLeft=${this.channelLeft}]"

    private var playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: HashMap<Long, GuildMusicManager> = HashMap()
    private val commands: Map<String, Command>

    init {
        val ytSourceManager = YoutubeAudioSourceManager()
        config.youtubeRefreshToken?.let {
            ytSourceManager.useOauth2(it, true)
        }
        playerManager.registerSourceManager(ytSourceManager)
        AudioSourceManagers.registerRemoteSources(
            playerManager,
        )
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
        commands["in-use"] = InUse(musicManagers, client)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val command = event.message.contentDisplay
        if (!command.startsWith(config.prefix)) return
        if (event.channel.idLong !in config.channels) return
        val action = command.split(' ')[0].removePrefix(config.prefix)
        commands[action]?.let {
            it.execute(event, guildAudioPlayer(event.guild))
            return
        }
        notACommand(event)
    }

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (client.selfUser.idLong == event.member.user.idLong && event.channelJoined == null) {
            val leaveServer: LeaveServer = commands["disconnect"] as LeaveServer
            leaveServer.leaveServer(event, guildAudioPlayer(event.guild))
        } else {
            leaveChannelIfAlone(event)
        }
    }

    private fun leaveChannelIfAlone(event: GuildVoiceUpdateEvent) {
        if (config.botAloneTimer == null || config.botAloneTimer < 0) {
            return
        }
        val guild = event.guild
        if (event.channelLeft == null) {
            return
        }
        logger.trace { "Someone left/moved from a channel ${event.debugString()}" }
        val channelId = event.channelLeft!!.idLong
        val channel = getConnectedVoiceChannelInGuild(guild)

        if (channel?.idLong == channelId) {
            logger.trace { "Someone left my channel ${channel.name}" }
            if (channel.members.size == 1) {
                logger.trace { "I'm alone, leaving in ${config.botAloneTimer}ms" }
                Thread {
                    Thread.sleep(config.botAloneTimer)
                    logger.trace { "Checking to see if i'm still alone" }
                    val threadChannel = getConnectedVoiceChannelInGuild(guild)
                    if (threadChannel?.idLong == channelId) {
                        if (threadChannel.members.size == 1) {
                            logger.trace { "I'm alone, leaving voice channel" }
                            val leaveServer: LeaveServer = commands["disconnect"] as LeaveServer
                            leaveServer.leaveServer(event, guildAudioPlayer(guild))
                        }
                    }
                }.start()
            }
        }
    }

    private fun getConnectedVoiceChannelInGuild(guild: Guild): VoiceChannel? {
        for (audioManager in client.audioManagers) {
            if (audioManager.guild.idLong == guild.idLong && audioManager.isConnected) {
                return audioManager.connectedChannel as VoiceChannel?
            }
        }
        return null
    }

    private fun notACommand(event: MessageReceivedEvent) {
        logger.info { "Got notACommand ${event.debugString()}" }
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
            } catch (e: InsufficientPermissionException) {
                logger.warn { "Don't have permission to post in ${it.name}" }
            }
        }
    }

    fun sendTrackExceptionMessage(guild: Guild, track: AudioTrack?, exception: FriendlyException?) {
        guild.textChannels.filter { it.idLong in config.channels }.forEach {
            try {
                it.sendMessage("```Got an exception when trying to play \"${track?.info?.title}\"\n${exception?.cause?.stackTraceToString()}```")
                    .complete()
            } catch (e: InsufficientPermissionException) {
                logger.warn { "Don't have permission to post in ${it.name}" }
            }
        }
    }
}
