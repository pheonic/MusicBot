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


class MusicBot(private val client: IDiscordClient, private val config: Config) {
    //TODO improve the log messages to show the actual relevant content of the message instead of nothing
    private val logger = KotlinLogging.logger { }
    private val playerManager: AudioPlayerManager
    private val musicManagers: MutableMap<Long, GuildMusicManager>

    init {
        playerManager = DefaultAudioPlayerManager()
        musicManagers = HashMap()
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    fun summon(event: MessageEvent) {
        logger.debug("Got summon event $event")
        val summoner = event.author
        val voiceChannel = summoner.voiceStates[event.guild.longID].channel
        voiceChannel?.join() ?: sendMessage(event.channel, "$summoner is not in a voice channel")
    }

    private fun sendMessage(channel: IChannel, message: String) {
        MessageBuilder(client).withChannel(channel).withContent(message).build()
    }

    fun playSong(event: MessageEvent) {
        logger.debug("Got playSong event $event")
        val musicManager = guildAudioPlayer(event.guild)
        val trackUrl = event.message.content.substringAfter(' ')
        playerManager.loadItemOrdered(
            musicManager,
            trackUrl,
            CustomAudioLoadResultHandler(event, musicManager, trackUrl)
        )
    }

    fun notACommand(event: MessageEvent) {
        logger.debug("Got notACommand event $event")
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
        logger.debug("Got nextSong event $event")
        guildAudioPlayer(event.guild).scheduler.next()
    }

    fun leaveServer(event: MessageEvent) {
        logger.debug("Got leaveServer event $event")
        sendMessage(event.channel, ":wave:")
        for (channel in client.connectedVoiceChannels) {
            if (channel.guild == event.guild) {
                channel.leave()
                break
            }
        }
    }

    fun pauseSong(event: MessageEvent) {
        logger.debug("Got pauseSong event $event")
        guildAudioPlayer(event.guild).isPaused = true
    }

    fun resumeSong(event: MessageEvent) {
        logger.debug("Got resumeSong event $event")
        guildAudioPlayer(event.guild).isPaused = false
    }

    fun clearPlaylist(event: MessageEvent) {
        logger.debug("Got clearPlaylist event $event")
        guildAudioPlayer(event.guild).scheduler.clear()
    }

    fun changeVolume(event: MessageEvent) {
        logger.debug("Got changeVolume event $event")
        val volume = event.message.content.substringAfter(' ').toIntOrNull()
        volume?.let { guildAudioPlayer(event.guild).volume = it }
    }

    fun shuffleQueue(event: MessageEvent) {
        logger.debug("Got shuffleQueue event $event")
        guildAudioPlayer(event.guild).scheduler.shuffle()
    }

    fun showQueue(event: MessageEvent) {
        logger.debug("Got showQueue event $event")
        //TODO rewrite this to also show the currently playing song and how many songs there are total in the queue
        val iterator = guildAudioPlayer(event.guild).scheduler.iterator()
        var i = 1
        val stringBuilder = StringBuilder("Queue:\n")
        while (iterator.hasNext() && stringBuilder.length < 1500) {
            val nextTrack = iterator.next()
            stringBuilder.append("$i. ${nextTrack.info.title}\n")
            i++
        }
        sendMessage(event.channel, stringBuilder.toString())
    }

    fun clean(
        event: MessageEvent,
        commands: List<String>
    ) {
        logger.debug("Got clean event $event")
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
