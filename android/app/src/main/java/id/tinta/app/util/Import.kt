package id.tinta.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

data class ImportedChapter(val title: String, val body: String)

data class ImportedBook(val title: String, val chapters: List<ImportedChapter>)

/**
 * Membaca naskah dari penyimpanan perangkat. Mendukung Markdown/teks biasa (dipisah per judul
 * `#` atau `##`) dan DOCX (isi `word/document.xml` dibaca tanpa pustaka tambahan).
 */
object Import {

    fun read(context: Context, uri: Uri): ImportedBook? = runCatching {
        val name = displayName(context, uri)
        val resolver = context.contentResolver
        if (name.endsWith(".docx", ignoreCase = true)) {
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            fromDocx(bytes, name)
        } else {
            val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return null
            fromText(text, name)
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

    fun fromDocx(bytes: ByteArray, fileName: String): ImportedBook {
        val xml = runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") return@use zip.readBytes().toString(Charsets.UTF_8)
                    entry = zip.nextEntry
                }
                ""
            }
        }.getOrDefault("")

        val text = xml
            .replace("</w:p>", "\n")
            .replace("<w:br/>", "\n")
            .replace("<w:br />", "\n")
            .replace("<w:tab/>", "\t")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .trim()

        val title = fileName.substringBeforeLast('.').trim().ifBlank { "Impor" }
        return ImportedBook(title, listOf(ImportedChapter(title, text)))
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
                    )
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
