package id.tinta.app.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import id.tinta.app.data.Book
import id.tinta.app.data.Chapter
import id.tinta.app.data.Volume
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Ekspor di perangkat: Markdown, teks polos, PDF, EPUB, dan DOCX.
 *
 * PDF memakai [Typesetter] supaya tata letak dirapikan otomatis: margin buku, bab mulai di halaman
 * baru, rata kanan-kiri, indentasi, nomor halaman, dan daftar isi dengan nomor halaman sungguhan.
 * EPUB dan DOCX memakai gaya tipografi yang sama supaya hasilnya konsisten di pembaca mana pun.
 */
object Export {

    private val format = PrintFormat()

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

    /** Satu daftar bagian-bagian naskah dalam urutan baca, siap ditata. */
    fun sections(book: Book, chapters: List<Chapter>): List<TypesetSection> {
        val out = mutableListOf<TypesetSection>()
        orderGroups(book, chapters).forEach { (volume, items) ->
            val volumeTitle = volume?.title?.ifBlank { "Bagian" }
            items.forEachIndexed { index, chapter ->
                out += TypesetSection(
                    title = chapter.title.ifBlank { "Tanpa judul" },
                    body = chapter.body,
                    volume = volumeTitle,
                    startsVolume = volume != null && index == 0,
                )
            }
        }
        return out
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

    // ------------------------------------------------------------------ PDF

    fun bookPdf(book: Book, chapters: List<Chapter>, cover: Bitmap? = null): ByteArray {
        val title = book.title.ifBlank { "Tanpa judul" }
        val sections = sections(book, chapters)
            .ifEmpty { listOf(TypesetSection(title, book.tags.joinToString(" · "), null, false)) }
        return pdf(title, sections, cover, frontMatter = true)
    }

    fun chapterPdf(chapter: Chapter): ByteArray {
        val title = chapter.title.ifBlank { "Tanpa judul" }
        return pdf(title, listOf(TypesetSection(title, chapter.body)), cover = null, frontMatter = false)
    }

    private fun pdf(
        title: String,
        sections: List<TypesetSection>,
        cover: Bitmap?,
        frontMatter: Boolean,
    ): ByteArray {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.LEFT
        }
        val regular = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        val bold = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val metrics = TextMetrics { text, size, isBold ->
            paint.typeface = if (isBold) bold else regular
            paint.textSize = size
            paint.measureText(text)
        }
        val pages = Typesetter(metrics, format).typeset(title, sections, frontMatter = frontMatter)
        val document = PdfDocument()
        var pageNo = 0

        fun startPage(): PdfDocument.Page {
            pageNo += 1
            return document.startPage(
                PdfDocument.PageInfo.Builder(
                    format.pageWidth.toInt(),
                    format.pageHeight.toInt(),
                    pageNo,
                ).create(),
            )
        }

        cover?.let { bitmap ->
            val page = startPage()
            val canvas = page.canvas
            val maxWidth = format.textWidth
            val maxHeight = format.pageHeight - format.marginTop - format.marginBottom
            val scale = minOf(maxWidth / bitmap.width, maxHeight / bitmap.height)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            val left = (format.pageWidth - width) / 2f
            val top = (format.pageHeight - height) / 2f
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + width, top + height), paint)
            document.finishPage(page)
        }

        pages.forEach { typesetPage ->
            val page = startPage()
            val canvas = page.canvas
            paint.typeface = regular
            paint.textAlign = Paint.Align.CENTER
            typesetPage.runningTitle?.let { running ->
                paint.textSize = 8.5f
                canvas.drawText(running, format.pageWidth / 2f, format.marginTop - 30f, paint)
            }
            typesetPage.lines.forEach { line ->
                paint.typeface = if (line.bold) bold else regular
                paint.textSize = line.size
                paint.textAlign = Paint.Align.LEFT
                val widths = line.words.map { paint.measureText(it) }
                if (line.justify && line.words.size > 1) {
                    val available = format.pageWidth - format.marginRight - line.x
                    val gap = (available - widths.sum()) / (line.words.size - 1)
                    var x = line.x
                    line.words.forEachIndexed { index, word ->
                        canvas.drawText(word, x, line.baseline, paint)
                        x += widths[index] + gap
                    }
                } else {
                    val text = line.words.joinToString(" ")
                    canvas.drawText(text, line.x, line.baseline, paint)
                    val folio = line.folio
                    if (folio != null) {
                        val right = format.pageWidth - format.marginRight
                        val folioWidth = paint.measureText(folio)
                        dotLeader(canvas, paint, line.x + widths.sum() + 6f, right - folioWidth - 6f, line.baseline)
                        paint.textAlign = Paint.Align.RIGHT
                        canvas.drawText(folio, right, line.baseline, paint)
                        paint.textAlign = Paint.Align.LEFT
                    }
                }
            }
            typesetPage.folio?.let { folio ->
                paint.typeface = regular
                paint.textSize = 9f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(
                    folio,
                    format.pageWidth / 2f,
                    format.pageHeight - format.marginBottom / 2f,
                    paint,
                )
            }
            document.finishPage(page)
        }

        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    private fun dotLeader(canvas: android.graphics.Canvas, paint: Paint, from: Float, to: Float, baseline: Float) {
        if (to <= from) return
        val size = paint.textSize
        paint.textSize = 8f
        paint.textAlign = Paint.Align.LEFT
        val step = paint.measureText(".") * 2.2f
        var x = from
        while (x + step <= to) {
            canvas.drawText(".", x, baseline, paint)
            x += step
        }
        paint.textSize = size
    }

    // ------------------------------------------------------------------ EPUB

    private val epubCss = """
        @charset "utf-8";
        html, body { margin: 0; padding: 0; }
        body {
            font-family: "Liberation Serif", "Times New Roman", Georgia, serif;
            font-size: 1em;
            line-height: 1.55;
            text-align: justify;
            hyphens: auto;
            margin: 1.4em 1.2em;
            widows: 2;
            orphans: 2;
        }
        h1 {
            font-size: 1.55em;
            line-height: 1.2;
            text-align: left;
            margin: 2.2em 0 1.2em;
            page-break-after: avoid;
            break-after: avoid;
        }
        h2 { font-size: 1.2em; margin: 1.8em 0 .6em; page-break-after: avoid; break-after: avoid; }
        h3 { font-size: 1.05em; margin: 1.4em 0 .4em; page-break-after: avoid; break-after: avoid; }
        h2.volume { text-align: center; margin: 2em 0 1.6em; letter-spacing: .08em; text-transform: uppercase; }
        p { margin: 0; text-indent: 1.5em; }
        h1 + p, h2 + p, h3 + p, ul + p { text-indent: 0; }
        ul { margin: .8em 0 .8em 1.2em; padding: 0; text-align: left; }
        li { margin: 0 0 .35em; line-height: 1.45; }
        .cover { text-align: center; margin: 0; padding: 0; }
        .cover img { max-width: 100%; max-height: 100%; }
        nav ol { margin: 1em 0 0 1.2em; padding: 0; text-align: left; }
        nav li { margin: .35em 0; text-indent: 0; }
    """.trimIndent()

    fun bookEpub(book: Book, chapters: List<Chapter>, cover: Bitmap? = null): ByteArray {
        val title = book.title.ifBlank { "Tanpa judul" }
        val sections = sections(book, chapters)
            .ifEmpty { listOf(TypesetSection(title, book.tags.joinToString(" · "))) }
        return epub(title, sections, cover)
    }

    fun chapterEpub(book: Book, chapter: Chapter): ByteArray {
        val title = chapter.title.ifBlank { "Tanpa judul" }
        return epub(book.title.ifBlank { title }, listOf(TypesetSection(title, chapter.body)), cover = null)
    }

    private fun epub(title: String, sections: List<TypesetSection>, cover: Bitmap?): ByteArray {
        val pages = sections.map { section ->
            val html = buildString {
                if (section.startsVolume && !section.volume.isNullOrBlank()) {
                    append("<h2 class=\"volume\">").append(xml(section.volume)).append("</h2>")
                }
                append("<h1>").append(xml(section.title.ifBlank { "Tanpa judul" })).append("</h1>")
                append(htmlBody(section.body))
            }
            section.title.ifBlank { "Tanpa judul" } to html
        }.toMutableList()
        if (pages.isEmpty()) pages.add(title to "<p></p>")

        val names = pages.indices.map { "ch${it + 1}.xhtml" }
        val coverBytes = cover?.let { bitmap ->
            ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }.toByteArray()
        }

        fun head(pageTitle: String): String =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"id\"><head>" +
                "<meta charset=\"utf-8\"/><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
                "<title>${xml(pageTitle)}</title>" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\"/></head>"

        val out = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(out).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(java.util.zip.ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            val mimetype = "application/epub+zip".toByteArray(Charsets.UTF_8)
            zip.putNextEntry(
                java.util.zip.ZipEntry("mimetype").apply {
                    method = java.util.zip.ZipEntry.STORED
                    size = mimetype.size.toLong()
                    crc = java.util.zip.CRC32().apply { update(mimetype) }.value
                },
            )
            zip.write(mimetype)
            zip.closeEntry()

            if (coverBytes != null) {
                zip.putNextEntry(java.util.zip.ZipEntry("OEBPS/cover.jpg"))
                zip.write(coverBytes)
                zip.closeEntry()
                put(
                    "OEBPS/cover.xhtml",
                    head("Sampul") +
                        "<body><div class=\"cover\"><img src=\"cover.jpg\" alt=\"Sampul\"/></div></body></html>",
                )
            }

            put(
                "META-INF/container.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<container version=\"1.0\" " +
                    "xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">" +
                    "<rootfiles><rootfile full-path=\"OEBPS/content.opf\" " +
                    "media-type=\"application/oebps-package+xml\"/></rootfiles></container>",
            )
            put("OEBPS/style.css", epubCss)

            val manifest = StringBuilder()
            val spine = StringBuilder()
            pages.forEachIndexed { index, page ->
                val id = "p${index + 1}"
                manifest.append("<item id=\"").append(id).append("\" href=\"").append(names[index])
                    .append("\" media-type=\"application/xhtml+xml\"/>")
                spine.append("<itemref idref=\"").append(id).append("\"/>")
                put("OEBPS/${names[index]}", head(page.first) + "<body>${page.second}</body></html>")
            }

            val navItems = pages.indices.joinToString("") { index ->
                "<li><a href=\"${names[index]}\">${xml(pages[index].first)}</a></li>"
            }
            put(
                "OEBPS/nav.xhtml",
                head("Daftar isi") +
                    "<body><nav epub:type=\"toc\" xmlns:epub=\"http://www.idpf.org/2007/ops\">" +
                    "<h1>Daftar isi</h1><ol>$navItems</ol></nav></body></html>",
            )
            put(
                "OEBPS/content.opf",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" " +
                    "unique-identifier=\"bookid\"><metadata " +
                    "xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
                    "<dc:identifier id=\"bookid\">urn:uuid:${bookId(title)}</dc:identifier>" +
                    "<dc:title>${xml(title)}</dc:title>" +
                    "<dc:language>id</dc:language><dc:creator>Tinta</dc:creator>" +
                    (if (coverBytes != null) "<meta name=\"cover\" content=\"cover-image\"/>" else "") +
                    "<meta property=\"dcterms:modified\">${java.time.Instant.now()}</meta>" +
                    "</metadata><manifest>" +
                    "<item id=\"nav\" href=\"nav.xhtml\" " +
                    "media-type=\"application/xhtml+xml\" properties=\"nav\"/>" +
                    "<item id=\"css\" href=\"style.css\" media-type=\"text/css\"/>" +
                    (if (coverBytes != null) {
                        "<item id=\"cover\" href=\"cover.xhtml\" " +
                            "media-type=\"application/xhtml+xml\"/><item id=\"cover-image\" " +
                            "href=\"cover.jpg\" media-type=\"image/jpeg\" properties=\"cover-image\"/>"
                    } else {
                        ""
                    }) +
                    manifest +
                    "</manifest><spine>" +
                    (if (coverBytes != null) "<itemref idref=\"cover\"/>" else "") +
                    "$spine</spine></package>",
            )
        }
        return out.toByteArray()
    }

    private fun bookId(title: String): String =
        java.util.UUID.nameUUIDFromBytes(title.toByteArray(Charsets.UTF_8)).toString()

    private fun htmlBody(body: String): String {
        val out = StringBuilder()
        val buffer = StringBuilder()
        var inList = false

        fun flushParagraph() {
            val text = buffer.toString().trim()
            buffer.setLength(0)
            if (text.isNotEmpty()) out.append("<p>").append(xml(text)).append("</p>")
        }

        fun closeList() {
            if (inList) {
                out.append("</ul>")
                inList = false
            }
        }

        body.split("\n").forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> {
                    flushParagraph()
                    closeList()
                }
                line.startsWith("###") -> {
                    flushParagraph()
                    closeList()
                    out.append("<h3>").append(xml(plainMarkdown(line.trimStart('#').trim()))).append("</h3>")
                }
                line.startsWith("#") -> {
                    flushParagraph()
                    closeList()
                    out.append("<h2>").append(xml(plainMarkdown(line.trimStart('#').trim()))).append("</h2>")
                }
                isBullet(line) -> {
                    flushParagraph()
                    if (!inList) {
                        out.append("<ul>")
                        inList = true
                    }
                    out.append("<li>").append(xml(plainMarkdown(line.drop(2).trim()))).append("</li>")
                }
                else -> {
                    closeList()
                    if (buffer.isNotEmpty()) buffer.append(' ')
                    buffer.append(plainMarkdown(line))
                }
            }
        }
        flushParagraph()
        closeList()
        return out.toString()
    }

    // ------------------------------------------------------------------ DOCX

    private val docxStyles = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
        <w:docDefaults><w:rPrDefault><w:rPr>
        <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/>
        <w:sz w:val="22"/><w:szCs w:val="22"/><w:lang w:val="id-ID"/>
        </w:rPr></w:rPrDefault><w:pPrDefault><w:pPr>
        <w:spacing w:after="0" w:line="330" w:lineRule="auto"/><w:jc w:val="both"/>
        </w:pPr></w:pPrDefault></w:docDefaults>
        <w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/></w:style>
        <w:style w:type="paragraph" w:styleId="BodyText"><w:name w:val="Body Text"/><w:basedOn w:val="Normal"/>
        <w:pPr><w:ind w:firstLine="340"/><w:spacing w:after="0" w:line="330" w:lineRule="auto"/><w:jc w:val="both"/></w:pPr><w:qFormat/></w:style>
        <w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/>
        <w:pPr><w:spacing w:before="3600" w:after="480"/><w:jc w:val="center"/></w:pPr>
        <w:rPr><w:b/><w:sz w:val="52"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/>
        <w:next w:val="BodyText"/><w:pPr><w:keepNext/><w:pageBreakBefore/><w:spacing w:before="0" w:after="240"/>
        <w:jc w:val="left"/><w:outlineLvl w:val="0"/></w:pPr><w:rPr><w:b/><w:sz w:val="32"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/>
        <w:next w:val="BodyText"/><w:pPr><w:keepNext/><w:spacing w:before="320" w:after="140"/>
        <w:jc w:val="left"/><w:outlineLvl w:val="1"/></w:pPr><w:rPr><w:b/><w:sz w:val="26"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/><w:basedOn w:val="Normal"/>
        <w:next w:val="BodyText"/><w:pPr><w:keepNext/><w:spacing w:before="240" w:after="120"/>
        <w:jc w:val="left"/><w:outlineLvl w:val="2"/></w:pPr><w:rPr><w:b/><w:sz w:val="23"/></w:rPr></w:style>
        <w:style w:type="paragraph" w:styleId="Bullet"><w:name w:val="Bullet"/><w:basedOn w:val="Normal"/>
        <w:pPr><w:ind w:left="340" w:hanging="340"/><w:jc w:val="left"/></w:pPr></w:style>
        </w:styles>
    """.trimIndent()

    fun bookDocx(book: Book, chapters: List<Chapter>): ByteArray {
        val title = book.title.ifBlank { "Tanpa judul" }
        val sections = sections(book, chapters)
            .ifEmpty { listOf(TypesetSection(title, book.tags.joinToString(" · "))) }
        return docx(title, sections, withTitlePage = true)
    }

    fun chapterDocx(chapter: Chapter): ByteArray =
        docx(chapter.title.ifBlank { "Tanpa judul" }, listOf(TypesetSection(chapter.title, chapter.body)), withTitlePage = false)

    private fun docx(title: String, sections: List<TypesetSection>, withTitlePage: Boolean): ByteArray {
        val body = StringBuilder()

        fun paragraph(text: String, style: String) {
            body.append("<w:p><w:pPr><w:pStyle w:val=\"").append(style).append("\"/></w:pPr>")
            body.append("<w:r><w:t xml:space=\"preserve\">").append(xml(text)).append("</w:t></w:r></w:p>")
        }

        if (withTitlePage) paragraph(title, "Title")

        sections.forEach { section ->
            if (section.startsVolume) {
                section.volume?.takeIf { it.isNotBlank() }?.let { paragraph(it, "Heading1") }
            }
            paragraph(section.title.ifBlank { "Tanpa judul" }, "Heading1")

            val buffer = StringBuilder()
            fun flushParagraph() {
                val text = buffer.toString().trim()
                buffer.setLength(0)
                if (text.isNotEmpty()) paragraph(text, "BodyText")
            }
            section.body.split("\n").forEach { raw ->
                val line = raw.trim()
                when {
                    line.isEmpty() -> flushParagraph()
                    line.startsWith("###") -> {
                        flushParagraph()
                        paragraph(plainMarkdown(line.trimStart('#').trim()), "Heading3")
                    }
                    line.startsWith("#") -> {
                        flushParagraph()
                        paragraph(plainMarkdown(line.trimStart('#').trim()), "Heading2")
                    }
                    isBullet(line) -> {
                        flushParagraph()
                        paragraph("\u2022  " + plainMarkdown(line.drop(2).trim()), "Bullet")
                    }
                    else -> {
                        if (buffer.isNotEmpty()) buffer.append(' ')
                        buffer.append(plainMarkdown(line))
                    }
                }
            }
            flushParagraph()
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
                    "vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                    "<Override PartName=\"/word/styles.xml\" ContentType=\"application/" +
                    "vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/>" +
                    "<Override PartName=\"/word/footer1.xml\" ContentType=\"application/" +
                    "vnd.openxmlformats-officedocument.wordprocessingml.footer+xml\"/></Types>",
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
                "word/_rels/document.xml.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/" +
                    "relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/" +
                    "2006/relationships/styles\" Target=\"styles.xml\"/>" +
                    "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/" +
                    "2006/relationships/footer\" Target=\"footer1.xml\"/></Relationships>",
            )
            put("word/styles.xml", docxStyles)
            put(
                "word/footer1.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<w:ftr xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">" +
                    "<w:p><w:pPr><w:jc w:val=\"center\"/><w:rPr><w:sz w:val=\"18\"/></w:rPr></w:pPr>" +
                    "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r>" +
                    "<w:r><w:instrText xml:space=\"preserve\"> PAGE </w:instrText></w:r>" +
                    "<w:r><w:fldChar w:fldCharType=\"separate\"/></w:r>" +
                    "<w:r><w:t>1</w:t></w:r>" +
                    "<w:r><w:fldChar w:fldCharType=\"end\"/></w:r></w:p></w:ftr>",
            )
            put(
                "word/document.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
                    "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<w:body>$body" +
                    "<w:sectPr><w:footerReference w:type=\"default\" r:id=\"rId2\"/>" +
                    "<w:pgSz w:w=\"11906\" w:h=\"16838\"/>" +
                    "<w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" " +
                    "w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/>" +
                    "<w:docGrid w:linePitch=\"360\"/></w:sectPr>" +
                    "</w:body></w:document>",
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
