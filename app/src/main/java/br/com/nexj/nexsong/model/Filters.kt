package br.com.nexj.nexsong.model

enum class FormatFilter(val label: String) {
    ALL("Todos"),
    MP3("MP3"),
    AAC("M4A / AAC"),
    FLAC("FLAC"),
    OGG("OGG / Opus"),
    OTHER("Outros");

    companion object {
        fun of(song: Song): FormatFilter {
            val mime = song.mimeType.lowercase()
            val ext = song.displayName.substringAfterLast('.', "").lowercase()
            return when {
                mime.contains("mpeg") || ext == "mp3" -> MP3
                mime.contains("mp4") || mime.contains("aac") || ext == "m4a" || ext == "aac" -> AAC
                mime.contains("flac") || ext == "flac" -> FLAC
                mime.contains("ogg") || mime.contains("opus") || mime.contains("vorbis") ||
                    ext == "ogg" || ext == "opus" -> OGG
                else -> OTHER
            }
        }
    }
}

enum class DurationFilter(val label: String) {
    ALL("Qualquer"),
    SHORT("Até 2 min"),
    MEDIUM("De 2 a 6 min"),
    LONG("Mais de 6 min");

    fun matches(durationMs: Long): Boolean = when (this) {
        ALL -> true
        SHORT -> durationMs < 120_000L
        MEDIUM -> durationMs in 120_000L..360_000L
        LONG -> durationMs > 360_000L
    }
}

enum class SortBy(val label: String) {
    TITLE("Título"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    DURATION("Duração")
}

enum class LyricsFilter(val label: String) {
    ALL("Todas"),
    WITH("Com letra salva"),
    WITHOUT("Sem letra")
}
