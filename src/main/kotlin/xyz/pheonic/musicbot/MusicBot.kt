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
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.StatusType
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

    private fun codeBlock(s: String) = "```$s```"

    private fun sendMessage(channel: IChannel, message: String) {
        MessageBuilder(client).withChannel(channel).withContent(message).build()
    }

    fun sendNowPlayingMessage(guild: IGuild, track: AudioTrack?) {
        guild.channels.filter { it.longID in config.channels }.forEach {
            val duration = millisToTime(track?.duration ?: 0)
            sendMessage(it, codeBlock("Now playing: ${track?.info?.title} ($duration)"))
        }
        client.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "♪${track?.info?.title}♪")
    }

    fun clearPresence() {
        client.changePresence(StatusType.ONLINE)
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
        val musicManager = musicManagers.getOrDefault(guildId, GuildMusicManager(playerManager, guild, this, config))
        musicManagers[guildId] = musicManager
        guild.audioManager.audioProvider = musicManager.audioProvider()
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
        val guildAudioPlayer = guildAudioPlayer(event.guild)
        volume?.let { guildAudioPlayer.volume = it }
        sendMessage(event.channel, codeBlock("Volume set to ${guildAudioPlayer.volume}"))

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
            sendMessage(event.channel, codeBlock("There are no songs in queue."))
            return
        }
        var totalTime = currentTrack.duration - currentTrack.position
        val stringBuilder = StringBuilder()
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
        sendMessage(event.channel, codeBlock(stringBuilder.toString()))
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
        Thread(Runnable {
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
        }).start()
    }

    fun help(event: MessageEvent) {
        val helpMessage = """
            All commands start with: ${config.prefix}. For example ${config.prefix}summon.
            The music bot can handle these formats: youtube, soundcloud, bandcamp, vimeo and direct links to music files.
            In the case of direct links it can handle these file types: mp3, flac, wav, webm, mp4, ogg, aac, m3u and pls.
            Commands:
                summon - Summons the bot to the voice channel you are in.
                disconnect - Disconnects the bot from the voice channel it is in.
                play url - Adds this url to the queue.
                skip - Skips the currently playing song.
                pause - Pauses the currently playing song.
                resume - Resumes the currently playing song.
                queue - Displays a message with the songs currently in the queue. Due to discord's character limit this
                        may not be all of the songs but the message will say the total amount of songs that can't be
                        displayed as well as the total time of all of the songs in the queue.
                shuffle - Shuffles the songs in the queue.
                volume [number] - Sets the volume to the given number. If a number is not given will display the current
                                  volume level.
                repeat [off|one|all] - Sets the current repeat mode or prints repeat mode if no argument is provided.
                remove number - Removes the track at this point in the queue.
                clean - Deletes the bots messages and if the bot has the manage message permission will delete the
                        messages sent to it
                musicbot-help - Displays this help message.
        """.trimIndent()
        sendMessage(event.channel, codeBlock(helpMessage))
    }

    fun repeat(event: MessageEvent) {
        logger.debug("Got repeat ${event.debugString()}")
        val value = event.message.content.removePrefix("${config.prefix}repeat").trim().toUpperCase()
        val guildAudioPlayer = guildAudioPlayer(event.guild)
        if (value.isBlank()) {
            sendMessage(event.channel, codeBlock("Current repeat mode is: ${guildAudioPlayer.repeatMode}"))
        } else {
            val repeatMode = try {
                RepeatMode.valueOf(value)
            } catch (e: IllegalArgumentException) {
                null
            }
            if (repeatMode != null) {
                guildAudioPlayer.repeatMode = repeatMode
                sendMessage(event.channel, codeBlock("Set repeat mode to: $repeatMode"))
            } else {
                sendMessage(event.channel, codeBlock("Cannot find valid mode for $value. Use one of off, one, all"))
            }
        }
    }

    fun remove(event: MessageEvent) {
        logger.debug("Got remove ${event.debugString()}")
        val trackNum = event.message.content.substringAfter(' ').toIntOrNull()
        trackNum?.let {
            val removed = guildAudioPlayer(event.guild).scheduler.remove(it)
            sendMessage(event.channel, codeBlock("Removing ${removed?.info?.title} from the queue."))
        }
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
            track?.let {
                sendMessage(event.channel, "Adding ${it.info.title} to queue")
                musicManager.scheduler.queue(it)
            }
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
