package id.tinta.app.util

data class SpellIssue(
    val start: Int,
    val end: Int,
    val message: String,
    val suggestion: String? = null,
)

/**
 * Pemeriksa ejaan ringan yang berjalan sepenuhnya di perangkat, tanpa kamus besar dan tanpa
 * jaringan. Fokusnya kesalahan yang paling sering muncul di naskah Indonesia: kata baku, kata
 * depan yang menempel, spasi dan tanda baca, serta nama tokoh dari Story Bible.
 */
object Spellcheck {

    private val corrections: Map<String, String> = buildMap {
        putAll(
            mapOf(
                "merubah" to "mengubah",
                "merobah" to "mengubah",
                "praktek" to "praktik",
                "prakteknya" to "praktiknya",
                "analisa" to "analisis",
                "resiko" to "risiko",
                "obyek" to "objek",
                "jaman" to "zaman",
                "aktifitas" to "aktivitas",
                "kwalitas" to "kualitas",
                "tehnik" to "teknik",
                "sistim" to "sistem",
                "ijin" to "izin",
                "karna" to "karena",
                "silahkan" to "silakan",
                "apotik" to "apotek",
                "standarisasi" to "standardisasi",
            ),
        )
        val places = listOf(
            "sini", "situ", "sana", "atas", "bawah", "dalam", "luar",
            "rumah", "kantor", "sekolah", "pasar", "kota", "desa",
        )
        places.forEach { place ->
            put("di$place", "di $place")
            put("ke$place", "ke $place")
        }
        put("dimana", "di mana")
        put("kemana", "ke mana")
        put("gimana", "bagaimana")
    }

    private val wordPattern = Regex("[\\p{L}][\\p{L}'’-]*")

    fun check(
        text: String,
        names: List<String> = emptyList(),
        dictionary: List<String> = emptyList(),
        limit: Int = 300,
    ): List<SpellIssue> {
        if (text.isBlank()) return emptyList()
        val issues = mutableListOf<SpellIssue>()
        val knownNames = names.map { it.trim() }.filter { it.length >= 3 }.distinct()
        val skipped = dictionary.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

        fun add(start: Int, end: Int, message: String, suggestion: String? = null) {
            if (issues.size >= limit) return
            if (start < 0 || end <= start || end > text.length) return
            issues += SpellIssue(start, end, message, suggestion)
        }

        // Spasi ganda dan spasi sebelum tanda baca.
        Regex(" {2,}").findAll(text).forEach { match ->
            if (match.range.first == 0) return@forEach
            val before = text[match.range.first - 1]
            if (before == '\n' || before == '\t') return@forEach
            add(match.range.first, match.range.last + 1, "Spasi ganda")
        }
        Regex("\\s+([,.;:!?])").findAll(text).forEach { match ->
            add(match.range.first, match.range.last + 1, "Spasi sebelum tanda baca")
        }
        Regex("([,;:])(?=\\p{L})").findAll(text).forEach { match ->
            add(match.range.first, match.range.last + 1, "Tambahkan spasi setelah tanda baca")
        }
        Regex("([!?])\\1{1,}").findAll(text).forEach { match ->
            add(match.range.first, match.range.last + 1, "Tanda baca berulang")
        }

        wordPattern.findAll(text).forEach { match ->
            val token = match.value
            val lower = token.lowercase()
            if (lower in skipped) return@forEach

            val canonicalName = knownNames.firstOrNull { it.equals(token, ignoreCase = true) }
            if (canonicalName != null) {
                if (token != canonicalName) {
                    add(match.range.first, match.range.last + 1, "Ejaan nama tokoh", canonicalName)
                }
                return@forEach
            }

            val suggestion = corrections[lower]
            if (suggestion != null) {
                val shaped = if (token.firstOrNull()?.isUpperCase() == true) {
                    suggestion.replaceFirstChar { it.uppercase() }
                } else {
                    suggestion
                }
                add(match.range.first, match.range.last + 1, "Bentuk baku: $shaped", shaped)
                return@forEach
            }

            if (token.length >= 4 && token.firstOrNull()?.isUpperCase() == true) {
                val near = knownNames.firstOrNull { name ->
                    name.length >= 4 && name.isNotEmpty() &&
                        name.first().lowercase() == token.first().lowercase() &&
                        editDistance(token.lowercase(), name.lowercase()) == 1
                }
                if (near != null) {
                    add(match.range.first, match.range.last + 1, "Mungkin maksudnya $near", near)
                }
            }
        }

        return issues.sortedBy { it.start }
    }

    private fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (kotlin.math.abs(a.length - b.length) > 1) return 2
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            previous = current
        }
        return previous[b.length]
    }
}
