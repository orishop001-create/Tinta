package id.tinta.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

/**
 * Menampilkan Markdown inline langsung di dalam editor. Penanda `**`, `*`, `_`, `~~`, dan `` ` ``
 * disembunyikan, isinya digambar tebal/miring/coret, jadi pengguna tidak perlu membuka pratinjau
 * hanya untuk melihat formatnya.
 */
class MarkdownInlineTransformation : VisualTransformation {
    private var cachedSource: String? = null
    private var cached: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        if (cachedSource == source) cached?.let { return it }

        val map = IntArray(source.length + 1)
        val out = StringBuilder(source.length)
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
        val open = ArrayDeque<Pair<String, Int>>()
        var index = 0

        while (index < source.length) {
            val marker = markerAt(source, index)
            if (marker == null) {
                map[index] = out.length
                out.append(source[index])
                index += 1
                map[index] = out.length
                continue
            }
            for (step in 0 until marker.length) {
                if (index + step < source.length) map[index + step] = out.length
            }
            index += marker.length
            map[index] = out.length

            val top = open.lastOrNull()
            if (top != null && top.first == marker) {
                open.removeLast()
                if (out.length > top.second) {
                    spans.add(AnnotatedString.Range(styleOf(marker), top.second, out.length))
                }
            } else {
                open.addLast(marker to out.length)
            }
        }

        val transformed = TransformedText(
            AnnotatedString(out.toString(), spans),
            Mapping(map, out.length),
        )
        cachedSource = source
        cached = transformed
        return transformed
    }

    private class Mapping(private val map: IntArray, private val visibleLength: Int) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            map[offset.coerceIn(0, map.size - 1)]

        override fun transformedToOriginal(offset: Int): Int {
            val target = offset.coerceIn(0, visibleLength)
            var low = 0
            var high = map.size - 1
            while (low < high) {
                val mid = (low + high) / 2
                if (map[mid] < target) low = mid + 1 else high = mid
            }
            return low
        }
    }
}

private fun styleOf(marker: String): SpanStyle = when (marker) {
    "**" -> SpanStyle(fontWeight = FontWeight.Bold)
    "~~" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    "`" -> SpanStyle(fontFamily = FontFamily.Monospace)
    else -> SpanStyle(fontStyle = FontStyle.Italic)
}

/** Penanda hanya dianggap format kalau menempel pada kata, sehingga "* * *" tetap terlihat. */
internal fun markerAt(text: String, index: Int): String? {
    for (marker in listOf("**", "~~", "`", "*", "_")) {
        if (!text.startsWith(marker, index)) continue
        if (marker == "**" && text.startsWith("***", index)) continue
        if (marker == "`") return marker
        val next = text.getOrNull(index + marker.length)
        val prev = text.getOrNull(index - 1)
        val nextOk = next != null && !next.isWhitespace()
        val prevOk = prev != null && !prev.isWhitespace()
        if (nextOk || prevOk) return marker
    }
    return null
}

private data class Wrap(val contentStart: Int, val contentEnd: Int, val closeEnd: Int)

private fun wrappingAt(text: String, caret: Int, marker: String): Wrap? {
    var index = 0
    var open = -1
    while (index < text.length) {
        val found = markerAt(text, index)
        if (found == null) {
            index += 1
            continue
        }
        if (found == marker) {
            if (open < 0) {
                open = index
            } else {
                val contentStart = open + marker.length
                if (caret in contentStart..index) {
                    return Wrap(contentStart, index, index + marker.length)
                }
                open = -1
            }
        }
        index += found.length
    }
    return null
}

/**
 * Sakelar format ala pengolah kata: menekan B pada teks terpilih membungkusnya, menekan B pada
 * kursor kosong menyiapkan pasangan penanda sehingga ketikan berikutnya langsung tebal/miring,
 * dan menekan B lagi di dalam format melepasnya.
 */
fun toggleInline(value: TextFieldValue, marker: String): TextFieldValue {
    val text = value.text
    val start = minOf(value.selection.start, value.selection.end).coerceIn(0, text.length)
    val end = maxOf(value.selection.start, value.selection.end).coerceIn(0, text.length)

    if (end > start) {
        val selected = text.substring(start, end)
        val inside = wrappingAt(text, start, marker)
        if (inside != null && start >= inside.contentStart && end <= inside.contentEnd) {
            // Seleksi sudah berformat: lepas penandanya, jangan bungkus dua kali.
            val openAt = inside.contentStart - marker.length
            val cut = text
                .removeRange(inside.closeEnd - marker.length, inside.closeEnd)
                .removeRange(openAt, openAt + marker.length)
            return TextFieldValue(
                text = cut,
                selection = TextRange(
                    (start - marker.length).coerceIn(0, cut.length),
                    (end - marker.length).coerceIn(0, cut.length),
                ),
            )
        }
        val alreadyWrapped = selected.length >= marker.length * 2 &&
            selected.startsWith(marker) && selected.endsWith(marker)
        return if (alreadyWrapped) {
            val inner = selected.substring(marker.length, selected.length - marker.length)
            TextFieldValue(
                text = text.replaceRange(start, end, inner),
                selection = TextRange(start, start + inner.length),
            )
        } else {
            TextFieldValue(
                text = text.replaceRange(start, end, marker + selected + marker),
                selection = TextRange(start + marker.length, end + marker.length),
            )
        }
    }

    val wrapping = wrappingAt(text, start, marker)
    if (wrapping != null) {
        val openAt = wrapping.contentStart - marker.length
        return if (wrapping.contentStart == wrapping.contentEnd) {
            val cut = text.removeRange(openAt, wrapping.closeEnd)
            TextFieldValue(text = cut, selection = TextRange(openAt.coerceIn(0, cut.length)))
        } else {
            TextFieldValue(text = text, selection = TextRange(wrapping.closeEnd.coerceIn(0, text.length)))
        }
    }

    return TextFieldValue(
        text = text.replaceRange(start, start, marker + marker),
        selection = TextRange(start + marker.length),
    )
}
