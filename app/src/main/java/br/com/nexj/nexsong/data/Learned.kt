package br.com.nexj.nexsong.data

import android.content.Context
import br.com.nexj.nexsong.classify.GenreClassifier
import br.com.nexj.nexsong.model.Song

/**
 * Aprende com as correções de gênero do usuário.
 * Cada vez que o usuário define o gênero de uma música, o app anota um "voto" para o artista,
 * para o álbum e para a pasta dela. Quando os votos de um grupo apontam claramente para um
 * gênero, as outras músicas ainda sem gênero desse grupo recebem uma SUGESTÃO (nunca uma decisão).
 */
class Learned(context: Context) {

    /** Votos agrupados por "tipo:chave", cada um com a contagem por gênero. */
    class Data(val votes: Map<String, Map<String, Int>>) {
        val isEmpty: Boolean get() = votes.isEmpty()
    }

    data class Hit(val genreId: String, val source: String)

    private val prefs = context.getSharedPreferences("nexsong_learned", Context.MODE_PRIVATE)

    private fun keysOf(song: Song): List<Pair<String, String>> {
        val artist = GenreClassifier.normalize(song.artist)
        val album = GenreClassifier.normalize(song.album)
        val result = ArrayList<Pair<String, String>>()
        if (artist.isNotEmpty()) result.add(KIND_ARTIST to artist)
        if (artist.isNotEmpty() && album.isNotEmpty()) result.add(KIND_ALBUM to "$artist $album")
        val path = song.relativePath
        val organized = path.startsWith(GenreClassifier.ORGANIZED_ROOT) ||
            path.startsWith(FolderOrganizer.COPIES_ROOT)
        if (path.isNotBlank() && !organized) {
            val folder = GenreClassifier.normalize(path.replace('/', ' '))
            if (folder.isNotEmpty()) result.add(KIND_FOLDER to folder)
        }
        return result
    }

    /** Anota que o usuário definiu este gênero para esta música. */
    fun record(song: Song, genreId: String) {
        val editor = prefs.edit()
        for ((kind, key) in keysOf(song)) {
            val name = "$kind:$key|$genreId"
            editor.putInt(name, prefs.getInt(name, 0) + 1)
        }
        editor.apply()
    }

    fun load(): Data {
        val votes = HashMap<String, HashMap<String, Int>>()
        for ((name, value) in prefs.all) {
            val count = value as? Int ?: continue
            val cut = name.lastIndexOf('|')
            if (cut <= 0) continue
            votes.getOrPut(name.substring(0, cut)) { HashMap() }[name.substring(cut + 1)] = count
        }
        return Data(votes)
    }

    /** Quantos grupos (artista, álbum ou pasta) já têm algum voto. */
    fun count(): Int = load().votes.size

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** Sugestão para a música, ou null se os votos não forem claros o bastante. */
    fun suggest(song: Song, data: Data): Hit? {
        if (data.isEmpty) return null
        val keys = keysOf(song).associate { it.first to it.second }
        // Do mais específico para o mais geral.
        val order = listOf(
            Triple(KIND_ALBUM, MIN_ALBUM, "álbum que você corrigiu"),
            Triple(KIND_ARTIST, MIN_ARTIST, "artista que você corrigiu"),
            Triple(KIND_FOLDER, MIN_FOLDER, "pasta que você corrigiu")
        )
        for ((kind, minimum, label) in order) {
            val key = keys[kind] ?: continue
            val counts = data.votes["$kind:$key"] ?: continue
            val sorted = counts.entries.sortedByDescending { it.value }
            val best = sorted.firstOrNull() ?: continue
            val second = sorted.getOrNull(1)?.value ?: 0
            if (best.value >= minimum && best.value >= second * 2) {
                return Hit(best.key, label)
            }
        }
        return null
    }

    companion object {
        private const val KIND_ARTIST = "artist"
        private const val KIND_ALBUM = "album"
        private const val KIND_FOLDER = "folder"
        private const val MIN_ALBUM = 2
        private const val MIN_ARTIST = 2
        private const val MIN_FOLDER = 3
    }
}
