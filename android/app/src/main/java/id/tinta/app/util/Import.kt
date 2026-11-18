package id.tinta.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

data class ImportedChapter(val title: String, val body: String)

data class ImportedBook(val title: String, val chapters: List<ImportedChapter>)

/**
 * Membaca naskah dari penyimpanan perangkat: teks/Markdown, DOCX (dipisah per judul), dan EPUB
 * (mengikuti urutan spine). Tidak ada pustaka tambahan dan tidak ada unggahan.
 */
object Import {

    fun read(context: Context, uri: Uri): ImportedBook? = runCatching {
        val name = displayName(context, uri)
        val resolver = context.contentResolver
        when {
            name.endsWith(".epub", ignoreCase = true) -> {
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
                fromEpub(bytes, name)
            }
            name.endsWith(".docx", ignoreCase = true) -> {
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
                fromDocx(bytes, name)
            }
            else -> {
                val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return null
                fromText(text, name)
            }
        }
    }.getOrNull()

    fun fromText(text: String, fileName: String): ImportedBook {
        val clean = text.replace("\r\n", "\n").replace('\r', '\n')
        val lines = clean.lines()
        val baseTitle = fileName.substringBeforeLast('.').trim().ifBlank { "Impor" }
        val firstContent = lines.indexOfFirst { it.isNotBlank() }
        val h1Index = lines.indexOfFirst { it.startsWith("# ") && it.removePrefix("# ").isNotBlank() }

        if (h1Index >= 0 && h1Index == firstContent) {
            val bookTitle = lines[h1Index].removePrefix("# ").trim()
            val chapters = split(lines.drop(h1Index + 1), level = 2)
            if (chapters.isNotEmpty()) return ImportedBook(bookTitle, chapters)
        }

        val chapters = split(lines, level = 1)
        if (chapters.isNotEmpty()) return ImportedBook(baseTitle, chapters)
        return ImportedBook(baseTitle, listOf(ImportedChapter(baseTitle, clean.trim())))
    }

    private data class DocParagraph(val text: String, val style: String, val bold: Boolean)

    fun fromDocx(bytes: ByteArray, fileName: String): ImportedBook {
        val fallbackTitle = fileName.substringBeforeLast('.').trim().ifBlank { "Impor" }
        val xml = readZipEntry(bytes, "word/document.xml")
        if (xml.isBlank()) return ImportedBook(fallbackTitle, listOf(ImportedChapter(fallbackTitle, "")))

        val paragraphs = xml.split("</w:p>").mapNotNull { chunk ->
            val style = Regex("<w:pStyle w:val=\"([^\"]+)\"").find(chunk)?.groupValues?.get(1).orEmpty()
            val bold = chunk.contains("<w:b/>") || Regex("<w:b w:val=\"(1|true)\"/>").containsMatchIn(chunk)
            val text = cleanXmlText(chunk)
            if (text.isBlank()) null else DocParagraph(text, style, bold)
        }

        val chapters = mutableListOf<ImportedChapter>()
        var bookTitle = ""
        var currentTitle: String? = null
        val buffer = StringBuilder()

        fun flush() {
            val body = buffer.toString().trim()
            buffer.setLength(0)
            if (currentTitle == null && body.isEmpty()) return
            chapters += ImportedChapter(
                title = currentTitle?.takeIf { it.isNotBlank() } ?: "Bab ${chapters.size + 1}",
                body = body,
            )
        }

        paragraphs.forEach { paragraph ->
            val style = paragraph.style.lowercase()
            val headingLevel = when {
                style.startsWith("title") || style.startsWith("judul") -> 0
                style.startsWith("heading") -> style.lastOrNull()?.digitToIntOrNull() ?: 1
                else -> null
            }
            val shortBoldHeading = headingLevel == null &&
                paragraph.bold &&
                paragraph.text.length <= 70 &&
                !paragraph.text.endsWith(".") &&
                !paragraph.text.contains(": ")
            if (headingLevel != null || shortBoldHeading) {
                if (headingLevel == 0 && bookTitle.isBlank() && chapters.isEmpty()) {
                    bookTitle = paragraph.text
                    return@forEach
                }
                flush()
                currentTitle = paragraph.text
            } else {
                if (buffer.isNotEmpty()) buffer.append('\n')
                buffer.append(paragraph.text)
            }
        }
        flush()

        if (chapters.isEmpty()) {
            val text = paragraphs.joinToString("\n\n") { it.text }
            return ImportedBook(fallbackTitle, listOf(ImportedChapter(fallbackTitle, text)))
        }
        return ImportedBook(bookTitle.ifBlank { fallbackTitle }, chapters)
    }

