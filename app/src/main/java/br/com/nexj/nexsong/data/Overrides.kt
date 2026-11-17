package br.com.nexj.nexsong.data

import android.content.Context

/** Guarda as correções de gênero feitas pelo usuário (por música e por artista), pelo id do gênero. */
class Overrides(context: Context) {

    private val prefs = context.getSharedPreferences("nexsong_overrides", Context.MODE_PRIVATE)

    fun forSong(id: Long): String? = prefs.getString("song:$id", null)?.lowercase()

    fun forArtist(normalizedArtist: String): String? {
        if (normalizedArtist.isEmpty()) return null
        return prefs.getString("artist:$normalizedArtist", null)?.lowercase()
    }

    fun setSong(id: Long, genreId: String) {
        prefs.edit().putString("song:$id", genreId).apply()
    }

    fun setSongs(ids: List<Long>, genreIds: List<String>) {
        val editor = prefs.edit()
        for (i in ids.indices) {
            editor.putString("song:${ids[i]}", genreIds[i])
        }
        editor.apply()
    }

    fun setArtist(normalizedArtist: String, genreId: String) {
        if (normalizedArtist.isEmpty()) return
        prefs.edit().putString("artist:$normalizedArtist", genreId).apply()
    }
}
