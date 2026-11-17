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

    // Sampul yang sudah terdekode disimpan di memori supaya menggulir tidak memicu decode ulang.
    // sizeOf memakai satuan KB, jadi kapasitasnya juga KB: 24 MB cukup untuk puluhan sampul.
    private const val CACHE_KB = 24 * 1024
    private val cache = object : android.util.LruCache<String, Bitmap>(CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int = maxOf(1, value.byteCount / 1024)
    }

    fun cached(name: String?, max: Int): Bitmap? = name?.let { cache.get("$it@$max") }

    fun bitmap(context: Context, name: String?, max: Int = 720): Bitmap? {
        if (name.isNullOrBlank()) return null
        cached(name, max)?.let { return it }
        val target = file(context, name) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(target.absolutePath, bounds)
        var sample = 1
        while (bounds.outWidth / sample > max || bounds.outHeight / sample > max) sample *= 2
        val decoded = BitmapFactory.decodeFile(
            target.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return null
        cache.put("$name@$max", decoded)
        return decoded
    }

    fun delete(context: Context, name: String?) {
        runCatching {
            if (name != null) {
                cache.snapshot().keys.filter { it.startsWith("$name@") }.forEach(cache::remove)
            }
            file(context, name)?.delete()
        }
    }

    /** Rasio bingkai sampul: 2:3 disarankan, 9:16, atau mengikuti potret foto aslinya. */
    fun aspect(ratio: String, width: Int = 0, height: Int = 0): Float? = when (ratio) {
        "9:16" -> 9f / 16f
        "asli" -> if (width > 0 && height > 0) width.toFloat() / height else null
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
