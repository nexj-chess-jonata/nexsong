package br.com.nexj.nexsong.data

/** Uma linha de letra. timeMs = -1 quando a letra não é sincronizada. */
data class LyricLine(val timeMs: Long, val text: String)

data class ParsedLyrics(val lines: List<LyricLine>, val synced: Boolean)

/** Lê letras no formato LRC ([mm:ss.xx] texto) ou texto simples. */
object LrcParser {

    private val timeTag = Regex("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?\\]")
    private val metaTag = Regex("^\\[[a-zA-Z#]+:.*\\]$")

    private fun toMs(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLongOrNull() ?: 0L
        val seconds = match.groupValues[2].toLongOrNull() ?: 0L
        val fraction = match.groupValues[3]
        val millis = when (fraction.length) {
            0 -> 0L
            1 -> (fraction.toLongOrNull() ?: 0L) * 100L
            2 -> (fraction.toLongOrNull() ?: 0L) * 10L
            else -> fraction.toLongOrNull() ?: 0L
        }
        return minutes * 60_000L + seconds * 1_000L + millis
    }

    fun parse(text: String): ParsedLyrics {
        val timed = ArrayList<LyricLine>()
        val plain = ArrayList<String>()

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (metaTag.matches(line)) continue

            var rest = line
            val times = ArrayList<Long>()
            while (true) {
                val match = timeTag.find(rest) ?: break
                if (match.range.first != 0) break
                times.add(toMs(match))
                rest = rest.substring(match.range.last + 1).trimStart()
            }

            if (times.isEmpty()) {
                plain.add(line)
            } else {
                for (time in times) timed.add(LyricLine(time, rest))
                plain.add(rest)
            }
        }

        if (timed.isNotEmpty()) {
            return ParsedLyrics(timed.sortedBy { it.timeMs }, true)
        }
        val trimmed = plain.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        return ParsedLyrics(trimmed.map { LyricLine(-1L, it) }, false)
    }
}
