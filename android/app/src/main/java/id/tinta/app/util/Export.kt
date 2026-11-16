package id.tinta.app.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import id.tinta.app.data.Book
import id.tinta.app.data.Chapter
import id.tinta.app.data.Volume
import java.io.ByteArrayOutputStream

/** Whole-book export: Markdown, plain text and PDF, all built on the device. */
object Export {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 54f

    fun orderGroups(book: Book, chapters: List<Chapter>): List<Pair<Volume?, List<Chapter>>> {
        val alive = chapters.filter { it.deletedAt == null }.sortedBy { it.order }
        val groups = book.volumes.map { volume ->
            volume to alive.filter { it.volumeId == volume.id }
        }
        val orphans = alive.filter { chapter ->
            chapter.volumeId == null || book.volumes.none { it.id == chapter.volumeId }
        }
        return groups + (null to orphans)
    }

    fun bookMarkdown(book: Book, chapters: List<Chapter>): String {
        val out = StringBuilder()
        out.append("# ").append(book.title.ifBlank { "Tanpa judul" }).append("\n\n")
        if (book.tags.isNotEmpty()) {
            out.append(book.tags.joinToString(" ") { "#$it" }).append("\n\n")
        }
        orderGroups(book, chapters).forEach { (volume, items) ->
            if (items.isEmpty()) return@forEach
            if (volume != null) {
                out.append("## ").append(volume.title.ifBlank { "Bagian" }).append("\n\n")
            }
            items.forEach { chapter ->
                out.append("### ").append(chapter.title.ifBlank { "Tanpa judul" }).append("\n\n")
                if (chapter.body.isNotBlank()) out.append(chapter.body.trim()).append("\n\n")
            }
        }
        return out.toString().trimEnd() + "\n"
    }

    fun bookPlainText(book: Book, chapters: List<Chapter>): String {
        val out = StringBuilder()
        out.append(book.title.ifBlank { "Tanpa judul" }).append("\n")
        if (book.tags.isNotEmpty()) {
            out.append(book.tags.joinToString(" · ")).append("\n")
        }
        orderGroups(book, chapters).forEach { (volume, items) ->
            if (items.isEmpty()) return@forEach
            if (volume != null) out.append("\n\n").append(volume.title.ifBlank { "Bagian" }).append("\n")
            items.forEach { chapter ->
                out.append("\n\n").append(chapter.title.ifBlank { "Tanpa judul" }).append("\n\n")
                if (chapter.body.isNotBlank()) out.append(chapter.body.trim()).append("\n")
            }
        }
        return out.toString()
    }

    fun bookPdf(book: Book, chapters: List<Chapter>): ByteArray {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        val maxWidth = PAGE_WIDTH - MARGIN * 2
        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
        )
        var canvas = page.canvas
        var y = MARGIN + 12f

        fun footer() {
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 9f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("$pageNumber", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN / 2, paint)
            paint.textAlign = Paint.Align.LEFT
        }

