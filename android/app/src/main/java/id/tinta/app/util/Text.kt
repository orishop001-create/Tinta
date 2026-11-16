package id.tinta.app.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object Text {
    private val localeId = Locale("id", "ID")

    fun wordCount(text: String): Int {
        if (text.isBlank()) return 0
        return text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
    }

    fun readingMinutes(words: Int): Int = maxOf(1, (words + 199) / 200)

    fun dayKey(time: Long = System.currentTimeMillis()): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun shortDate(time: Long): String =
        SimpleDateFormat("d MMM yyyy", localeId).format(Date(time))

    fun timeOnly(time: Long): String =
        SimpleDateFormat("HH.mm", localeId).format(Date(time))

    fun relative(time: Long): String {
        val diff = System.currentTimeMillis() - time
        if (diff < 60_000) return "baru saja"
        if (diff < 3_600_000) return "${diff / 60_000} menit lalu"
        if (diff < 86_400_000) return "${diff / 3_600_000} jam lalu"
        if (diff < 172_800_000) return "kemarin"
        if (diff < 604_800_000) return "${diff / 86_400_000} hari lalu"
        return shortDate(time)
    }

    fun snippet(body: String, max: Int = 150): String {
        val flat = body.replace(Regex("\\s+"), " ").trim()
        return if (flat.length <= max) flat else flat.take(max).trimEnd() + "…"
    }

    fun slugify(value: String): String {
        val slug = value.lowercase(localeId)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return slug.ifBlank { "naskah" }
    }

    fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
