package id.tinta.app.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Menampilkan Markdown langsung di dalam editor: penanda inline (`**`, `*`, `_`, `~~`, `` ` ``)
 * maupun penanda baris (`##`, `-`, `*`, `+`) disembunyikan, isinya digambar sesuai formatnya.
 * Seluruh penafsiran penanda diserahkan ke [MarkdownDocument] agar tidak ada dua parser.
 */
class MarkdownInlineTransformation : VisualTransformation {
    private var cachedSource: String? = null
    private var cached: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        if (cachedSource == source) cached?.let { return it }

        val document = MarkdownDocument.parse(source)
        val out = StringBuilder(source.length)
        val map = IntArray(source.length + 1)
        var pointer = 0

        for (index in source.indices) {
            while (pointer < document.hidden.size && document.hidden[pointer].last < index) pointer += 1
            map[index] = out.length
            val range = document.hidden.getOrNull(pointer)
            if (range != null && index >= range.first) continue
            if (index in document.bullets) out.append('•') else out.append(source[index])
        }
        map[source.length] = out.length

        val spans = document.spans.map {
            AnnotatedString.Range(it.item, map[it.start], map[it.end])
        }

        // Kursor yang jatuh di posisi penanda baris diarahkan ke awal isi baris, supaya mengetik
        // dan Backspace tidak menyelipkan atau memotong penanda yang tersembunyi.
        val preferred = IntArray(source.length + 1) { -1 }
        document.headings.forEach { heading ->
            val at = map[heading.contentStart]
            if (at in preferred.indices) preferred[at] = heading.contentStart
        }
        document.bullets.forEach { bullet ->
            val content = (bullet + 2).coerceAtMost(source.length)
            val bulletAt = map[bullet]
            if (bulletAt in preferred.indices) preferred[bulletAt] = content
            if (bullet + 1 <= source.length) {
                val spaceAt = map[bullet + 1]
                if (spaceAt in preferred.indices) preferred[spaceAt] = content
            }
        }

        val transformed = TransformedText(
            AnnotatedString(out.toString(), spans),
            Mapping(map, preferred, out.length),
        )
        cachedSource = source
        cached = transformed
        return transformed
    }

    private class Mapping(
        private val map: IntArray,
        private val preferred: IntArray,
        private val visibleLength: Int,
    ) : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int =
            map[offset.coerceIn(0, map.size - 1)]

        override fun transformedToOriginal(offset: Int): Int {
            val target = offset.coerceIn(0, visibleLength)
            val forced = preferred[target]
            if (forced >= 0) return forced
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
