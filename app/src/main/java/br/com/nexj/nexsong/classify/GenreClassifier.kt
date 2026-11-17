package br.com.nexj.nexsong.classify

import br.com.nexj.nexsong.model.Genre
import br.com.nexj.nexsong.model.Song
import java.text.Normalizer
import java.util.Locale

/**
 * Classifica uma música em um dos gêneros, sem internet.
 * Ordem de prioridade:
 *  1. correção do usuário para a música
 *  2. correção do usuário para o artista
 *  3. pasta já organizada pelo NexSong
 *  4. tag de gênero do arquivo
 *  5. base local de artistas (pelo campo artista; depois pelo nome do arquivo, como sugestão)
 *  6. nome da pasta (vira sugestão, aguarda confirmação)
 *  7. nome do artista começando com "MC" (sugestão de Funk)
 *  8. Outros
 */
object GenreClassifier {

    data class Result(val genre: Genre, val source: String, val confirmed: Boolean)

    const val ORGANIZED_ROOT = "Music/NexSong/"

    private val diacritics = Regex("\\p{Mn}+")
    private val notAlnum = Regex("[^a-z0-9 ]")
    private val spaces = Regex("\\s+")

    fun normalize(text: String): String {
        val lower = text.replace("&", " e ").lowercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD)
        return decomposed
            .replace(diacritics, "")
            .replace(notAlnum, " ")
            .replace(spaces, " ")
            .trim()
    }

    /** Palavras (já normalizadas) que indicam cada gênero na tag do arquivo. A ordem importa. */
    private val tagKeywords: List<Pair<String, List<String>>> = listOf(
        "gospel" to listOf("gospel", "louvor", "adoracao", "christian", "crista", "catolic", "worship", "religio"),
        "forro" to listOf("forro", "baiao", "xote", "xaxado", "piseiro", "pisadinha"),
        "pagode" to listOf("pagode"),
        "samba" to listOf("samba", "partido alto"),
        "sertanejo" to listOf("sertanej", "modao", "caipira", "country"),
        "funk" to listOf("funk"),
        "hiphop" to listOf("hip hop", "hiphop", "rap", "trap"),
        "eletronica" to listOf("eletronic", "electronic", "electro", "house", "techno", "trance", "edm", "dance", "dubstep"),
        "mpb" to listOf("mpb", "bossa", "tropicalia", "musica popular brasileira"),
        "rock" to listOf("rock", "metal", "punk", "grunge", "hardcore", "alternativ", "indie"),
        "internacional" to listOf("internacional", "international", "pop", "r b", "rnb", "soul", "disco")
    )

    private val exactArtists = HashMap<String, String>()
    private val multiWordArtists = ArrayList<Pair<String, String>>()

    init {
        for ((genreId, names) in ArtistDictionary.byGenre) {
            for (raw in names) {
                val name = normalize(raw)
                if (name.isEmpty()) continue
                if (name.contains(' ')) {
                    if (multiWordArtists.none { it.first == name }) multiWordArtists.add(name to genreId)
                } else {
                    exactArtists.putIfAbsent(name, genreId)
                }
            }
        }
    }

    /** Verdadeiro se alguma palavra do texto começa com a palavra-chave. */
    private fun hasWordStart(text: String, keyword: String): Boolean =
        " $text".contains(" $keyword")

    /** Verdadeiro se o texto contém a expressão como palavras inteiras. */
    private fun hasWords(text: String, phrase: String): Boolean =
        " $text ".contains(" $phrase ")

    private fun customGenres(genres: Map<String, Genre>): List<Genre> =
        genres.values.filter { g -> g.id != Genre.OUTROS_ID && Genre.DEFAULTS.none { it.id == g.id } }

    fun fromTag(tag: String?, genres: Map<String, Genre>): Genre? {
        if (tag.isNullOrBlank()) return null
        val n = normalize(tag)
        if (n.isEmpty()) return null
        for (custom in customGenres(genres)) {
            val label = normalize(custom.label)
            if (label.isNotEmpty() && hasWords(n, label)) return custom
        }
        for ((genreId, words) in tagKeywords) {
            val genre = genres[genreId] ?: continue
            if (words.any { hasWordStart(n, it) }) return genre
        }
        return null
    }

    fun fromArtist(artist: String, genres: Map<String, Genre>): Genre? {
        val n = normalize(artist)
        if (n.isEmpty()) return null
        exactArtists[n]?.let { id -> genres[id]?.let { return it } }
        for ((name, genreId) in multiWordArtists) {
            if (hasWords(n, name)) {
                genres[genreId]?.let { return it }
            }
        }
        return null
    }

    fun fromPath(path: String, genres: Map<String, Genre>): Genre? {
        val n = normalize(path.replace('/', ' '))
        if (n.isEmpty()) return null
        for (genre in genres.values) {
            if (genre.id == Genre.OUTROS_ID) continue
            val label = normalize(genre.label)
            if (label.isNotEmpty() && hasWords(n, label)) return genre
        }
        return null
    }

    fun classify(
        song: Song,
        songOverride: Genre?,
        artistOverride: Genre?,
        genres: Map<String, Genre>
    ): Result {
        songOverride?.let { return Result(it, "você", true) }
        artistOverride?.let { return Result(it, "você", true) }

        if (song.relativePath.startsWith(ORGANIZED_ROOT)) {
            fromPath(song.relativePath.removePrefix(ORGANIZED_ROOT), genres)?.let {
                return Result(it, "pasta NexSong", true)
            }
        }

        fromTag(song.tagGenre, genres)?.let { return Result(it, "tag", true) }
        fromArtist(song.artist, genres)?.let { return Result(it, "artista", true) }
        // Nome do artista no começo do nome do arquivo ("Artista - Música.mp3") quando a tag está errada.
        val base = song.displayName.substringBeforeLast('.')
        (fromArtist(base.substringBefore(" - "), genres) ?: fromArtist("$base ${song.title}", genres))
            ?.let { return Result(it, "nome do arquivo", false) }
        fromPath(song.relativePath, genres)?.let { return Result(it, "nome da pasta", false) }

        val funk = genres["funk"]
        if (funk != null && normalize(song.artist).startsWith("mc ")) {
            return Result(funk, "nome do artista", false)
        }
        return Result(Genre.OUTROS, "", true)
    }
}
