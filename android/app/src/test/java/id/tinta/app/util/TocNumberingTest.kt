package id.tinta.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TocNumberingTest {

    private val metrics = TextMetrics { text, size, _ -> text.length * size * 0.5f }

    private fun body(words: Int): String = (0 until words).joinToString(" ") { "kata$it" }

    @Test
    fun `nomor daftar isi sama dengan halaman awal tiap bab`() {
        val sections = listOf(
            TypesetSection("Bab 1 — Kereta Terlambat", body(40), "Bagian I — Peren", true),
            TypesetSection("Bab 2 — Payung Pinjaman", body(40), "Bagian I — Peren", false),
            TypesetSection("Bab 3 — Surat Tanpa Alamat", body(40), "Bagian II — Surat", true),
        )
        val pages = Typesetter(metrics, PrintFormat()).typeset("Hujan di Kota Tua", sections)
        val start = pages.indexOfFirst { it.folio == "1" }

        val toc = pages.flatMap { page -> page.lines.filter { it.folio != null } }
            .associate { line -> line.words.joinToString(" ") to line.folio!! }

        // Halaman awal setiap judul bab di badan buku.
        val actual = sections.associate { section ->
            val index = pages.indexOfFirst { page ->
                page.lines.any { it.bold && it.size == 17f && it.words.joinToString(" ") == section.title }
            }
            section.title to (index - start + 1).toString()
        }

        assertEquals(actual["Bab 1 — Kereta Terlambat"], toc["Bab 1 — Kereta Terlambat"])
        assertEquals(actual["Bab 2 — Payung Pinjaman"], toc["Bab 2 — Payung Pinjaman"])
        assertEquals(actual["Bab 3 — Surat Tanpa Alamat"], toc["Bab 3 — Surat Tanpa Alamat"])
        // Judul bagian berbagi halaman dengan bab pertama di bagian itu.
        assertEquals(toc["Bab 1 — Kereta Terlambat"], toc["Bagian I — Peren"])
        assertEquals(toc["Bab 3 — Surat Tanpa Alamat"], toc["Bagian II — Surat"])
    }
}
