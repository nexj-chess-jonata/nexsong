package br.com.nexj.nexsong.model

import android.net.Uri

/** Um gênero (pasta). Os padrões têm id fixo; os criados pelo usuário recebem um id próprio. */
data class Genre(val id: String, val label: String) {
    companion object {
        const val OUTROS_ID = "outros"
        val OUTROS = Genre(OUTROS_ID, "Outros")

        val DEFAULTS = listOf(
            Genre("samba", "Samba"),
            Genre("sertanejo", "Sertanejo"),
            Genre("rock", "Rock"),
            Genre("forro", "Forró"),
            Genre("pagode", "Pagode"),
            Genre("mpb", "MPB"),
            Genre("funk", "Funk"),
            Genre("eletronica", "Eletrônica"),
            Genre("hiphop", "Hip-Hop"),
            Genre("gospel", "Gospel"),
            Genre("internacional", "Internacional"),
            OUTROS
        )
    }
}

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val displayName: String,
    val relativePath: String,
    val mimeType: String,
    val tagGenre: String?,
    val genre: Genre = Genre.OUTROS,
    val source: String = "",
    val searchKey: String = "",
    /** false = o gênero é só uma sugestão e aguarda confirmação do usuário. */
    val confirmed: Boolean = true
)
