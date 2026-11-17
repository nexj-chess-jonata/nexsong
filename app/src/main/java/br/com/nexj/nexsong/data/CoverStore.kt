package br.com.nexj.nexsong.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import java.io.File
import java.io.FileOutputStream

/**
 * Capas das músicas. Usa a capa escolhida pelo usuário (guardada no app) ou,
 * se não houver, a capa embutida no arquivo.
 */
class CoverStore(private val context: Context) {

    private val dir = File(context.filesDir, "covers")

    private fun file(id: Long) = File(dir, "$id.jpg")

    fun hasCustom(id: Long): Boolean = file(id).exists()

    fun load(id: Long): Bitmap? {
        val custom = file(id)
        if (custom.exists()) {
            val bitmap = BitmapFactory.decodeFile(custom.path)
            if (bitmap != null) return bitmap
        }
        return try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                id
            )
            context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
        } catch (e: Exception) {
            null
        }
    }

    fun save(id: Long, source: Uri): Boolean {
        try {
            val original = context.contentResolver.openInputStream(source)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return false
            val maxSide = maxOf(original.width, original.height)
            val scaled = if (maxSide > 800) {
                val scale = 800f / maxSide
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }
            dir.mkdirs()
            FileOutputStream(file(id)).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun remove(id: Long) {
        try {
            file(id).delete()
        } catch (e: Exception) {
            // sem ação
        }
    }
}
