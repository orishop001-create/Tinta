package id.tinta.app.data

import android.content.Context
import android.util.AtomicFile
import id.tinta.app.util.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TintaStore(context: Context) {
    private val file = File(context.filesDir, "tinta.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null
    private val writeLock = Any()

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private fun load(): AppData {
        if (!file.exists()) {
            val seeded = Seed.create()
            saveNow(seeded)
            return seeded
        }
        return try {
            json.decodeFromString(AppData.serializer(), file.readText())
        } catch (_: Exception) {
            runCatching { file.copyTo(File(file.parentFile, "tinta-rusak.json"), overwrite = true) }
            val seeded = Seed.create()
            saveNow(seeded)
            seeded
        }
    }

    private fun encode(data: AppData): String =
        json.encodeToString(AppData.serializer(), data)

    private fun saveNow(data: AppData) {
        synchronized(writeLock) {
            val atomic = AtomicFile(file)
            val stream = atomic.startWrite()
            try {
                stream.write(encode(data).toByteArray(Charsets.UTF_8))
                atomic.finishWrite(stream)
            } catch (error: Exception) {
                atomic.failWrite(stream)
                throw error
            }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(400)
            runCatching { saveNow(_state.value) }
        }
    }

    fun flush() {
        saveJob?.cancel()
        runCatching { synchronized(writeLock) { saveNow(_state.value) } }
    }

    fun update(transform: (AppData) -> AppData) {
        val next = transform(_state.value)
        _state.value = next
        scheduleSave()
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun chapterOf(data: AppData, id: String): Chapter? =
        data.chapters.firstOrNull { it.id == id && it.deletedAt == null }

    fun book(id: String): Book? = _state.value.books.firstOrNull { it.id == id }

    fun chapter(id: String): Chapter? = chapterOf(_state.value, id)

    fun addWords(daily: List<DayStat>, delta: Int): List<DayStat> {
        if (delta <= 0) return daily
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = daily.firstOrNull { it.date == today }
        return if (existing == null) daily + DayStat(today, delta)
        else daily.map { if (it.date == today) it.copy(words = it.words + delta) else it }
    }

    fun createBook(title: String, tags: List<String>, cover: Int): String {
        val stamp = now()
        val id = UUID.randomUUID().toString()
        update { data ->
            data.copy(
                books = data.books + Book(
                    id = id,
                    title = title.ifBlank { "Tanpa judul" },
                    tags = tags.map { it.trim() }.filter { it.isNotEmpty() }.take(6),
                    cover = cover,
                    createdAt = stamp,
                    updatedAt = stamp,
                )
            )
        }
        return id
    }

    fun updateBook(id: String, title: String, tags: List<String>, cover: Int) {
        update { data ->
            data.copy(
                books = data.books.map {
                    if (it.id == id) it.copy(
                        title = title.ifBlank { "Tanpa judul" },
                        tags = tags.map { tag -> tag.trim() }.filter { tag -> tag.isNotEmpty() }.take(6),
                        cover = cover,
                        updatedAt = now(),
                    ) else it
                }
            )
        }
    }

    fun deleteBook(id: String) {
        val chapterIds = _state.value.chapters.filter { it.bookId == id }.map { it.id }.toSet()
        update { data ->
            data.copy(
                books = data.books.filterNot { it.id == id },
                chapters = data.chapters.filterNot { it.bookId == id },
                snapshots = data.snapshots.filterNot { it.chapterId in chapterIds },
                settings = data.settings.copy(
                    lastDocId = data.settings.lastDocId?.takeIf { it !in chapterIds }
                ),
            )
        }
    }

    fun addVolume(bookId: String, title: String) {
        update { data ->
            data.copy(
                books = data.books.map {
                    if (it.id == bookId) it.copy(
                        volumes = it.volumes + Volume(UUID.randomUUID().toString(), title.ifBlank { "Bagian baru" }),
                        updatedAt = now(),
                    ) else it
                }
            )
        }
    }

    fun deleteVolume(bookId: String, volumeId: String) {
        update { data ->
            data.copy(
                books = data.books.map {
                    if (it.id == bookId) it.copy(
                        volumes = it.volumes.filterNot { volume -> volume.id == volumeId },
                        updatedAt = now(),
                    ) else it
                },
                chapters = data.chapters.map {
                    if (it.bookId == bookId && it.volumeId == volumeId) it.copy(volumeId = null) else it
                },
            )
        }
    }

    fun createChapter(bookId: String, volumeId: String?, title: String): String {
        val stamp = now()
        val id = UUID.randomUUID().toString()
        update { data ->
            val order = (data.chapters.filter { it.bookId == bookId }.maxOfOrNull { it.order } ?: -1) + 1
            data.copy(
                chapters = data.chapters + Chapter(
                    id = id,
                    bookId = bookId,
                    volumeId = volumeId,
                    title = title.ifBlank { "Bab tanpa judul" },
                    order = order,
                    createdAt = stamp,
                    updatedAt = stamp,
                ),
                books = data.books.map {
                    if (it.id == bookId) it.copy(updatedAt = stamp) else it
                },
                settings = data.settings.copy(lastDocId = id),
            )
        }
        return id
    }

    fun updateChapterTitle(id: String, title: String) {
        if (chapterOf(_state.value, id) == null) return
        val stamp = now()
        update { data ->
            data.copy(
                chapters = data.chapters.map {
                    if (it.id == id) it.copy(title = title.ifBlank { "Bab tanpa judul" }, updatedAt = stamp) else it
                },
                books = data.books.map {
                    if (it.id == chapterOf(data, id)?.bookId) it.copy(updatedAt = stamp) else it
                },
                settings = data.settings.copy(lastDocId = id),
            )
        }
    }

    fun updateChapterBody(id: String, body: String) {
        val data = _state.value
        val chapter = chapterOf(data, id) ?: return
        if (chapter.body == body) return
        val delta = (Text.wordCount(body) - Text.wordCount(chapter.body)).coerceAtLeast(0)
        val stamp = now()
        update { current ->
            current.copy(
                chapters = current.chapters.map {
                    if (it.id == id) it.copy(body = body, updatedAt = stamp) else it
                },
                daily = addWords(current.daily, delta),
                books = current.books.map {
                    if (it.id == chapter.bookId) it.copy(updatedAt = stamp) else it
                },
                settings = current.settings.copy(lastDocId = id),
            )
        }
    }

    fun toggleMarkdown(id: String) {
        update { data ->
            data.copy(chapters = data.chapters.map {
                if (it.id == id) it.copy(markdown = !it.markdown) else it
            })
        }
    }

    fun moveChapter(id: String, volumeId: String?) {
        update { data ->
            data.copy(chapters = data.chapters.map {
                if (it.id == id) it.copy(volumeId = volumeId, updatedAt = now()) else it
            })
        }
    }

    fun duplicateChapter(id: String): String? {
        val data = _state.value
        val source = chapterOf(data, id) ?: return null
        val stamp = now()
        val newId = UUID.randomUUID().toString()
        update { current ->
            val order = (current.chapters.filter { it.bookId == source.bookId }.maxOfOrNull { it.order } ?: -1) + 1
            current.copy(
                chapters = current.chapters + source.copy(
                    id = newId,
                    title = "${source.title} (salinan)",
                    order = order,
                    createdAt = stamp,
                    updatedAt = stamp,
                ),
                settings = current.settings.copy(lastDocId = newId),
            )
        }
        return newId
    }

    fun deleteChapter(id: String) {
        val stamp = now()
        update { data ->
            data.copy(
                chapters = data.chapters.map {
                    if (it.id == id) it.copy(deletedAt = stamp) else it
                },
                settings = data.settings.copy(lastDocId = data.settings.lastDocId?.takeIf { it != id }),
            )
        }
    }

    fun restoreChapter(id: String) {
        update { data ->
            data.copy(chapters = data.chapters.map {
                if (it.id == id) it.copy(deletedAt = null, updatedAt = now()) else it
            })
        }
    }

    fun purgeChapter(id: String) {
        update { data ->
            data.copy(
                chapters = data.chapters.filterNot { it.id == id },
                snapshots = data.snapshots.filterNot { it.chapterId == id },
                settings = data.settings.copy(lastDocId = data.settings.lastDocId?.takeIf { it != id }),
            )
        }
    }

    fun openChapter(id: String) {
        val chapter = chapterOf(_state.value, id) ?: return
        snapshotIfChanged(id)
        update { data -> data.copy(settings = data.settings.copy(lastDocId = id)) }
    }

    private fun withSnapshot(data: AppData, chapter: Chapter, kind: String): AppData {
        if (chapter.body.isBlank()) return data
        val latest = data.snapshots.filter { it.chapterId == chapter.id }.maxByOrNull { it.createdAt }
        if (kind == "auto" && latest?.body == chapter.body) return data
        val snapshot = Snapshot(
            id = UUID.randomUUID().toString(),
            chapterId = chapter.id,
            body = chapter.body,
            kind = kind,
            createdAt = now(),
        )
        val others = data.snapshots.filterNot { it.chapterId == chapter.id }
        val kept = (listOf(snapshot) + data.snapshots.filter { it.chapterId == chapter.id })
            .sortedByDescending { it.createdAt }
            .take(40)
        return data.copy(snapshots = others + kept)
    }

    fun snapshotIfChanged(id: String) {
        val data = _state.value
        val chapter = chapterOf(data, id) ?: return
        update { current -> withSnapshot(current, chapter, "auto") }
    }

    fun manualSnapshot(id: String) {
        val chapter = chapterOf(_state.value, id) ?: return
        update { data -> withSnapshot(data, chapter, "manual") }
    }

    fun snapshotsFor(id: String): List<Snapshot> =
        _state.value.snapshots.filter { it.chapterId == id }.sortedByDescending { it.createdAt }

    fun restoreSnapshot(snapshotId: String) {
        val data = _state.value
        val snapshot = data.snapshots.firstOrNull { it.id == snapshotId } ?: return
        val chapter = chapterOf(data, snapshot.chapterId) ?: return
        update { current ->
            val safe = withSnapshot(current, chapter, "sebelum-pulih")
            safe.copy(
                chapters = safe.chapters.map {
                    if (it.id == chapter.id) it.copy(body = snapshot.body, updatedAt = now()) else it
                },
            )
        }
    }

    fun replaceAll(
        query: String,
        replacement: String,
        regex: Boolean,
        caseSensitive: Boolean,
        bookId: String?,
    ): Int {
        if (query.isEmpty()) return 0
        val pattern = try {
            val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            if (regex) Regex(query, options) else Regex(Regex.escape(query), options)
        } catch (_: Exception) {
            return -1
        }
        var count = 0
        update { data ->
            val changed = mutableListOf<Chapter>()
            val kept = data.chapters.map { chapter ->
                if (chapter.deletedAt != null || (bookId != null && chapter.bookId != bookId)) return@map chapter
                val matches = pattern.findAll(chapter.body).count()
                if (matches == 0) return@map chapter
                count += matches
                val updated = chapter.copy(
                    body = pattern.replace(chapter.body, replacement),
                    updatedAt = now(),
                )
                changed += chapter
                updated
            }
            if (changed.isEmpty()) return@update data
            var next = data.copy(chapters = kept)
            changed.forEach { next = withSnapshot(next, it, "sebelum-ganti") }
            next
        }
        return count
    }

    fun updateSettings(transform: (Settings) -> Settings) {
        update { data -> data.copy(settings = transform(data.settings)) }
    }

    fun addPhrase(phrase: String) {
        if (phrase.isBlank()) return
        updateSettings { settings ->
            settings.copy(phrases = (settings.phrases + phrase.trim()).distinct().take(20))
        }
    }

    fun removePhrase(phrase: String) {
        updateSettings { settings -> settings.copy(phrases = settings.phrases - phrase) }
    }

    fun exportJson(): String = json.encodeToString(
        AppData.serializer(),
        _state.value.copy(settings = _state.value.settings.copy(lastDocId = null))
    )

    fun importJson(text: String): Result<Unit> = runCatching {
        val parsed = json.decodeFromString(AppData.serializer(), text)
        require(parsed.schema == 1) { "Versi cadangan tidak dikenal." }
        require(parsed.books.isNotEmpty() || parsed.chapters.isEmpty()) { "Cadangan tidak berisi buku." }
        saveNow(parsed)
        _state.value = parsed
    }

    fun resetAll() {
        val empty = AppData(settings = Settings())
        saveNow(empty)
        _state.value = empty
    }
}
