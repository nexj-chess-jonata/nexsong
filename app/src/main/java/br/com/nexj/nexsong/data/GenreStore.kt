package br.com.nexj.nexsong.data

import android.content.Context
import br.com.nexj.nexsong.classify.GenreClassifier
import br.com.nexj.nexsong.model.Genre

/** Lista de gêneros do usuário: os padrões mais os que ele criar, renomear ou apagar. */
class GenreStore(context: Context) {

    private val prefs = context.getSharedPreferences("nexsong_genres", Context.MODE_PRIVATE)

    fun all(): List<Genre> {
        val raw = prefs.getString("list", null) ?: return Genre.DEFAULTS
        val parsed = raw.split('\n').mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                Genre(parts[0], parts[1])
            } else {
                null
            }
        }.filter { it.id != Genre.OUTROS_ID }
        // "Outros" sempre existe e fica por último.
        return parsed + Genre.OUTROS
    }

    private fun save(list: List<Genre>) {
        val text = list.filter { it.id != Genre.OUTROS_ID }
            .joinToString("\n") { "${it.id}\t${it.label}" }
        prefs.edit().putString("list", text).apply()
    }

    private fun clean(label: String): String =
        label.replace('\t', ' ').replace('\n', ' ').trim()

    private fun labelTaken(list: List<Genre>, label: String, exceptId: String?): Boolean {
        val key = GenreClassifier.normalize(label)
        return list.any { it.id != exceptId && GenreClassifier.normalize(it.label) == key }
    }

    /** Cria um gênero. Devolve false se o nome for vazio ou já existir. */
    fun add(label: String): Boolean {
        val name = clean(label)
        val list = all()
        if (name.isEmpty() || GenreClassifier.normalize(name).isEmpty()) return false
        if (labelTaken(list, name, null)) return false
        val base = "c_" + GenreClassifier.normalize(name).replace(' ', '_')
        var id = base
        var n = 2
        while (list.any { it.id == id }) {
            id = "${base}_$n"
            n++
        }
        save(list + Genre(id, name))
        return true
    }

    fun rename(id: String, label: String): Boolean {
        val name = clean(label)
        val list = all()
        if (id == Genre.OUTROS_ID || name.isEmpty()) return false
        if (labelTaken(list, name, id)) return false
        save(list.map { if (it.id == id) Genre(id, name) else it })
        return true
    }

    fun delete(id: String) {
        if (id == Genre.OUTROS_ID) return
        save(all().filter { it.id != id })
    }
}
