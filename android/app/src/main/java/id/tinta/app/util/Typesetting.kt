package id.tinta.app.util

import kotlin.math.max

/** Pengukur lebar teks. Di Android diisi Paint, di unit test diisi pengukur sederhana. */
fun interface TextMetrics {
    fun width(text: String, size: Float, bold: Boolean): Float
}

/** Ukuran halaman A4 (poin) dengan margin buku cetak standar 2,5 cm. */
data class PrintFormat(
    val pageWidth: Float = 595f,
    val pageHeight: Float = 842f,
    val marginTop: Float = 78f,
    val marginBottom: Float = 78f,
    val marginLeft: Float = 72f,
    val marginRight: Float = 72f,
) {
    val textWidth: Float get() = pageWidth - marginLeft - marginRight
    val textBottom: Float get() = pageHeight - marginBottom
}

data class TypesetLine(
    val words: List<String>,
    val x: Float,
    val baseline: Float,
    val size: Float,
    val bold: Boolean = false,
    val justify: Boolean = false,
    /** Nomor halaman tujuan untuk baris daftar isi. */
    val folio: String? = null,
)

data class TypesetPage(
    val lines: List<TypesetLine>,
    val folio: String?,
    val runningTitle: String?,
)

data class TypesetSection(
    val title: String,
    val body: String,
    val volume: String? = null,
    val startsVolume: Boolean = false,
)

/**
 * Tata letak cetak otomatis: naskah dipotong sendiri ke halaman A4 dengan margin buku, bab selalu
 * mulai di halaman baru, teks rata kanan-kiri, indentasi baris pertama, nomor halaman, dan daftar
 * isi yang nomor halamannya dihitung dari hasil pemotongan itu sendiri.
 *
 * Seluruh perhitungan memakai [TextMetrics] sehingga bisa diuji di JVM tanpa Android.
 */
