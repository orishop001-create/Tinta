package id.tinta.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/** Satu heading di dalam naskah, dipakai panel kerangka dan daftar isi. */
data class MarkdownHeading(val level: Int, val contentStart: Int, val title: String)

/**
 * Model tunggal untuk penanda Markdown. Semua bagian editor membaca penanda dari sini:
 * transformasi tampilan, panel kerangka, pratinjau berhalaman, dan penjaga Backspace.
 * Penanda tetap disimpan sebagai teks agar naskah portabel, tetapi hanya model ini yang
 * boleh menafsirkannya sehingga aturan penyembunyian tidak bercabang di banyak tempat.
 */
class MarkdownDocument private constructor(
    val source: String,
    /** Rentang karakter penanda yang disembunyikan, terurut dan tidak bertumpuk. */
    val hidden: List<IntRange>,
    /** Indeks tanda butir yang digambar sebagai `•`. */
    val bullets: Set<Int>,
    /** Rentang gaya dalam koordinat teks asli. */
    val spans: List<AnnotatedString.Range<SpanStyle>>,
    val headings: List<MarkdownHeading>,
) {
    val visible: String by lazy(LazyThreadSafetyMode.NONE) {
        val out = StringBuilder(source.length)
        var pointer = 0
        for (index in source.indices) {
            while (pointer < hidden.size && hidden[pointer].last < index) pointer += 1
            if (pointer < hidden.size && index >= hidden[pointer].first) continue
            if (index in bullets) out.append('•') else out.append(source[index])
        }
        out.toString()
    }

    fun outline(): List<MarkdownHeading> = headings.map { heading ->
        val end = lineEnd(heading.contentStart)
        heading.copy(title = stripMarkers(source.substring(heading.contentStart, end)).trim())
    }

    /** Paragraf tanpa penanda, siap dipotong menjadi halaman baca. */
    fun plainParagraphs(): List<String> = visible
        .split(Regex("\n\\s*\n"))
        .map { it.replace('\n', ' ').trim() }
        .filter { it.isNotEmpty() }

    private fun lineEnd(from: Int): Int {
        val found = source.indexOf('\n', from)
        return if (found < 0) source.length else found
    }

    companion object {
        fun parse(source: String): MarkdownDocument {
            val hidden = mutableListOf<IntRange>()
            val bullets = mutableSetOf<Int>()
            val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
            val headings = mutableListOf<MarkdownHeading>()
            val open = ArrayDeque<Pair<String, Int>>()
            var index = 0
            var lineStart = true

            while (index < source.length) {
                if (lineStart) {
                    lineStart = false
                    val heading = headingLength(source, index)
                    if (heading > 0) {
                        hidden.add(index until index + heading)
                        headings.add(MarkdownHeading(heading - 1, index + heading, ""))
                        index += heading
                        continue
                    }
                    if (bulletAt(source, index)) {
                        hidden.add(index until index + 1)
                        bullets.add(index)
                        index += 1
                        continue
                    }
                }

                val marker = markerAt(source, index)
                if (marker == null) {
                    if (source[index] == '\n') lineStart = true
                    index += 1
                    continue
                }

                hidden.add(index until index + marker.length)
                index += marker.length
                val top = open.lastOrNull()
                if (top != null && top.first == marker) {
                    open.removeLast()
                    if (index > top.second) {
                        spans.add(AnnotatedString.Range(styleOf(marker), top.second, index))
                    }
                } else {
                    open.addLast(marker to index)
                }
            }

            headings.forEach { heading ->
                val end = source.indexOf('\n', heading.contentStart).let {
                    if (it < 0) source.length else it
                }
                if (end > heading.contentStart) {
                    spans.add(AnnotatedString.Range(HeadingStyle, heading.contentStart, end))
                }
            }

            return MarkdownDocument(source, hidden, bullets, spans, headings)
        }
    }
}

internal val HeadingStyle = SpanStyle(fontWeight = FontWeight.Bold)

internal fun styleOf(marker: String): SpanStyle = when (marker) {
    "**" -> SpanStyle(fontWeight = FontWeight.Bold)
    "~~" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    "`" -> SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    else -> SpanStyle(fontStyle = FontStyle.Italic)
}

