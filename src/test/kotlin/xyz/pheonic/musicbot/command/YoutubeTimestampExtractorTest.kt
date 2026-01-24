package xyz.pheonic.musicbot.command

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class YoutubeTimestampTest {

    @Test
    fun `parses t in query as seconds`() {
        val ms = YoutubeTimestamp.extractStartMillis("https://www.youtube.com/watch?v=abc123&t=90")
        assertEquals(90_000L, ms)
    }

    @Test
    fun `parses start in query as seconds`() {
        val ms = YoutubeTimestamp.extractStartMillis("https://www.youtube.com/watch?v=abc123&start=120")
        assertEquals(120_000L, ms)
    }

    @Test
    fun `parses youtu_be t in query`() {
        val ms = YoutubeTimestamp.extractStartMillis("https://youtu.be/abc123?t=45")
        assertEquals(45_000L, ms)
    }

    @Test
    fun `parses fragment t as seconds`() {
        val ms = YoutubeTimestamp.extractStartMillis("https://www.youtube.com/watch?v=abc123#t=33")
        assertEquals(33_000L, ms)
    }

    @Test
    fun `parses fragment t as hms`() {
        val ms = YoutubeTimestamp.extractStartMillis("https://www.youtube.com/watch?v=abc123#t=1h2m3s")
        assertEquals((1 * 3600 + 2 * 60 + 3) * 1000L, ms)
    }

    @Test
    fun `parses query t as hms`() {
        val ms = YoutubeTimestamp.extractStartMillis("https://www.youtube.com/watch?v=abc123&t=2m10s")
        assertEquals(130_000L, ms)
    }

    @Test
    fun `ignores missing timestamp`() {
        val ms = YoutubeTimestamp.extractStartMillis("https://www.youtube.com/watch?v=abc123")
        assertNull(ms)
    }

    @Test
    fun `ignores non-youtube host`() {
        val ms = YoutubeTimestamp.extractStartMillis("https://example.com/watch?v=abc123&t=90")
        assertNull(ms)
    }

    @Test
    fun `ignores non-url input`() {
        val ms = YoutubeTimestamp.extractStartMillis("ytsearch: never gonna give you up")
        assertNull(ms)
    }

    @Test
    fun `returns null for zero-ish hms value`() {
        // With the implementation shown earlier, "0s" and "0m0s" return null (treated like absent).
        val ms = YoutubeTimestamp.extractStartMillis("https://www.youtube.com/watch?v=abc123&t=0s")
        assertNull(ms)
    }
}
