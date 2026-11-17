package br.com.nexj.nexsong.data

import android.content.Context
import java.io.File

/** Letras guardadas no aparelho (um arquivo .lrc por música), para uso sem internet. */
class LyricsStore(context: Context) {

    private val dir = File(context.filesDir, "lyrics")
    private val prefs = context.getSharedPreferences("nexsong_lyrics", Context.MODE_PRIVATE)

    private fun file(id: Long) = File(dir, "$id.lrc")

    /** Ids das músicas que já têm letra salva. */
    fun ids(): Set<Long> = try {
        dir.list()?.mapNotNull { it.removeSuffix(".lrc").toLongOrNull() }?.toSet() ?: emptySet()
    } catch (e: Exception) {
        emptySet()
    }

    fun load(id: Long): String? = try {
        val f = file(id)
        if (f.exists()) f.readText() else null
    } catch (e: Exception) {
        null
    }

    fun save(id: Long, text: String): Boolean = try {
        dir.mkdirs()
        file(id).writeText(text)
        prefs.edit().remove("nf:$id").apply()
        true
    } catch (e: Exception) {
        false
    }

    fun delete(id: Long) {
        try {
            file(id).delete()
        } catch (e: Exception) {
            // sem ação
        }
        prefs.edit().remove("offset:$id").apply()
    }

    /** Marca que a busca online não achou letra, para não repetir a busca à toa. */
    fun notFound(id: Long): Boolean = prefs.contains("nf:$id")

    fun markNotFound(id: Long) {
        prefs.edit().putBoolean("nf:$id", true).apply()
    }

    fun clearNotFound(id: Long) {
        prefs.edit().remove("nf:$id").apply()
    }

    /** Ajuste de tempo da letra, em milissegundos. */
    fun offset(id: Long): Long = prefs.getLong("offset:$id", 0L)

    fun setOffset(id: Long, value: Long) {
        prefs.edit().putLong("offset:$id", value).apply()
    }

    var autoFetch: Boolean
        get() = prefs.getBoolean("auto_fetch", true)
        set(value) {
            prefs.edit().putBoolean("auto_fetch", value).apply()
        }
}
