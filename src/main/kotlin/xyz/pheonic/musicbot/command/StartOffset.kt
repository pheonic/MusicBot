package xyz.pheonic.musicbot.command

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

internal fun applyStartMillis(track: AudioTrack, startMillis: Long?) {
    val millis = startMillis ?: return
    if (!track.isSeekable) return
    val duration = track.duration
    if (duration <= 0L) return
    track.position = millis.coerceIn(0L, duration)
}
