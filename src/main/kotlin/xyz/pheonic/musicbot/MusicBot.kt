package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*


class MusicBot(private val client: JDA, private val config: Config) {
    private val logger = KotlinLogging.logger { }

    private fun GuildMessageReceivedEvent.debugString() =
        "Event [id=${this.messageId}, author=${this.author.name}, content=${this.message.contentDisplay}]"

    private var playerManager: AudioPlayerManager = DefaultAudioPlayerManager()
    private val musicManagers: MutableMap<Long, GuildMusicManager>

    init {
        musicManagers = HashMap()
        AudioSourceManagers.registerRemoteSources(playerManager)
        AudioSourceManagers.registerLocalSource(playerManager)
    }

    fun summon(event: GuildMessageReceivedEvent) {
        logger.debug("Got summon ${event.debugString()}")
        val summoner = event.member
        val voiceChannel = summoner?.voiceState?.channel
        val audioManager = event.guild.audioManager
        audioManager.openAudioConnection(voiceChannel)
        if (audioManager.isConnected) {
            sendMessage(event.channel, "$summoner is not in a voice channel")
        }
    }

    private fun codeBlock(s: String) = "```$s```"

    private fun sendMessage(channel: TextChannel, message: String) {
        try {
            channel.sendMessage(message).complete()
        } catch (e: Exception) {
            logger.warn("Don't have permission to post in ${channel.name}")
        }
    }

    fun sendNowPlayingMessage(guild: Guild, track: AudioTrack?) {
        guild.textChannels.filter { it.idLong in config.channels }.forEach {
            val duration = millisToTime(track?.duration ?: 0)
            sendMessage(it, codeBlock("Now playing: ${track?.info?.title} ($duration)"))
        }
//        client.changePresence(StatusType.ONLINE, Activity.ActivityType.LISTENING, "♪${track?.info?.title}♪")
    }

    fun clearPresence() {
//        client.changePresence(StatusType.ONLINE)
    }

    fun playSong(event: GuildMessageReceivedEvent) {
        logger.debug("Got playSong ${event.debugString()}")
        if (event.guild !in client.audioManagers.map { it.guild }) {
            summon(event)
        }
        val musicManager = guildAudioPlayer(event.guild)
        val trackUrl = event.message.contentDisplay.substringAfter(' ')
        playerManager.loadItemOrdered(
            musicManager,
            trackUrl,
            CustomAudioLoadResultHandler(event, musicManager, trackUrl)
        )
    }

    fun notACommand(event: GuildMessageReceivedEvent) {
        logger.debug("Got notACommand ${event.debugString()}")
    }

    @Synchronized
    private fun guildAudioPlayer(guild: Guild): GuildMusicManager {
        val guildId = guild.idLong
        val musicManager = musicManagers.getOrDefault(guildId, GuildMusicManager(playerManager, guild, this, config))
        musicManagers[guildId] = musicManager
        guild.audioManager.sendingHandler = musicManager.getSendHandler()
//        guild.guild.= musicManager . audioProvider ()
        return musicManager
    }

    fun skipSong(event: GuildMessageReceivedEvent) {
        logger.debug("Got skipSong ${event.debugString()}")
        guildAudioPlayer(event.guild).scheduler.skip()
    }

    fun leaveServer(event: GuildMessageReceivedEvent) {
        logger.debug("Got leaveServer ${event.debugString()}")
        sendMessage(event.channel, ":wave:")
        for (audioManager in client.audioManagers) {
            if (audioManager.guild == event.guild) {
                audioManager.closeAudioConnection()
                val guildAudioPlayer = guildAudioPlayer(event.guild)
                val currentMode = guildAudioPlayer.repeatMode
                guildAudioPlayer.repeatMode = RepeatMode.OFF
                guildAudioPlayer.scheduler.clearAll()
                guildAudioPlayer.repeatMode = currentMode
                break
            }
        }
    }

    fun pauseSong(event: GuildMessageReceivedEvent) {
        logger.debug("Got pauseSong ${event.debugString()}")
        guildAudioPlayer(event.guild).isPaused = true
    }