/** `## ` di awal baris: jumlah pagar + satu spasi yang ikut disembunyikan. */
internal fun headingLength(text: String, index: Int): Int {
    var cursor = index
    while (cursor < text.length && text[cursor] == '#') cursor += 1
    val count = cursor - index
    if (count in 1..6 && cursor < text.length && text[cursor] == ' ') return count + 1
    return 0
}

/** `- `, `* `, `+ ` di awal baris digambar sebagai butir, bukan tanda minus. */
internal fun bulletAt(text: String, index: Int): Boolean {
    val char = text[index]
    if (char != '-' && char != '*' && char != '+') return false
    return index + 1 < text.length && text[index + 1] == ' '
}

/**
 * Penanda inline tanpa alokasi list per karakter. Penanda hanya sah bila menempel pada kata,
 * sehingga `* * *` tetap tampil apa adanya.
 */
internal fun markerAt(text: String, index: Int): String? {
    when (text[index]) {
        '*' -> {
            if (index + 1 < text.length && text[index + 1] == '*') {
                return if (attached(text, index, 2) && !text.startsWith("***", index)) "**" else null
            }
            return if (attached(text, index, 1)) "*" else null
        }
        '~' -> {
            return if (index + 1 < text.length && text[index + 1] == '~' && attached(text, index, 2)) {
                "~~"
            } else {
                null
            }
        }
        '`' -> return "`"
        '_' -> return if (attached(text, index, 1)) "_" else null
    }
    return null
}

private fun attached(text: String, index: Int, length: Int): Boolean {
    val next = text.getOrNull(index + length)
    val prev = text.getOrNull(index - 1)
    return (next != null && !next.isWhitespace()) || (prev != null && !prev.isWhitespace())
}

/** Membuang penanda inline dari potongan teks, dipakai judul kerangka dan impor. */
internal fun stripMarkers(text: String): String {
    val out = StringBuilder(text.length)
    var index = 0
    while (index < text.length) {
        val marker = markerAt(text, index)
        if (marker != null) {
            index += marker.length
            continue
        }
        out.append(text[index])
        index += 1
    }
    return out.toString()
}

/**
 * Penjaga Backspace: menghapus satu karakter tepat di spasi penanda baris tidak boleh
 * menyisakan `##` atau `-` yatim. Tindakan itu diubah menjadi melepas format baris.
 */
fun protectLineMarkers(old: androidx.compose.ui.text.input.TextFieldValue, new: androidx.compose.ui.text.input.TextFieldValue): androidx.compose.ui.text.input.TextFieldValue {
    if (old.text == new.text) return new
    if (!new.selection.collapsed) return new
    if (old.text.length - new.text.length != 1) return new
    val cut = new.selection.start
    if (cut < 0 || cut >= old.text.length) return new
    if (old.text.removeRange(cut, cut + 1) != new.text) return new

    val lineStart = old.text.lastIndexOf('\n', (cut - 1).coerceAtLeast(0)).let {
        if (it < 0) 0 else it + 1
    }
    val heading = headingLength(old.text, lineStart)
    if (heading > 0 && cut == lineStart + heading - 1) {
        // Spasi sudah hilang dari `new.text`, jadi yang dibuang hanya pagar `#`-nya saja,
        // bukan sepanjang penanda lama (itu akan ikut memakan huruf pertama judul).
        val markers = heading - 1
        val text = new.text.removeRange(lineStart, (lineStart + markers).coerceAtMost(new.text.length))
        return androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(lineStart.coerceIn(0, text.length)))
    }
    val marker = old.text.getOrNull(lineStart)
    val isBullet = (marker == '-' || marker == '*' || marker == '+') && old.text.getOrNull(lineStart + 1) == ' '
    if (isBullet && (cut == lineStart || cut == lineStart + 1)) {
        // Satu karakter penanda butir saja; spasinya sudah terhapus atau menyusul dibuang.
        val text = new.text.removeRange(lineStart, (lineStart + 1).coerceAtMost(new.text.length))
        return androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(lineStart.coerceIn(0, text.length)))
    }
    return new
}
