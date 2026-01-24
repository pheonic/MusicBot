package xyz.pheonic.musicbot.command

import java.net.URI

/**
 * Extracts a start position (ms) from common YouTube URL timestamp formats:
 * - https://youtu.be/<id>?t=90
 * - https://www.youtube.com/watch?v=<id>&t=1m30s
 * - https://www.youtube.com/watch?v=<id>&start=90
 * - https://www.youtube.com/watch?v=<id>#t=1m30s
 *
 * Returns null if the input is not a URL or no timestamp is present.
 */
object YoutubeTimestamp {
    fun extractStartMillis(inputUrl: String): Long? {
        val uri = runCatching { URI(inputUrl.trim()) }.getOrNull() ?: return null
        val host = (uri.host ?: return null).lowercase()
        if (!host.contains("youtube.com") && !host.contains("youtu.be")) return null

        val queryParams = parseQueryParams(uri.rawQuery)
        val fragmentParams = parseFragmentParams(uri.rawFragment)

        val raw =
            queryParams["t"]
                ?: queryParams["start"]
                ?: fragmentParams["t"]
                ?: fragmentParams["start"]
                ?: return null

        val seconds = parseYoutubeTimeToSeconds(raw) ?: return null
        return (seconds * 1000L).coerceAtLeast(0L)
    }

    private fun parseQueryParams(rawQuery: String?): Map<String, String> =
        rawQuery
            ?.split("&")
            ?.mapNotNull { part ->
                if (part.isBlank()) return@mapNotNull null
                val idx = part.indexOf('=')
                if (idx < 0) part to "" else part.substring(0, idx) to part.substring(idx + 1)
            }
            ?.associate { (k, v) -> decodeUrlComponent(k) to decodeUrlComponent(v) }
            ?: emptyMap()

    private fun parseFragmentParams(rawFragment: String?): Map<String, String> {
        // Common fragments:
        //   "t=1m30s"
        //   "t=90"
        //   "0m10s" (rare, but seen)
        val fragment = rawFragment?.trim().orEmpty()
        if (fragment.isBlank()) return emptyMap()
        if (fragment.contains("=")) return parseQueryParams(fragment)
        return mapOf("t" to fragment)
    }

    private fun decodeUrlComponent(s: String): String =
        runCatching { java.net.URLDecoder.decode(s, Charsets.UTF_8) }.getOrDefault(s)

    private fun parseYoutubeTimeToSeconds(raw: String): Long? {
        val s = raw.trim().lowercase()
        if (s.isBlank()) return null

        // Plain integer seconds, e.g. "90"
        s.toLongOrNull()?.let { return it.coerceAtLeast(0L) }

        // Formats like "1h2m3s", "2m10s", "45s"
        val regex = Regex("""^(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?$""")
        val m = regex.matchEntire(s) ?: return null
        val hours = m.groupValues[1].takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
        val minutes = m.groupValues[2].takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
        val seconds = m.groupValues[3].takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
        val total = hours * 3600 + minutes * 60 + seconds
        return if (total == 0L) null else total
    }
}