    fun resumeSong(event: GuildMessageReceivedEvent) {
        logger.debug("Got resumeSong ${event.debugString()}")
        guildAudioPlayer(event.guild).isPaused = false
    }

    fun clearPlaylist(event: GuildMessageReceivedEvent) {
        logger.debug("Got clearPlaylist ${event.debugString()}")
        guildAudioPlayer(event.guild).scheduler.clear()
        sendMessage(event.channel, codeBlock("Cleared queue"))
    }

    fun changeVolume(event: GuildMessageReceivedEvent) {
        logger.debug("Got changeVolume ${event.debugString()}")
        val volume = event.message.contentDisplay.substringAfter(' ').toIntOrNull()
        val guildAudioPlayer = guildAudioPlayer(event.guild)
        volume?.let { guildAudioPlayer.volume = it }
        sendMessage(event.channel, codeBlock("Volume set to ${guildAudioPlayer.volume}"))
    }

    fun shuffleQueue(event: GuildMessageReceivedEvent) {
        logger.debug("Got shuffleQueue ${event.debugString()}")
        guildAudioPlayer(event.guild).scheduler.shuffle()
        sendMessage(event.channel, codeBlock("Queue shuffled"))
    }

    fun showQueue(event: GuildMessageReceivedEvent) {
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
        event: GuildMessageReceivedEvent,
        commands: List<String>
    ) {
        logger.debug("Got clean ${event.debugString()}")
        //TODO
//        Thread(Runnable {
//            for (message in event.channel.iterableHistory) {
//                if (message.isPinned) continue
//                if (message.content.startsWith(config.prefix)) {
//                    val command = message.content.split(' ')[0].substring(config.prefix.length)
//                    if (command in commands) {
//                        message.delete()
//                    }
//                }
//                if (message.author.longID == client.ourUser.longID) {
//                    message.delete()
//                }
//                Thread.sleep(500)
//            }
//            logger.info("Finished cleaning")
//        }).start()
    }

    fun help(event: GuildMessageReceivedEvent) {
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
                                       NB: If repeat mode is set to one then skip becomes restart song. If you want the
                                       song to not play again either change the repeat mode or do clear-all.
                remove number - Removes the track at this point in the queue.
                clean - Deletes the bots messages and if the bot has the manage message permission will delete the
                        messages sent to it.
                clear - Clears the queue, doesn't clear the currently playing song.
                clear-all - Clears the queue and the currently playing song. (Bypasses repeat)
                musicbot-help - Displays this help message.
                restart - Reinitializes the bot, use in case the bot doesn't seem to be responding correctly.
        """.trimIndent()
        sendMessage(event.channel, codeBlock(helpMessage))
    }

    fun repeat(event: GuildMessageReceivedEvent) {
        logger.debug("Got repeat ${event.debugString()}")
        val value = event.message.contentDisplay.removePrefix("${config.prefix}repeat").trim().toUpperCase()
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

    fun remove(event: GuildMessageReceivedEvent) {
        logger.debug("Got remove ${event.debugString()}")
        val trackNum = event.message.contentDisplay.substringAfter(' ').toIntOrNull()
        trackNum?.let {
            val removed = guildAudioPlayer(event.guild).scheduler.remove(it)
            sendMessage(event.channel, codeBlock("Removing ${removed?.info?.title} from the queue."))
        }
    }

    fun restart(event: GuildMessageReceivedEvent) {
        logger.debug("Got restart ${event.debugString()}")
        for (audioManager in client.audioManagers) {
            audioManager.closeAudioConnection()
        }
        playerManager.shutdown()
        musicManagers.clear()
        playerManager = DefaultAudioPlayerManager()
        AudioSourceManagers.registerLocalSource(playerManager)
        AudioSourceManagers.registerRemoteSources(playerManager)
    }

    fun clearAll(event: GuildMessageReceivedEvent) {
        logger.debug("Got clear-all ${event.debugString()}")
        guildAudioPlayer(event.guild).scheduler.clearAll()
        sendMessage(event.channel, codeBlock("Cleared all"))
    }

    inner class CustomAudioLoadResultHandler(
        private val event: GuildMessageReceivedEvent,
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
