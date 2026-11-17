package id.tinta.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Urutan tombol < > harus sama dengan urutan bab yang terlihat di layar buku. */
class ReadingOrderTest {

    private fun chapter(id: String, title: String, volumeId: String?, order: Int, createdAt: Long) =
        Chapter(
            id = id,
            bookId = "b",
            volumeId = volumeId,
            title = title,
            order = order,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    private val book = Book(
        id = "b",
        title = "Hujan di Kota Tua",
        volumes = listOf(
            Volume("v1", "Bagian I"),
            Volume("v2", "Bagian II"),
        ),
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun urutanMengikutiBagianBukanNomorGlobal() {
        // Order hanya unik di dalam bagian: bab 3 punya order 0 di bagian II, jadi pengurutan
        // global dengan `order` saja menaruhnya sebelum bab 2.
        val list = listOf(
            chapter("3", "Bab 3", "v2", 0, 30),
            chapter("2", "Bab 2", "v1", 1, 20),
            chapter("1", "Bab 1", "v1", 0, 10),
        )
        assertEquals(listOf("Bab 1", "Bab 2", "Bab 3"), readingOrder(book, list).map { it.title })
    }

    @Test
    fun babTanpaBagianDitaruhPalingAkhir() {
        val list = listOf(
            chapter("x", "Tanpa bagian", null, 0, 5),
            chapter("1", "Bab 1", "v1", 0, 10),
        )
        assertEquals(listOf("Bab 1", "Tanpa bagian"), readingOrder(book, list).map { it.title })
    }

    @Test
    fun bagianTanpaBabTetapStabil() {
        val list = listOf(
            chapter("2", "Bab 2", "v1", 1, 20),
            chapter("1", "Bab 1", "v1", 0, 10),
            chapter("3", "Bab 3", "v2", 0, 30),
        )
        assertEquals(listOf("Bab 1", "Bab 2", "Bab 3"), readingOrder(book, list).map { it.title })
    }
}
