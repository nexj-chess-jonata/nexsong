package br.com.nexj.nexsong.classify

import android.content.ContentResolver
import android.net.Uri
import br.com.nexj.nexsong.model.Song
import java.security.MessageDigest
import kotlin.math.abs

/**
 * Encontra músicas repetidas pelo título + artista (ou pelo nome do arquivo, se não houver artista)
 * e pela duração parecida. Nada é apagado aqui: só se monta a lista para o usuário decidir.
 */
object DuplicateFinder {

    /** Duas músicas só são candidatas se a duração diferir no máximo isto. */
    const val TOLERANCE_MS = 3_000L

    data class Group(val key: String, val songs: List<Song>)

    private val copySuffix = Regex(
        "(\\s*[(\\[]\\d+[)\\]]|\\s*-\\s*c[oó]pia|\\s*-\\s*copy|\\s+copy|\\s+c[oó]pia)\\s*$",
        RegexOption.IGNORE_CASE
    )

    private fun clean(text: String): String =
        GenreClassifier.normalize(text.replace(copySuffix, ""))

    private fun bucketKey(song: Song): String? {
        val artist = GenreClassifier.normalize(song.artist)
        if (artist.isNotEmpty()) {
            val title = clean(song.title)
            return if (title.isEmpty()) null else "$title|$artist"
        }
        val stem = clean(song.displayName.substringBeforeLast('.'))
        return if (stem.isEmpty()) null else "arquivo|$stem"
    }

    fun find(songs: List<Song>, ignored: Set<String>): List<Group> {
        val buckets = HashMap<String, ArrayList<Song>>()
        for (song in songs) {
            val key = bucketKey(song) ?: continue
            buckets.getOrPut(key) { ArrayList() }.add(song)
        }
        val groups = ArrayList<Group>()
        for ((key, list) in buckets) {
            if (list.size < 2) continue
            val sorted = list.sortedBy { it.durationMs }
            var cluster = ArrayList<Song>()
            fun close() {
                if (cluster.size >= 2) {
                    val ids = cluster.map { it.id }.sorted().joinToString(",")
                    val groupKey = "$key#$ids"
                    if (groupKey !in ignored) groups.add(Group(groupKey, cluster.toList()))
                }
                cluster = ArrayList()
            }
            for (song in sorted) {
                val last = cluster.lastOrNull()
                if (last != null && abs(song.durationMs - last.durationMs) > TOLERANCE_MS) close()
                cluster.add(song)
            }
            close()
        }
        return groups.sortedBy { it.songs.first().title.lowercase() }
    }

    /** Pontuação para sugerir qual cópia manter: formato sem perdas, metadados completos, letra salva. */
    private fun keepScore(song: Song, hasLyrics: Boolean): Int {
        val mime = song.mimeType.lowercase()
        val ext = song.displayName.substringAfterLast('.', "").lowercase()
        var score = 0
        if (mime.contains("flac") || mime.contains("wav") || ext == "flac" || ext == "wav" || ext == "alac") score += 4
        if (!song.tagGenre.isNullOrBlank()) score += 1
        if (song.album.isNotBlank()) score += 1
        if (hasLyrics) score += 1
        if (song.relativePath.startsWith(GenreClassifier.ORGANIZED_ROOT)) score += 1
        return score
    }

    /** A cópia sugerida para ficar. Em caso de empate, a mais antiga (menor id). */
    fun suggestedKeep(group: Group, lyricsIds: Set<Long>): Song =
        group.songs.sortedWith(
            compareByDescending<Song> { keepScore(it, it.id in lyricsIds) }.thenBy { it.id }
        ).first()

    /** Impressão digital (SHA-1) do conteúdo do arquivo. Deve rodar fora da thread principal. */
    fun fingerprint(resolver: ContentResolver, uri: Uri): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            val stream = resolver.openInputStream(uri) ?: return null
            stream.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
