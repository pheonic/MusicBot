package xyz.pheonic.musicbot.command

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.junit.jupiter.api.Test
import xyz.pheonic.musicbot.GuildMusicManager


internal class SummonTest {

    @Test
    fun executeSummonerInChannel() {
        val event = mockk<GuildMessageReceivedEvent>(relaxed = true)
        val musicManager = mockk<GuildMusicManager>(relaxed = true)
        val channel = mockk<VoiceChannel>(relaxed = true)
        every { event.member?.effectiveName } returns "name"
        every { event.member?.voiceState?.channel } returns channel
        val summon = spyk<Summon>()
        summon.execute(event, musicManager)
        verify { event.guild.audioManager.openAudioConnection(ofType()) }
    }

    @Test
    fun executeSummonerNotInChannel() {
        val event = mockk<GuildMessageReceivedEvent>(relaxed = true)
        val musicManager = mockk<GuildMusicManager>(relaxed = true)
        mockk<VoiceChannel>(relaxed = true)
        every { event.member?.effectiveName } returns "name"
        every { event.member?.voiceState?.channel } returns null
        val summon = spyk<Summon>()
        summon.execute(event, musicManager)
        verify { summon.sendMessage(ofType(), ofType(), ofType()) }
    }
}