class Typesetter(
    private val metrics: TextMetrics,
    private val format: PrintFormat = PrintFormat(),
) {

    fun typeset(
        title: String,
        sections: List<TypesetSection>,
        frontMatter: Boolean = true,
        toc: Boolean = frontMatter,
    ): List<TypesetPage> {
        val heading = title.ifBlank { "Tanpa judul" }
        val entries = if (toc) tocEntries(sections) else emptyList()
        val flow = BodyFlow(heading, entries.size)
        sections.forEachIndexed { index, section ->
            flow.beginChapter()
            entries.forEachIndexed { entryIndex, entry ->
                if (entry.sectionIndex == index) flow.record(entryIndex)
            }
            if (section.startsVolume) {
                section.volume?.takeIf { it.isNotBlank() }?.let {
                    flow.heading(it, size = 20f, before = 0f, after = 18f, leading = 30f)
                }
            }
            flow.heading(section.title.ifBlank { "Tanpa judul" }, size = 17f, before = 0f, after = 14f, leading = 26f)
            flow.body(section.body)
        }
        val pages = mutableListOf<TypesetPage>()
        if (frontMatter) pages += titlePage(heading, sections)
        if (entries.isNotEmpty()) pages += tocPages(entries, flow.folios)
        pages += flow.finish()
        return pages
    }

    // ---------------------------------------------------------------- halaman judul

    private fun titlePage(title: String, sections: List<TypesetSection>): TypesetPage {
        val lines = mutableListOf<TypesetLine>()
        fun centered(words: List<String>, size: Float, bold: Boolean, baseline: Float) {
            if (words.isEmpty()) return
            val width = metrics.width(words.joinToString(" "), size, bold)
            val x = format.marginLeft + max(0f, (format.textWidth - width) / 2f)
            lines += TypesetLine(words, x, baseline, size, bold)
        }

        var y = format.marginTop + 120f
        wrap(title.split(" ").filter { it.isNotBlank() }, 26f, true, format.textWidth * 0.85f).forEach { words ->
            y += 34f
            centered(words, 26f, true, y)
        }
        y += 26f
        centered(listOf("T i n t a"), 10f, false, y)
        y += 20f
        centered(listOf(java.time.Year.now().value.toString()), 10f, false, y)

        val copyright = ("Hak cipta © ${java.time.Year.now().value} ${title.ifBlank { "Tanpa judul" }}. " +
            "Seluruh hak atas naskah ini milik penulis. Disusun otomatis oleh Tinta dengan tata letak " +
            "buku A4, ${sections.size} bab, dan tanpa mengunggah naskah ke mana pun.")
        val copyrightLines = wrap(copyright.split(" ").filter { it.isNotBlank() }, 9.5f, false, format.textWidth * 0.72f)
        var cy = format.textBottom - 14f * copyrightLines.size
        copyrightLines.forEach { words ->
            centered(words, 9.5f, false, cy)
            cy += 14f
        }
        return TypesetPage(lines, null, null)
    }

    // ---------------------------------------------------------------- daftar isi

    private data class TocEntry(val title: String, val volume: Boolean, val sectionIndex: Int)

    private fun tocEntries(sections: List<TypesetSection>): List<TocEntry> {
        val out = mutableListOf<TocEntry>()
        sections.forEachIndexed { index, section ->
            if (section.startsVolume) {
                section.volume?.takeIf { it.isNotBlank() }?.let {
                    out += TocEntry(it, volume = true, sectionIndex = index)
                }
            }
            out += TocEntry(section.title.ifBlank { "Tanpa judul" }, volume = false, sectionIndex = index)
        }
        return out
    }

    private fun tocPages(entries: List<TocEntry>, folios: IntArray): List<TypesetPage> {
        val pages = mutableListOf<TypesetPage>()
        var lines = mutableListOf<TypesetLine>()
        var y = format.marginTop

        fun flush() {
            if (lines.isEmpty()) return
            pages += TypesetPage(lines, folio = null, runningTitle = runTitle)
            lines = mutableListOf()
            y = format.marginTop
        }

        fun heading() {
            lines += TypesetLine(listOf("Daftar isi"), format.marginLeft, y + 16f, 16f, bold = true)
            y += 34f
        }

        heading()
        entries.forEach { entry ->
            val size = if (entry.volume) 11f else 11f
            val leading = if (entry.volume) 24f else 19f
            if (y + leading > format.textBottom) {
                flush()
                heading()
            }
            val words = fit(
                entry.title.split(" ").filter { it.isNotBlank() },
                size,
                entry.volume,
                format.textWidth - 42f,
            )
            val x = format.marginLeft + if (entry.volume) 0f else 14f
            val folio = folios.getOrNull(entry.sectionIndex)?.takeIf { it > 0 }?.toString()
            lines += TypesetLine(words, x, y + size, size, bold = entry.volume, justify = false, folio = folio)
            y += leading
        }
        flush()
        return pages
    }

    // ---------------------------------------------------------------- isi naskah

    private inner class BodyFlow(title: String, tocCount: Int) {
        private val runningTitle = title.uppercase()
        private val pages = mutableListOf<TypesetPage>()
        private val lines = mutableListOf<TypesetLine>()
        val folios = IntArray(tocCount)
        private var pageNo = 0
        private var y = format.marginTop

        fun beginChapter() {
            if (pageNo > 0) flush()
            pageNo += 1
            y = format.marginTop
        }

        fun record(entryIndex: Int) {
            folios[entryIndex] = pageNo
        }

        private fun flush() {
            if (lines.isEmpty()) return
            pages += TypesetPage(lines.toList(), pageNo.toString(), runningTitle)
            lines.clear()
        }

        private fun breakPage() {
            flush()
            pageNo += 1
            y = format.marginTop
        }

        private fun space(height: Float) {
            if (lines.isNotEmpty()) y += height
        }

        fun heading(text: String, size: Float, before: Float, after: Float, leading: Float) {
            val needed = before + leading + BODY_LEADING * 2f
            if (lines.isNotEmpty() && y + needed > format.textBottom) breakPage()
            space(before)
            val words = text.split(" ").filter { it.isNotBlank() }
            if (words.isNotEmpty()) {
                lines += TypesetLine(words, format.marginLeft, y + size, size, bold = true)
            }
            y += leading
            space(after)
        }

        fun body(text: String) {
            if (text.isBlank()) return
            val buffer = StringBuilder()
            var firstParagraph = true

            fun flushParagraph() {
                val content = buffer.toString().trim()
                buffer.setLength(0)
                if (content.isEmpty()) return
                paragraph(content, firstIndent = if (firstParagraph) 0f else FIRST_LINE_INDENT)
                firstParagraph = false
            }

            text.split("\n").forEach { raw ->
                val line = raw.trim()
                when {
                    line.isEmpty() -> flushParagraph()
                    line.startsWith("#") -> {
                        flushParagraph()
                        val level = line.takeWhile { it == '#' }.length
                        val size = when {
                            level <= 1 -> 15f
                            level == 2 -> 13f
                            else -> 11.5f
                        }
                        heading(
                            plainMarkdown(line.trimStart('#').trim()),
                            size = size,
                            before = 14f,
                            after = 8f,
                            leading = size * 1.6f,
                        )
                    }
                    isBullet(line) -> {
                        flushParagraph()
                        paragraph(
                            "\u2022 " + plainMarkdown(line.drop(2).trim()),
                            firstIndent = 14f,
                            restIndent = 14f,
                        )
                    }
                    else -> {
                        if (buffer.isNotEmpty()) buffer.append(' ')
                        buffer.append(plainMarkdown(line))
                    }
                }
            }
            flushParagraph()
        }

        private fun paragraph(text: String, firstIndent: Float = FIRST_LINE_INDENT, restIndent: Float = 0f) {
            val words = text.split(" ").filter { it.isNotBlank() }
            if (words.isEmpty()) return
            val wrapped = wrapWords(words, BODY_SIZE, false, firstIndent, restIndent)
            wrapped.forEachIndexed { index, lineWords ->
                val last = index == wrapped.lastIndex
                if (lines.isNotEmpty() && y + BODY_LEADING > format.textBottom) breakPage()
                // Jangan tinggalkan satu baris sendirian di puncak halaman berikutnya.
                if (lines.isNotEmpty() && wrapped.size > 1 && y + BODY_LEADING * 2 > format.textBottom) {
                    if (index + 2 >= wrapped.size) breakPage()
                }
                val x = format.marginLeft + if (index == 0) firstIndent else restIndent
                lines += TypesetLine(
                    words = lineWords,
                    x = x,
                    baseline = y + BODY_SIZE,
                    size = BODY_SIZE,
                    justify = !last && lineWords.size > 1,
                )
                y += BODY_LEADING
            }
        }

        fun finish(): List<TypesetPage> {
            flush()
            return pages.toList()
        }
    }

    // ---------------------------------------------------------------- pembantu

    private fun wrapWords(
        words: List<String>,
        size: Float,
        bold: Boolean,
        firstIndent: Float,
        restIndent: Float,
    ): List<List<String>> {
        val space = metrics.width(" ", size, bold)
        val out = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        var used = 0f
        words.forEach { word ->
            val width = metrics.width(word, size, bold)
            val limit = format.textWidth - if (out.isEmpty()) firstIndent else restIndent
            if (current.isNotEmpty() && used + space + width > limit) {
                out += current
                current = mutableListOf(word)
                used = width
            } else {
                used += (if (current.isEmpty()) 0f else space) + width
                current += word
            }
        }
        if (current.isNotEmpty()) out += current
        return out
    }

    private fun wrap(words: List<String>, size: Float, bold: Boolean, width: Float): List<List<String>> {
        if (words.isEmpty()) return emptyList()
        val space = metrics.width(" ", size, bold)
        val out = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        var used = 0f
        words.forEach { word ->
            val w = metrics.width(word, size, bold)
            if (current.isNotEmpty() && used + space + w > width) {
                out += current
                current = mutableListOf(word)
                used = w
            } else {
                used += (if (current.isEmpty()) 0f else space) + w
                current += word
            }
        }
        if (current.isNotEmpty()) out += current
        return out
    }

    /** Potong judul daftar isi yang terlalu panjang supaya tidak menabrak nomor halaman. */
    private fun fit(words: List<String>, size: Float, bold: Boolean, width: Float): List<String> {
        if (words.isEmpty()) return words
        if (metrics.width(words.joinToString(" "), size, bold) <= width) return words
        val visible = words.toMutableList()
        while (visible.size > 1 && metrics.width(visible.joinToString(" ") + "…", size, bold) > width) {
            visible.removeAt(visible.lastIndex)
        }
        visible[visible.lastIndex] = visible.last().trimEnd(',', '.', ';', ':') + "…"
        return visible
    }

    private companion object {
        const val BODY_SIZE = 11f
        const val BODY_LEADING = 17f
        const val FIRST_LINE_INDENT = 17f
        val runTitle = "DAFTAR ISI"
    }
}

internal fun isBullet(line: String): Boolean =
    line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")

/** Buang penanda Markdown sebaris supaya teks cetak bersih. */
fun plainMarkdown(text: String): String = text
    .replace(Regex("!?\\[([^\\]]+)]\\([^)]*\\)"), "$1")
    .replace("**", "")
    .replace("__", "")
    .replace("`", "")
    .replace(Regex("(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)"), "$1")
    .trim()
