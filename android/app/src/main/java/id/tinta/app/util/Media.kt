package id.tinta.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import java.io.File
import java.util.UUID

/** Sampul kustom disalin ke penyimpanan privat aplikasi, jadi tetap terbaca tanpa izin galeri. */
object Covers {
    private fun dir(context: Context): File = File(context.filesDir, "sampul").apply { mkdirs() }

    fun save(context: Context, uri: Uri): String? = runCatching {
        val target = File(dir(context), "${UUID.randomUUID()}.img")
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        stream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        if (target.length() == 0L) {
            target.delete()
            null
        } else {
            target.name
        }
    }.getOrNull()

    fun file(context: Context, name: String?): File? {
        if (name.isNullOrBlank()) return null
        return File(dir(context), name).takeIf { it.exists() }
    }

    fun bitmap(context: Context, name: String?, max: Int = 720): Bitmap? {
        val target = file(context, name) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > max || bounds.outHeight / sample > max) sample *= 2
        return BitmapFactory.decodeFile(
            target.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    fun delete(context: Context, name: String?) {
        runCatching { file(context, name)?.delete() }
    }

    /** Rasio bingkai sampul: 2:3 disarankan, 9:16, atau mengikuti potret foto aslinya. */
    fun aspect(ratio: String, bitmap: Bitmap? = null): Float? = when (ratio) {
        "9:16" -> 9f / 16f
        "asli" -> bitmap?.let { if (it.height > 0) it.width.toFloat() / it.height else null }
        else -> 2f / 3f
    }

    fun label(ratio: String): String = when (ratio) {
        "9:16" -> "9:16"
        "asli" -> "Potret asli"
        else -> "2:3"
    }
}

/** Font milik pengguna: berkas TTF/OTF disalin ke aplikasi lalu dipakai editor. */
object Fonts {
    private fun dir(context: Context): File = File(context.filesDir, "font").apply { mkdirs() }

    fun install(context: Context, uri: Uri): String? = runCatching {
        val target = File(dir(context), "${UUID.randomUUID()}.font")
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        stream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
        Typeface.createFromFile(target)
        target.name
    }.getOrNull()

    fun file(context: Context, name: String?): File? {
        if (name.isNullOrBlank()) return null
        return File(dir(context), name).takeIf { it.exists() }
    }
}
