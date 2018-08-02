package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import mu.KotlinLogging
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.util.MessageBuilder
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*


class MusicBot(private val client: IDiscordClient, private val config: Config) {
    private val logger = KotlinLogging.logger { }

    private fun MessageEvent.debugString() =
        "Event [id=${this.messageID}, author=${this.author.name}, content=${this.message.content}]"

    private val playerManager: AudioPlayerManager
    private val musicManagers: MutableMap<Long, GuildMusicManager>

    init {
        playerManager = DefaultAudioPlayerManager()
        musicManagers = HashMap()
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    fun summon(event: MessageEvent) {
        logger.debug("Got summon ${event.debugString()}")
        val summoner = event.author
        val voiceChannel = summoner.voiceStates[event.guild.longID].channel
        voiceChannel?.join() ?: sendMessage(event.channel, "$summoner is not in a voice channel")
    }

    private fun sendMessage(channel: IChannel, message: String) {
        MessageBuilder(client).withChannel(channel).withContent(message).build()
    }

    fun playSong(event: MessageEvent) {
        logger.debug("Got playSong ${event.debugString()}")
        val musicManager = guildAudioPlayer(event.guild)
        val trackUrl = event.message.content.substringAfter(' ')
        playerManager.loadItemOrdered(
            musicManager,
            trackUrl,
            CustomAudioLoadResultHandler(event, musicManager, trackUrl)
        )
    }

    fun notACommand(event: MessageEvent) {
        logger.debug("Got notACommand ${event.debugString()}")
    }

    @Synchronized
    private fun guildAudioPlayer(guild: IGuild): GuildMusicManager {
        val guildId = guild.longID
        val musicManager = musicManagers.getOrDefault(guildId, GuildMusicManager(playerManager))
        musicManagers[guildId] = musicManager
        guild.audioManager.audioProvider = musicManager.audioProvider()
        musicManager.volume = config.startVolume
        return musicManager
    }

    fun nextSong(event: MessageEvent) {
        logger.debug("Got nextSong ${event.debugString()}")
        guildAudioPlayer(event.guild).scheduler.next()
    }

    fun leaveServer(event: MessageEvent) {
        logger.debug("Got leaveServer ${event.debugString()}")
        sendMessage(event.channel, ":wave:")
        for (channel in client.connectedVoiceChannels) {
            if (channel.guild == event.guild) {
                channel.leave()
                break
            }
        }
    }

    fun pauseSong(event: MessageEvent) {
        logger.debug("Got pauseSong ${event.debugString()}")
        guildAudioPlayer(event.guild).isPaused = true
    }

    fun resumeSong(event: MessageEvent) {
        logger.debug("Got resumeSong ${event.debugString()}")
        guildAudioPlayer(event.guild).isPaused = false
    }

    fun clearPlaylist(event: MessageEvent) {
        logger.debug("Got clearPlaylist ${event.debugString()}")
        guildAudioPlayer(event.guild).scheduler.clear()
    }

    fun changeVolume(event: MessageEvent) {
        logger.debug("Got changeVolume ${event.debugString()}")
        val volume = event.message.content.substringAfter(' ').toIntOrNull()
        volume?.let { guildAudioPlayer(event.guild).volume = it }
    }

    fun shuffleQueue(event: MessageEvent) {
        logger.debug("Got shuffleQueue ${event.debugString()}")
        guildAudioPlayer(event.guild).scheduler.shuffle()
    }

    fun showQueue(event: MessageEvent) {
        logger.debug("Got showQueue ${event.debugString()}")
        val guildAudioPlayer = guildAudioPlayer(event.guild)
        var i = 1
        val currentTrack = guildAudioPlayer.nowPlaying()
        if (currentTrack == null) {
            sendMessage(event.channel, "```There are no songs in queue.```")
            return
        }
        var totalTime = currentTrack.duration
        val stringBuilder = StringBuilder("```")
        stringBuilder.append(
            "Currently playing: ${currentTrack.info.title} " +
                    "(${millisToTime(currentTrack.position)} / ${millisToTime(currentTrack.duration)})\n\n"
        )
        val iterator = guildAudioPlayer.scheduler.iterator()
        while (iterator.hasNext()) {
            val nextTrack = iterator.next()
            totalTime += nextTrack.duration
            if (stringBuilder.length < 1500) {
                stringBuilder.append("$i. ${nextTrack.info.title} (${millisToTime(nextTrack.duration)})\n")
                i++
            }
        }
        if (i < guildAudioPlayer.scheduler.size()) {
            stringBuilder.append("\nAnd ${guildAudioPlayer.scheduler.size() - i} more songs.")
        }
        stringBuilder.append("\nTotal queue time: ${millisToTime(totalTime)}")
        stringBuilder.append("```")
        sendMessage(event.channel, stringBuilder.toString())
    }

    private fun millisToTime(millis: Long): String {
        var duration = Duration.of(millis, ChronoUnit.MILLIS)
        val minutes = duration.toMinutes()
        duration = duration.minusMinutes(minutes)
        val seconds = duration.seconds
        return "%02d:%02d".format(minutes, seconds)
    }

    fun clean(
        event: MessageEvent,
        commands: List<String>
    ) {
        logger.debug("Got clean ${event.debugString()}")
        for (message in event.channel.fullMessageHistory) {
            if (message.isPinned) continue
            if (message.content.startsWith(config.prefix)) {
                val command = message.content.split(' ')[0].substring(config.prefix.length)
                if (command in commands) {
                    message.delete()
                }
            }
            if (message.author.longID == client.ourUser.longID) {
                message.delete()
            }
            Thread.sleep(500)
        }
        logger.info("Finished cleaning")
    }

    inner class CustomAudioLoadResultHandler(
        private val event: MessageEvent,
        private val musicManager: GuildMusicManager,
        private val trackUrl: String
    ) : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException?) {
            sendMessage(event.channel, "Could not play: ${exception?.message}")
        }

        override fun trackLoaded(track: AudioTrack?) {
            sendMessage(event.channel, "Adding ${track?.info?.title} to queue")
            musicManager.scheduler.queue(track!!)
        }

        override fun noMatches() {
            sendMessage(event.channel, "Nothing found at $trackUrl")
        }

        override fun playlistLoaded(playlist: AudioPlaylist?) {
            if (playlist?.selectedTrack != null) {
                sendMessage(event.channel, "Adding ${playlist.selectedTrack.info?.title} to queue")
                musicManager.scheduler.queue(playlist.selectedTrack)
            } else {
                val tracks = playlist?.tracks ?: throw Exception("Playlist without tracks")
                sendMessage(event.channel, "Adding ${tracks.size} tracks to the queue")
                for (track in tracks) {
                    musicManager.scheduler.queue(track)
                }
            }
        }
    }
}