    fun fromEpub(bytes: ByteArray, fileName: String): ImportedBook {
        val fallbackTitle = fileName.substringBeforeLast('.').trim().ifBlank { "Impor" }
        val entries = mutableMapOf<String, ByteArray>()
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) entries[entry.name] = zip.readBytes()
                    entry = zip.nextEntry
                }
            }
        }
        fun textOf(path: String): String? = entries[path]?.toString(Charsets.UTF_8)

        val container = textOf("META-INF/container.xml").orEmpty()
        val opfPath = Regex("full-path=\"([^\"]+)\"").find(container)?.groupValues?.get(1)
            ?: entries.keys.firstOrNull { it.endsWith(".opf") }
        val opf = opfPath?.let { textOf(it) }.orEmpty()
        val baseDir = opfPath?.substringBeforeLast('/', "")?.let { if (it.isEmpty()) "" else "$it/" }.orEmpty()

        val manifest = mutableMapOf<String, String>()
        Regex("<item\\b[^>]*>").findAll(opf).forEach { item ->
            val tag = item.value
            val id = Regex("id=\"([^\"]+)\"").find(tag)?.groupValues?.get(1) ?: return@forEach
            val href = Regex("href=\"([^\"]+)\"").find(tag)?.groupValues?.get(1) ?: return@forEach
            manifest[id] = href
        }
        val spine = Regex("<itemref\\b[^>]*idref=\"([^\"]+)\"").findAll(opf)
            .map { it.groupValues[1] }
            .toList()
        val hrefs = if (spine.isNotEmpty()) {
            spine.mapNotNull { manifest[it] }
        } else {
            entries.keys.filter { it.endsWith(".xhtml") || it.endsWith(".html") }.sorted()
        }

        val chapters = mutableListOf<ImportedChapter>()
        hrefs.distinct().forEach { href ->
            val path = if (href.startsWith("/")) href.drop(1) else baseDir + href.substringBefore('#')
            val html = textOf(path)
                ?: entries.entries.firstOrNull { it.key.endsWith(href.substringBefore('#')) }
                    ?.value?.toString(Charsets.UTF_8)
                ?: return@forEach
            val file = path.substringAfterLast('/').lowercase()
            if (file.startsWith("nav") || file.startsWith("toc") || file.startsWith("cover")) {
                return@forEach
            }
            val headingMatch = Regex("<h[12][^>]*>(.*?)</h[12]>", RegexOption.DOT_MATCHES_ALL).find(html)
            val heading = headingMatch?.groupValues?.get(1)?.let { cleanHtml(it) }?.takeIf { it.isNotBlank() }
            val title = heading
                ?: Regex("<title[^>]*>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
                    .find(html)?.groupValues?.get(1)?.let { cleanHtml(it) }?.takeIf { it.isNotBlank() }
                ?: "Bab ${chapters.size + 1}"
            // Judul bab sudah dipakai sebagai nama bab; jangan digambar ulang di awal isi.
            val bodyHtml = if (headingMatch != null) {
                html.removeRange(headingMatch.range.first, headingMatch.range.last + 1)
            } else {
                html
            }
            val body = bodyHtml
                .replace(
                    Regex("<(script|style)[^>]*>.*?</\\1>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)),
                    "",
                )
                .split(Regex("</(p|div|h[1-6]|li)>|<br\\s*/?>", RegexOption.IGNORE_CASE))
                .map { cleanHtml(it) }
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
            if (body.isNotBlank()) chapters += ImportedChapter(title, body)
        }

        val bookTitle = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL)
            .find(opf)?.groupValues?.get(1)?.let { cleanHtml(it) }?.takeIf { it.isNotBlank() }
            ?: fallbackTitle
        if (chapters.isEmpty()) {
            return ImportedBook(bookTitle, listOf(ImportedChapter(bookTitle, "")))
        }
        return ImportedBook(bookTitle, chapters)
    }

    private fun split(lines: List<String>, level: Int): List<ImportedChapter> {
        val prefix = "#".repeat(level) + " "
        val chapters = mutableListOf<ImportedChapter>()
        var currentTitle: String? = null
        val buffer = StringBuilder()

        fun flush() {
            val body = buffer.toString().trim()
            if (currentTitle != null || body.isNotEmpty()) {
                chapters.add(
                    ImportedChapter(
                        title = currentTitle?.takeIf { it.isNotBlank() } ?: "Bab ${chapters.size + 1}",
                        body = body,
                    ),
                )
            }
            buffer.clear()
        }

        for (line in lines) {
            val isHeading = line.startsWith(prefix) &&
                !(level == 1 && line.startsWith("## ")) &&
                line.removePrefix(prefix).isNotBlank()
            if (isHeading) {
                flush()
                currentTitle = line.removePrefix(prefix).trim()
                continue
            }
            buffer.append(line).append('\n')
        }
        flush()
        return chapters
    }

    private fun readZipEntry(bytes: ByteArray, path: String): String = runCatching {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == path) return@use zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
            ""
        }
    }.getOrDefault("")

    private fun cleanXmlText(chunk: String): String = chunk
        .replace("<w:tab/>", "\t")
        .replace("<w:br/>", "\n")
        .replace("<w:br />", "\n")
        .replace(Regex("<[^>]+>"), "")
        .let { decodeEntities(it) }
        .trim()

    private fun cleanHtml(chunk: String): String = decodeEntities(
        chunk
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace('\u00a0', ' '),
    ).replace(Regex("[ \\t]+"), " ").trim()

    private fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&#160;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&hellip;", "…")

    private fun displayName(context: Context, uri: Uri): String {
        val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "impor.txt"
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (column >= 0) cursor.getString(column) else null
                    } else {
                        null
                    }
                }
        }.getOrNull() ?: fallback
    }
}
