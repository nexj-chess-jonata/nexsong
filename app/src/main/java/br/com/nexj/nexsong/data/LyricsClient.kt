package br.com.nexj.nexsong.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Busca letras no LRCLIB (https://lrclib.net), serviço gratuito e sem chave.
 * Deve ser chamado fora da thread principal.
 */
class LyricsClient {

    sealed class Result {
        data class Found(val text: String) : Result()
        object NotFound : Result()
        object Error : Result()
    }

    private class Response(val code: Int, val body: String)

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun request(address: String): Response {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            val body = if (code == 200) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                ""
            }
            return Response(code, body)
        } finally {
            connection.disconnect()
        }
    }

    private fun text(obj: JSONObject, key: String): String =
        if (obj.isNull(key)) "" else obj.optString(key, "")

    /** Prefere a letra sincronizada; se não houver, a letra simples. */
    private fun pickText(obj: JSONObject): String? {
        val synced = text(obj, "syncedLyrics")
        if (synced.isNotBlank()) return synced
        val plain = text(obj, "plainLyrics")
        if (plain.isNotBlank()) return plain
        return null
    }

    fun fetch(title: String, artist: String, album: String, durationMs: Long): Result {
        if (title.isBlank()) return Result.NotFound
        val seconds = durationMs / 1000

        try {
            // 1) Busca exata por título, artista, álbum e duração.
            if (artist.isNotBlank()) {
                val params = StringBuilder()
                params.append("track_name=").append(enc(title))
                params.append("&artist_name=").append(enc(artist))
                if (album.isNotBlank()) params.append("&album_name=").append(enc(album))
                if (seconds > 0) params.append("&duration=").append(seconds)
                val exact = request("$BASE/get?$params")
                if (exact.code == 200) {
                    val found = pickText(JSONObject(exact.body))
                    if (found != null) return Result.Found(found)
                }
            }

            // 2) Busca aberta, escolhendo o resultado com duração mais próxima.
            val params = StringBuilder()
            params.append("track_name=").append(enc(title))
            if (artist.isNotBlank()) params.append("&artist_name=").append(enc(artist))
            val search = request("$BASE/search?$params")
            if (search.code == 404) return Result.NotFound
            if (search.code != 200) return Result.Error

            val array = JSONArray(search.body)
            var bestText: String? = null
            var bestScore = Double.MAX_VALUE
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val candidate = pickText(obj) ?: continue
                val duration = obj.optDouble("duration", 0.0)
                val diff = if (seconds > 0 && duration > 0.0) abs(duration - seconds) else 0.0
                if (diff > MAX_DURATION_DIFF_SECONDS) continue
                // Letra sincronizada vale mais que letra simples.
                val synced = text(obj, "syncedLyrics").isNotBlank()
                val score = diff + if (synced) 0.0 else 100.0
                if (score < bestScore) {
                    bestScore = score
                    bestText = candidate
                }
            }
            return if (bestText != null) Result.Found(bestText) else Result.NotFound
        } catch (e: Exception) {
            return Result.Error
        }
    }

    companion object {
        private const val BASE = "https://lrclib.net/api"
        private const val USER_AGENT = "NexSong/1.0 (Android; app de organizacao de musicas)"
        private const val MAX_DURATION_DIFF_SECONDS = 5.0
    }
}
