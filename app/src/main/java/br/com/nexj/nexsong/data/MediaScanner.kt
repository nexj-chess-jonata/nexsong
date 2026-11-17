package br.com.nexj.nexsong.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import br.com.nexj.nexsong.model.Song

/** Lê todas as músicas do aparelho pelo MediaStore (offline). */
class MediaScanner(private val context: Context) {

    fun scan(): List<Song> {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val columns = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.MIME_TYPE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            columns.add(MediaStore.Audio.Media.GENRE)
        }

        val genreByAudioId: Map<Long, String> =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) loadGenreMap() else emptyMap()

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 30000"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val result = ArrayList<Song>()
        resolver.query(collection, columns.toTypedArray(), selection, null, sort)?.use { c ->
            val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val iPath = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val iMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val iGenre = c.getColumnIndex(MediaStore.Audio.Media.GENRE)

            while (c.moveToNext()) {
                val id = c.getLong(iId)
                val relativePath = c.getString(iPath).orEmpty()
                // Cópias criadas pelo próprio app não entram na biblioteca.
                if (relativePath.startsWith(FolderOrganizer.COPIES_ROOT)) continue

                val displayName = c.getString(iName).orEmpty()
                var title = c.getString(iTitle).orEmpty()
                var artist = c.getString(iArtist).orEmpty()
                if (artist == MediaStore.UNKNOWN_STRING) artist = ""

                // Sem artista na tag: tenta "Artista - Título" no nome do arquivo.
                if (artist.isBlank()) {
                    val base = displayName.substringBeforeLast('.')
                    if (base.contains(" - ")) {
                        artist = base.substringBefore(" - ").trim()
                        title = base.substringAfter(" - ").trim().ifBlank { title }
                    }
                }
                if (title.isBlank()) title = displayName.substringBeforeLast('.')

                val tagGenre = when {
                    iGenre >= 0 -> c.getString(iGenre)
                    else -> genreByAudioId[id]
                }

                result.add(
                    Song(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        title = title,
                        artist = artist,
                        album = c.getString(iAlbum).orEmpty(),
                        durationMs = c.getLong(iDuration),
                        displayName = displayName,
                        relativePath = relativePath,
                        mimeType = c.getString(iMime).orEmpty(),
                        tagGenre = tagGenre
                    )
                )
            }
        }
        return result
    }

    /** Android 10: o gênero fica numa tabela separada. */
    private fun loadGenreMap(): Map<Long, String> {
        val map = HashMap<Long, String>()
        val resolver = context.contentResolver
        val genres = MediaStore.Audio.Genres.getContentUri(MediaStore.VOLUME_EXTERNAL)
        resolver.query(
            genres,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null, null, null
        )?.use { g ->
            while (g.moveToNext()) {
                val genreId = g.getLong(0)
                val name = g.getString(1).orEmpty()
                val members = MediaStore.Audio.Genres.Members.getContentUri(
                    MediaStore.VOLUME_EXTERNAL, genreId
                )
                resolver.query(
                    members,
                    arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                    null, null, null
                )?.use { m ->
                    while (m.moveToNext()) {
                        map[m.getLong(0)] = name
                    }
                }
            }
        }
        return map
    }
}
