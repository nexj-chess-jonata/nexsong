package br.com.nexj.nexsong.data

import android.content.Context

/** Registro de onde cada música estava antes de ser movida, para permitir desfazer. */
class MoveLog(context: Context) {

    data class Entry(val id: Long, val originalPath: String)

    private val prefs = context.getSharedPreferences("nexsong_moves", Context.MODE_PRIVATE)

    fun count(): Int = prefs.all.size

    /** Guarda o local original. Se a música já foi movida antes, mantém o primeiro local. */
    fun add(entries: List<Entry>) {
        val editor = prefs.edit()
        for (entry in entries) {
            val key = "m:${entry.id}"
            if (!prefs.contains(key)) editor.putString(key, entry.originalPath)
        }
        editor.apply()
    }

    fun entries(limit: Int): List<Entry> {
        val result = ArrayList<Entry>()
        for ((key, value) in prefs.all) {
            if (result.size >= limit) break
            val id = key.removePrefix("m:").toLongOrNull() ?: continue
            val path = value as? String ?: continue
            result.add(Entry(id, path))
        }
        return result
    }

    fun remove(ids: List<Long>) {
        val editor = prefs.edit()
        for (id in ids) editor.remove("m:$id")
        editor.apply()
    }
}
