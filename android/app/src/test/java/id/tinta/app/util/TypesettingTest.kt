package id.tinta.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypesettingTest {

    private val metrics = TextMetrics { text, size, _ -> text.length * size * 0.5f }
    private val format = PrintFormat()
    private val typesetter = Typesetter(metrics, format)

    private fun body(words: Int, seed: Int = 0): String =
        (0 until words).joinToString(" ") { "kata${(it + seed) % 23}" }

    private fun bodyStart(pages: List<TypesetPage>): Int = pages.indexOfFirst { it.folio == "1" }

    @Test
    fun `halaman isi diberi nomor urut dan header`() {
        val pages = typesetter.typeset("Buku Contoh", listOf(TypesetSection("Bab Satu", body(600))))
        val start = bodyStart(pages)
        assertTrue("harus ada halaman judul dan daftar isi", start > 0)
        val numbered = pages.drop(start)
        assertTrue("naskah panjang harus pecah jadi beberapa halaman", numbered.size >= 2)
        numbered.forEachIndexed { index, page ->
            assertEquals((index + 1).toString(), page.folio)
            assertEquals("BUKU CONTOH", page.runningTitle)
        }
    }

    @Test
    fun `setiap bab mulai di halaman baru`() {
        val pages = typesetter.typeset(
            "Buku",
            listOf(
                TypesetSection("Bab Satu", body(40)),
                TypesetSection("Bab Dua", body(40)),
            ),
        )
        val headingPages = pages.indices.filter { index ->
            pages[index].lines.any { it.bold && it.size == 17f }
        }
        assertEquals(2, headingPages.size)
        assertTrue("bab kedua harus di halaman setelah bab pertama", headingPages[0] < headingPages[1])
    }

    @Test
    fun `daftar isi memakai nomor halaman hasil pemotongan`() {
        val pages = typesetter.typeset(
            "Buku",
            listOf(
                TypesetSection("Bab Satu", body(200)),
                TypesetSection("Bab Dua", body(30)),
            ),
        )
        val start = bodyStart(pages)
        val tocLine = pages.flatMap { it.lines }
            .first { it.folio != null && it.words.joinToString(" ") == "Bab Dua" }
        val headingPage = pages.indexOfFirst { page ->
            page.lines.any { it.bold && it.size == 17f && it.words.joinToString(" ") == "Bab Dua" }
        }
        assertTrue(headingPage > start)
        assertEquals((headingPage - start + 1).toString(), tocLine.folio)
    }

    @Test
    fun `baris isi tidak melebihi lebar teks`() {
        val pages = typesetter.typeset("Buku", listOf(TypesetSection("Bab", body(500))))
        pages.filter { it.folio != null }.forEach { page ->
            page.lines.filter { it.size == 11f }.forEach { line ->
                val words = line.words.sumOf { metrics.width(it, line.size, line.bold).toDouble() }.toFloat()
                val gaps = (line.words.size - 1).coerceAtLeast(0) * metrics.width(" ", line.size, line.bold)
                val available = format.pageWidth - format.marginRight - line.x
                assertTrue(
                    "baris melebihi lebar teks: ${words + gaps} > $available",
                    words + gaps <= available + 0.5f,
                )
            }
        }
    }

    @Test
    fun `judul tidak tertinggal sendirian di dasar halaman`() {
        val pages = typesetter.typeset("Buku", listOf(TypesetSection("Bab", body(1200))))
        pages.forEach { page ->
            page.lines
                .filter { it.bold && it.folio == null && it.size >= 11.5f }
                .forEach { line ->
                    assertTrue(
                        "judul terlalu dekat dasar halaman: ${line.baseline}",
                        line.baseline + 17f * 2f <= format.pageHeight - format.marginBottom + 0.5f,
                    )
                }
        }
    }

    @Test
    fun `penanda markdown dibuang dari teks cetak`() {
        assertEquals("Judul miring", plainMarkdown("**Judul** *miring*"))
        assertEquals("tautan", plainMarkdown("[tautan](https://contoh.id)"))
    }
}
