package br.com.nexj.nexsong.data

import android.content.Context

/** Guarda quais grupos de duplicatas o usuário marcou como "não são duplicatas" e quais músicas ele ocultou. */
class DuplicateStore(context: Context) {

    private val prefs = context.getSharedPreferences("nexsong_duplicates", Context.MODE_PRIVATE)

    private fun read(key: String): Set<String> =
        prefs.getStringSet(key, null)?.toSet() ?: emptySet()

    fun ignoredGroups(): Set<String> = read("ignored")

    fun ignore(groupKey: String) {
        prefs.edit().putStringSet("ignored", read("ignored") + groupKey).apply()
    }

    fun clearIgnored() {
        prefs.edit().remove("ignored").apply()
    }

    fun hiddenIds(): Set<Long> = read("hidden").mapNotNull { it.toLongOrNull() }.toSet()

    fun hide(ids: Collection<Long>) {
        prefs.edit().putStringSet("hidden", read("hidden") + ids.map { it.toString() }).apply()
    }

    fun unhideAll() {
        prefs.edit().remove("hidden").apply()
    }
}
