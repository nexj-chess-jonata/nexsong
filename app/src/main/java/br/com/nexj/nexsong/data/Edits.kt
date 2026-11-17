package br.com.nexj.nexsong.data

import android.content.Context

/** Guarda as edições de título, artista e álbum feitas pelo usuário. O arquivo de música não é alterado. */
class Edits(context: Context) {

    private val prefs = context.getSharedPreferences("nexsong_edits", Context.MODE_PRIVATE)

    fun title(id: Long): String? = prefs.getString("title:$id", null)

    fun artist(id: Long): String? = prefs.getString("artist:$id", null)

    fun album(id: Long): String? = prefs.getString("album:$id", null)

    fun has(id: Long): Boolean =
        prefs.contains("title:$id") || prefs.contains("artist:$id") || prefs.contains("album:$id")

    fun set(id: Long, title: String?, artist: String?, album: String?) {
        val editor = prefs.edit()
        if (title != null) editor.putString("title:$id", title)
        if (artist != null) editor.putString("artist:$id", artist)
        if (album != null) editor.putString("album:$id", album)
        editor.apply()
    }

    fun clear(id: Long) {
        prefs.edit()
            .remove("title:$id")
            .remove("artist:$id")
            .remove("album:$id")
            .apply()
    }
}