        fun nextPage() {
            footer()
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
            )
            canvas = page.canvas
            y = MARGIN + 12f
        }

        fun drawLine(text: String, size: Float, bold: Boolean, gap: Float) {
            paint.textSize = size
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            if (y + size > PAGE_HEIGHT - MARGIN) nextPage()
            canvas.drawText(text, MARGIN, y, paint)
            y += gap
        }

        fun drawParagraph(text: String, size: Float, bold: Boolean, gap: Float) {
            paint.textSize = size
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val words = text.split(" ").filter { it.isNotEmpty() }
            var line = StringBuilder()
            words.forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (line.isNotEmpty() && paint.measureText(candidate) > maxWidth) {
                    drawLine(line.toString(), size, bold, gap)
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(candidate)
                }
            }
            if (line.isNotEmpty()) drawLine(line.toString(), size, bold, gap)
        }

        fun drawBlock(text: String, size: Float, bold: Boolean, gap: Float) {
            val blocks = text.split("\n")
            blocks.forEach { block ->
                val line = block.trimEnd()
                if (line.isBlank()) {
                    y += gap * 0.6f
                } else {
                    drawParagraph(line, size, bold, gap)
                }
            }
        }

        // Halaman judul + hak cipta
        y += 60f
        drawParagraph(book.title.ifBlank { "Tanpa judul" }, 24f, true, 34f)
        drawParagraph("Tinta", 11f, false, 20f)
        if (book.tags.isNotEmpty()) {
            drawParagraph(book.tags.joinToString(" · "), 10f, false, 18f)
        }
        y += 14f
        drawParagraph(
            "Hak cipta © ${java.time.Year.now().value} ${book.title.ifBlank { "Tanpa judul" }}. " +
                "Dibuat dengan Tinta dan disimpan di perangkat penulis.",
            9f,
            false,
            14f,
        )

        // Daftar isi
        nextPage()
        drawParagraph("Daftar isi", 16f, true, 26f)
        var entry = 0
        orderGroups(book, chapters).forEach { (volume, items) ->
            if (items.isEmpty()) return@forEach
            if (volume != null) {
                drawParagraph(volume.title.ifBlank { "Bagian" }, 11f, true, 20f)
            }
            items.forEach { chapter ->
                entry += 1
                drawParagraph("$entry. ${chapter.title.ifBlank { "Tanpa judul" }}", 11f, false, 18f)
            }
        }

        nextPage()
        orderGroups(book, chapters).forEach { (volume, items) ->
            if (items.isEmpty()) return@forEach
            if (volume != null) {
                y += 12f
                drawParagraph(volume.title.ifBlank { "Bagian" }, 14f, true, 24f)
            }
            items.forEach { chapter ->
                y += 10f
                drawParagraph(chapter.title.ifBlank { "Tanpa judul" }, 13f, true, 22f)
                if (chapter.body.isNotBlank()) drawBlock(chapter.body.trim(), 11f, false, 18f)
            }
        }

        footer()
        document.finishPage(page)
        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    fun bookEpub(book: Book, chapters: List<Chapter>): ByteArray {
        val pages = mutableListOf<Pair<String, String>>()
        orderGroups(book, chapters).forEach { (volume, items) ->
            if (items.isEmpty()) return@forEach
            if (volume != null) {
                val heading = volume.title.ifBlank { "Bagian" }
                pages.add(heading to "<h1>${xml(heading)}</h1>")
            }
            items.forEach { chapter ->
                val title = chapter.title.ifBlank { "Tanpa judul" }
                val html = buildString {
                    append("<h1>").append(xml(title)).append("</h1>")
                    chapter.body.trim().split("\n").forEach { line ->
                        if (line.isNotBlank()) append("<p>").append(xml(line.trim())).append("</p>")
                    }
                }
                pages.add(title to html)
            }
        }
        if (pages.isEmpty()) pages.add(book.title.ifBlank { "Tanpa judul" } to "<p></p>")

        val names = pages.indices.map { "ch${it + 1}.xhtml" }
        val out = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zip ->
            val mimetype = "application/epub+zip".toByteArray(Charsets.UTF_8)
            val entry = java.util.zip.ZipEntry("mimetype").apply {
                method = java.util.zip.ZipEntry.STORED
                size = mimetype.size.toLong()
                crc = java.util.zip.CRC32().apply { update(mimetype) }.value
            }
            zip.putNextEntry(entry)
            zip.write(mimetype)
            zip.closeEntry()

            fun put(name: String, content: String) {
                zip.putNextEntry(java.util.zip.ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            put(
                "META-INF/container.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<container version=\"1.0\" " +
                    "xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">" +
                    "<rootfiles><rootfile full-path=\"OEBPS/content.opf\" " +
                    "media-type=\"application/oebps-package+xml\"/></rootfiles></container>",
            )

            val manifest = StringBuilder()
            val spine = StringBuilder()
            pages.forEachIndexed { index, page ->
                val id = "p${index + 1}"
                manifest.append("<item id=\"").append(id).append("\" href=\"").append(names[index])
                    .append("\" media-type=\"application/xhtml+xml\"/>")
                spine.append("<itemref idref=\"").append(id).append("\"/>")
                put(
                    "OEBPS/${names[index]}",
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                        "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"id\"><head>" +
                        "<meta charset=\"utf-8\"/><title>${xml(page.first)}</title></head>" +
                        "<body>${page.second}</body></html>",
                )
            }

            val navItems = pages.indices.joinToString("") { index ->
                "<li><a href=\"${names[index]}\">${xml(pages[index].first)}</a></li>"
            }
            put(
                "OEBPS/nav.xhtml",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"id\"><head>" +
                    "<meta charset=\"utf-8\"/><title>Daftar isi</title></head><body>" +
                    "<nav epub:type=\"toc\" xmlns:epub=\"http://www.idpf.org/2007/ops\">" +
                    "<h1>Daftar isi</h1><ol>$navItems</ol></nav></body></html>",
            )

            put(
                "OEBPS/content.opf",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" " +
                    "unique-identifier=\"bookid\"><metadata " +
                    "xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
                    "<dc:identifier id=\"bookid\">urn:uuid:${book.id}</dc:identifier>" +
                    "<dc:title>${xml(book.title.ifBlank { "Tanpa judul" })}</dc:title>" +
                    "<dc:language>id</dc:language><dc:creator>Tinta</dc:creator>" +
                    "<meta property=\"dcterms:modified\">${java.time.Instant.now()}</meta>" +
                    "</metadata><manifest>" +
                    "<item id=\"nav\" href=\"nav.xhtml\" " +
                    "media-type=\"application/xhtml+xml\" properties=\"nav\"/>" +
                    manifest +
                    "</manifest><spine>$spine</spine></package>",
            )
        }
        return out.toByteArray()
    }

    fun bookDocx(book: Book, chapters: List<Chapter>): ByteArray {
        val body = StringBuilder()
        fun paragraph(text: String, bold: Boolean) {
            body.append("<w:p><w:r>")
            if (bold) body.append("<w:rPr><w:b/></w:rPr>")
            body.append("<w:t xml:space=\"preserve\">").append(xml(text)).append("</w:t>")
            body.append("</w:r></w:p>")
        }

        paragraph(book.title.ifBlank { "Tanpa judul" }, bold = true)
        if (book.tags.isNotEmpty()) paragraph(book.tags.joinToString(" · "), bold = false)
        orderGroups(book, chapters).forEach { (volume, items) ->
            if (items.isEmpty()) return@forEach
            if (volume != null) paragraph(volume.title.ifBlank { "Bagian" }, bold = true)
            items.forEach { chapter ->
                paragraph(chapter.title.ifBlank { "Tanpa judul" }, bold = true)
                chapter.body.trim().split("\n").forEach { line ->
                    if (line.isNotBlank()) paragraph(line.trim(), bold = false)
                }
            }
        }

        val out = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(java.util.zip.ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            put(
                "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" " +
                    "ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/word/document.xml\" ContentType=\"application/" +
                    "vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>",
            )
            put(
                "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/" +
                    "relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/" +
                    "officeDocument/2006/relationships/officeDocument\" " +
                    "Target=\"word/document.xml\"/></Relationships>",
            )
            put(
                "word/document.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                    "<w:body>$body<w:sectPr/></w:body></w:document>",
            )
        }
        return out.toByteArray()
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
