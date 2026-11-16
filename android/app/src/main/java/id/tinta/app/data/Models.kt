package id.tinta.app.data

import kotlinx.serialization.Serializable

val DEFAULT_PHRASES = listOf("“", "”", "—", "…", "Tiba-tiba,", "Sementara itu,")

@Serializable
data class Volume(
    val id: String,
    val title: String,
)

@Serializable
data class Book(
    val id: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val cover: Int = 0,
    val volumes: List<Volume> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class Chapter(
    val id: String,
    val bookId: String,
    val volumeId: String? = null,
    val title: String,
    val body: String = "",
    val markdown: Boolean = true,
    val order: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

@Serializable
data class Snapshot(
    val id: String,
    val chapterId: String,
    val body: String,
    val kind: String = "auto",
    val createdAt: Long,
)

@Serializable
data class DayStat(
    val date: String,
    val words: Int,
)

@Serializable
data class Settings(
    val theme: String = "kertas",
    val fontSize: Float = 19f,
    val lineHeight: Float = 1.75f,
    val font: String = "serif",
    val dailyGoal: Int = 500,
    val phrases: List<String> = DEFAULT_PHRASES,
    val lastDocId: String? = null,
)

@Serializable
data class AppData(
    val schema: Int = 1,
    val books: List<Book> = emptyList(),
    val chapters: List<Chapter> = emptyList(),
    val snapshots: List<Snapshot> = emptyList(),
    val daily: List<DayStat> = emptyList(),
    val settings: Settings = Settings(),
)
