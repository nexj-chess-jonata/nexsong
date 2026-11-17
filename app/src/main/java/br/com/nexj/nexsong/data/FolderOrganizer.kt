package br.com.nexj.nexsong.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import br.com.nexj.nexsong.model.Genre
import br.com.nexj.nexsong.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Organiza as músicas em pastas reais.
 *  - Mover: Music/NexSong/<Gênero>/ (com registro para desfazer).
 *  - Copiar: Music/NexSong Cópias/<Gênero>/ (os originais não são alterados).
 */
class FolderOrganizer(private val context: Context) {

    data class CopyReport(val copied: Int, val alreadyThere: Int, val failed: Int)
    data class MoveReport(val moved: Int, val failed: Int, val entries: List<MoveLog.Entry>)
    data class RestoreReport(val restored: Int, val failed: Int, val doneIds: List<Long>)

    private fun safeName(label: String): String {
        val cleaned = label.replace(Regex("[\\\\/:*?\"<>|]"), " ").trim()
        return cleaned.ifEmpty { "Sem nome" }
    }

    fun targetPath(genre: Genre): String = "Music/NexSong/${safeName(genre.label)}/"

    private fun copyPath(genre: Genre): String = "$COPIES_ROOT${safeName(genre.label)}/"

    fun audioUri(id: Long): Uri = ContentUris.withAppendedId(
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
        id
    )

    /** Pede ao Android a permissão do usuário para alterar esses arquivos (Android 11+). */
    @RequiresApi(Build.VERSION_CODES.R)
    fun writeRequest(uris: List<Uri>): IntentSender =
        MediaStore.createWriteRequest(context.contentResolver, uris).intentSender

    /** Pede ao Android a confirmação do usuário para apagar esses arquivos (Android 11+). */
    @RequiresApi(Build.VERSION_CODES.R)
    fun deleteRequest(uris: List<Uri>): IntentSender =
        MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender

    suspend fun copy(
        songs: List<Song>,
        onProgress: (done: Int, total: Int) -> Unit
    ): CopyReport = withContext(Dispatchers.IO) {
        var copied = 0
        var already = 0
        var failed = 0
        songs.forEachIndexed { index, song ->
            when (copyOne(song)) {
                Outcome.COPIED -> copied++
                Outcome.EXISTS -> already++
                Outcome.FAILED -> failed++
            }
            onProgress(index + 1, songs.size)
        }
        CopyReport(copied, already, failed)
    }

    suspend fun move(
        songs: List<Song>,
        onProgress: (done: Int, total: Int) -> Unit
    ): MoveReport = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var moved = 0
        var failed = 0
        val entries = ArrayList<MoveLog.Entry>()
        songs.forEachIndexed { index, song ->
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, targetPath(song.genre))
                }
                val rows = resolver.update(song.uri, values, null, null)
                if (rows > 0) {
                    moved++
                    entries.add(MoveLog.Entry(song.id, song.relativePath))
                } else {
                    failed++
                }
            } catch (e: Exception) {
                failed++
            }
            onProgress(index + 1, songs.size)
        }
        MoveReport(moved, failed, entries)
    }

    suspend fun restore(
        entries: List<MoveLog.Entry>,
        onProgress: (done: Int, total: Int) -> Unit
    ): RestoreReport = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var restored = 0
        var failed = 0
        val doneIds = ArrayList<Long>()
        entries.forEachIndexed { index, entry ->
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, entry.originalPath)
                }
                val rows = resolver.update(audioUri(entry.id), values, null, null)
                if (rows > 0) {
                    restored++
                    doneIds.add(entry.id)
                } else {
                    failed++
                }
            } catch (e: Exception) {
                failed++
            }
            onProgress(index + 1, entries.size)
        }
        RestoreReport(restored, failed, doneIds)
    }

    private enum class Outcome { COPIED, EXISTS, FAILED }

    private fun copyOne(song: Song): Outcome {
        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relativePath = copyPath(song.genre)
        val name = song.displayName.ifBlank { "${song.title}.mp3" }

        return try {
            val exists = resolver.query(
                collection,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND ${MediaStore.Audio.Media.RELATIVE_PATH} = ?",
                arrayOf(name, relativePath),
                null
            )?.use { it.count > 0 } ?: false
            if (exists) return Outcome.EXISTS

            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, song.mimeType.ifBlank { "audio/mpeg" })
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val target = resolver.insert(collection, values) ?: return Outcome.FAILED
            try {
                val input = resolver.openInputStream(song.uri)
                    ?: throw IllegalStateException("sem origem")
                input.use { src ->
                    val output = resolver.openOutputStream(target)
                        ?: throw IllegalStateException("sem destino")
                    output.use { dst -> src.copyTo(dst) }
                }
                val done = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
                resolver.update(target, done, null, null)
                Outcome.COPIED
            } catch (e: Exception) {
                resolver.delete(target, null, null)
                Outcome.FAILED
            }
        } catch (e: Exception) {
            Outcome.FAILED
        }
    }

    companion object {
        const val COPIES_ROOT = "Music/NexSong Cópias/"
    }
}
