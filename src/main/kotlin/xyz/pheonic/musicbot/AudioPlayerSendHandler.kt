package xyz.pheonic.musicbot

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer


class AudioPlayerSendHandler(private val player: AudioPlayer) : AudioSendHandler {
    private val buffer: ByteBuffer = ByteBuffer.allocate(1024)
    private val frame: MutableAudioFrame = MutableAudioFrame()

    init {
        frame.setBuffer(buffer)
    }

    override fun provide20MsAudio(): ByteBuffer? {
        buffer.flip()
        return buffer
    }

    override fun canProvide(): Boolean {
        return player.provide(frame)
    }

    override fun isOpus(): Boolean {
        return true
    }
}


