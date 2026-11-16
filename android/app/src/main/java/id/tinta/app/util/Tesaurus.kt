package id.tinta.app.util

import android.content.Context
import org.json.JSONObject

/**
 * Tesaurus kecil yang dibundel di aplikasi, ditulis ulang untuk Tinta dari daftar
 * padanan kata umum bahasa Indonesia. Sepenuhnya offline dan gratis.
 */
object Tesaurus {
    @Volatile
    private var cache: Map<String, List<String>>? = null

    fun synonyms(context: Context, word: String): List<String> {
        val key = normalize(word)
        if (key.isEmpty()) return emptyList()
        val loaded = cache ?: load(context).also { cache = it }
        return loaded[key].orEmpty()
    }

    fun normalize(word: String): String = word
        .trim()
        .trim('“', '”', '"', '\'', '(', ')', ',', '.', '!', '?', ';', ':', '—', '–')
        .lowercase()

    private fun load(context: Context): Map<String, List<String>> = runCatching {
        val raw = context.assets.open("tesaurus.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val out = HashMap<String, List<String>>(root.length())
        root.keys().forEach { key ->
            val array = root.optJSONArray(key) ?: return@forEach
            val values = (0 until array.length())
                .map { index -> array.optString(index) }
                .filter { it.isNotBlank() }
            if (values.isNotEmpty()) out[key.lowercase()] = values
        }
        out
    }.getOrDefault(emptyMap())
}
