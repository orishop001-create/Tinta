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
    private const val MARGIN = 46f

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

        fun nextPage() {
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

        drawParagraph(book.title.ifBlank { "Tanpa judul" }, 20f, true, 28f)
        if (book.tags.isNotEmpty()) {
            drawParagraph(book.tags.joinToString(" · "), 10f, false, 22f)
        }
        y += 6f

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

        document.finishPage(page)
        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }
}
