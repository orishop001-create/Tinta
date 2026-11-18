package id.tinta.app.util

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportTest {

    private fun zip(files: Map<String, String>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            files.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    @Test
    fun `markdown memakai judul buku dan bab level dua`() {
        val book = Import.fromText(
            "# Hujan di Kota Tua\n\n## Bab Satu\nIsi satu.\n\n## Bab Dua\nIsi dua.\n",
            "hujan.md",
        )
        assertEquals("Hujan di Kota Tua", book.title)
        assertEquals(listOf("Bab Satu", "Bab Dua"), book.chapters.map { it.title })
        assertEquals("Isi satu.", book.chapters[0].body)
    }

    @Test
    fun `epub mengikuti spine dan tidak mengulang judul di isi`() {
        val bytes = zip(
            mapOf(
                "META-INF/container.xml" to
                    "<container><rootfiles><rootfile full-path=\"OEBPS/content.opf\"/></rootfiles></container>",
                "OEBPS/content.opf" to """
                    <package><metadata><dc:title>Hujan</dc:title></metadata><manifest>
                    <item id="c1" href="Text/bab1.xhtml" media-type="application/xhtml+xml"/>
                    </manifest><spine><itemref idref="c1"/></spine></package>
                """.trimIndent(),
                "OEBPS/Text/bab1.xhtml" to
                    "<html><body><h2>Bab Satu</h2><p>Isi bab.</p></body></html>",
            )
        )
        val book = Import.fromEpub(bytes, "hujan.epub")
        assertEquals("Hujan", book.title)
        assertEquals(1, book.chapters.size)
        assertEquals("Bab Satu", book.chapters[0].title)
        assertEquals("Isi bab.", book.chapters[0].body)
        assertFalse(book.chapters[0].body.contains("Bab Satu"))
    }

    @Test
    fun `docx memakai style heading sebagai batas bab`() {
        val document = """
            <w:document><w:body>
            <w:p><w:pPr><w:pStyle w:val="Title"/></w:pPr><w:r><w:t>Hujan</w:t></w:r></w:p>
            <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Bab Satu</w:t></w:r></w:p>
            <w:p><w:r><w:t>Isi bab.</w:t></w:r></w:p>
            </w:body></w:document>
        """.trimIndent()
        val book = Import.fromDocx(zip(mapOf("word/document.xml" to document)), "hujan.docx")
        assertEquals("Hujan", book.title)
        assertEquals(1, book.chapters.size)
        assertEquals("Bab Satu", book.chapters[0].title)
        assertEquals("Isi bab.", book.chapters[0].body)
            .also { assertTrue(book.chapters[0].body.isNotBlank()) }
    }
}
