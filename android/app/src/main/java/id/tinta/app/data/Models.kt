package id.tinta.app.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

val DEFAULT_PHRASES = listOf("“", "”", "—", "…", "Tiba-tiba,", "Sementara itu,")

@Serializable
@Immutable
data class Volume(
    val id: String,
    val title: String,
    val coverImage: String? = null,
    val coverRatio: String = "2:3",
)

@Serializable
@Immutable
data class Book(
    val id: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val cover: Int = 0,
    val coverImage: String? = null,
    val coverRatio: String = "2:3",
    val volumes: List<Volume> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Urutan baca yang sama dengan tampilan buku: bagian sesuai urutan tampil, lalu bab di dalam
 * bagian itu. `order` hanya unik di dalam satu bagian, jadi mengurutkan seluruh bab buku dengan
 * `order` saja bisa menghasilkan urutan yang berbeda dari daftar yang dilihat penulis.
 */
fun readingOrder(book: Book?, chapters: List<Chapter>): List<Chapter> {
    val volumeIndex = book?.volumes
        ?.mapIndexed { index, volume -> volume.id to index }
        ?.toMap()
        .orEmpty()
    return chapters.sortedWith(
        compareBy<Chapter> { chapter ->
            chapter.volumeId?.let { volumeIndex[it] } ?: Int.MAX_VALUE
        }
            .thenBy { it.order }
            .thenBy { it.createdAt }
            .thenBy { it.id }
    )
}

@Serializable
@Immutable
data class Chapter(
    val id: String,
    val bookId: String,
    val volumeId: String? = null,
    val title: String,
    val body: String = "",
    val markdown: Boolean = true,
    /** ide, draf, revisi, atau selesai; dipakai papan cerita. */
    val status: String = "draf",
    val synopsis: String = "",
    val order: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val coverImage: String? = null,
    val coverRatio: String = "2:3",
)

@Serializable
@Immutable
data class Snapshot(
    val id: String,
    val chapterId: String,
    val body: String,
    val kind: String = "auto",
    val createdAt: Long,
)

@Serializable
@Immutable
data class DayStat(
    val date: String,
    val words: Int,
)

/** Satu kartu di Story Bible: karakter, lokasi, atau istilah dunia cerita. */
@Serializable
@Immutable
data class LoreEntry(
    val id: String,
    val bookId: String,
    val kind: String = KIND_CHARACTER,
    val name: String,
    val summary: String = "",
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        const val KIND_CHARACTER = "karakter"
        const val KIND_PLACE = "lokasi"
        const val KIND_TERM = "istilah"
        val kinds = listOf(KIND_CHARACTER, KIND_PLACE, KIND_TERM)

        fun kindLabel(kind: String): String = when (kind) {
            KIND_PLACE -> "Lokasi"
            KIND_TERM -> "Istilah"
            else -> "Karakter"
        }
    }
}

/** Catatan pinggir untuk satu kutipan; teks utama tidak ikut diubah. */
@Serializable
@Immutable
data class MarginNote(
    val id: String,
    val bookId: String,
    val chapterId: String,
    val quote: String = "",
    val body: String,
    val createdAt: Long,
    val resolved: Boolean = false,
)

@Serializable
@Immutable
data class Settings(
    val theme: String = "kertas",
    val fontSize: Float = 19f,
    val lineHeight: Float = 1.75f,
    val font: String = "serif",
    val dailyGoal: Int = 500,
    val phrases: List<String> = DEFAULT_PHRASES,
    val lastDocId: String? = null,
    val chapterGoal: Int = 1000,
    val typewriter: Boolean = false,
    val lastBackupAt: Long = 0L,
    val lock: Boolean = false,
    val backupFolder: String? = null,
    val fontFile: String? = null,
    val exportStyle: String = "buku",
    val spellcheck: Boolean = true,
    val dictionary: List<String> = emptyList(),
)

@Serializable
@Immutable
data class AppData(
    val schema: Int = 1,
    val books: List<Book> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val snapshots: List<Snapshot> = emptyList(),
    val daily: List<DayStat> = emptyList(),
    val settings: Settings = Settings(),
    val lore: List<LoreEntry> = emptyList(),
    val notes: List<MarginNote> = emptyList(),
)
