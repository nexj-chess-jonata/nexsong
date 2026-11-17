package br.com.nexj.nexsong.data

import android.content.Context
import br.com.nexj.nexsong.model.Song

/** Lista das músicas que o usuário mandou para a lixeira do Android pelo NexSong. */
class TrashLog(context: Context) {

    data class Item(val id: Long, val title: String, val artist: String, val trashedAt: Long) {
        /** Dias que faltam até o Android apagar de vez (a lixeira guarda por 30 dias). */
        fun daysLeft(now: Long): Int =
            (RETENTION_DAYS - (now - trashedAt) / DAY_MS).toInt().coerceAtLeast(0)
    }

    private val prefs = context.getSharedPreferences("nexsong_trash", Context.MODE_PRIVATE)

    fun add(songs: List<Song>) {
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        for (song in songs) {
            val title = song.title.replace('\n', ' ')
            val artist = song.artist.replace('\n', ' ')
            editor.putString("t:${song.id}", "$now\n$title\n$artist")
        }
        editor.apply()
    }

    fun remove(ids: Collection<Long>) {
        val editor = prefs.edit()
        for (id in ids) editor.remove("t:$id")
        editor.apply()
    }

    /** Itens na lixeira, do mais recente para o mais antigo. Os vencidos (30 dias) saem da lista. */
    fun all(): List<Item> {
        val now = System.currentTimeMillis()
        val result = ArrayList<Item>()
        val expired = ArrayList<Long>()
        for ((key, value) in prefs.all) {
            val id = key.removePrefix("t:").toLongOrNull() ?: continue
            val parts = (value as? String)?.split('\n') ?: continue
            val time = parts.getOrNull(0)?.toLongOrNull() ?: continue
            if (now - time > RETENTION_DAYS * DAY_MS) {
                expired.add(id)
                continue
            }
            result.add(Item(id, parts.getOrNull(1).orEmpty(), parts.getOrNull(2).orEmpty(), time))
        }
        if (expired.isNotEmpty()) remove(expired)
        return result.sortedByDescending { it.trashedAt }
    }

    companion object {
        const val RETENTION_DAYS = 30L
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}
