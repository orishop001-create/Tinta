package id.tinta.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.AtomicFile
import id.tinta.app.util.Covers
import id.tinta.app.util.Crypto
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
    companion object {
        const val BACKUP_FILE_NAME = "tinta-cadangan.json"

        @Volatile
        private var shared: TintaStore? = null

        /**
         * Satu instance per proses. Rotasi layar tidak boleh membuat store kedua yang memuat ulang
         * pustaka dari disk saat instance lama belum selesai menulis.
         */
        fun get(context: Context): TintaStore =
            shared ?: synchronized(this) {
                shared ?: TintaStore(context.applicationContext).also { shared = it }
            }
    }

    private val appContext = context.applicationContext
    private val file = File(context.filesDir, "tinta.json")
    private val backupDir: File? = context.getExternalFilesDir("cadangan")
    private val chapterDb = ChapterDb(context)
    private val dirtyBodies = linkedSetOf<String>()
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
            chapterDb.putAll(seeded.chapters)
            saveNow(seeded)
            return seeded
        }
        val parsed = try {
            json.decodeFromString(AppData.serializer(), file.readText())
        } catch (_: Exception) {
            runCatching { file.copyTo(File(file.parentFile, "tinta-rusak.json"), overwrite = true) }
            val seeded = Seed.create()
            chapterDb.putAll(seeded.chapters)
            saveNow(seeded)
            return seeded
        }

        // Cadangan lama masih menyimpan badan naskah di JSON. Pindahkan sekali ke SQLite,
        // lalu tulis ulang JSON tanpa badan naskah supaya berkasnya tetap ringkas.
        val stored = chapterDb.bodies()
        var legacyBodies = false
        val chapters = parsed.chapters.map { chapter ->
            val saved = stored[chapter.id]
            when {
                chapter.body.isNotBlank() -> {
                    if (saved != chapter.body) chapterDb.put(chapter.id, chapter.body)
                    legacyBodies = true
                    chapter
                }
                saved != null -> chapter.copy(body = saved)
                else -> chapter
            }
        }
        val merged = parsed.copy(chapters = chapters)
        if (legacyBodies) runCatching { saveNow(merged) }
        return merged
    }

    private fun encode(data: AppData): String =
        json.encodeToString(AppData.serializer(), data)

    private fun withoutBodies(data: AppData): AppData =
        data.copy(chapters = data.chapters.map { it.copy(body = "") })

    private fun saveNow(data: AppData) {
        synchronized(writeLock) {
            val atomic = AtomicFile(file)
            val stream = atomic.startWrite()
            try {
                stream.write(encode(withoutBodies(data)).toByteArray(Charsets.UTF_8))
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
            runCatching {
                val snapshot = _state.value
                val ids = synchronized(writeLock) {
                    dirtyBodies.toList().also { dirtyBodies.clear() }
                }
                ids.forEach { id ->
                    snapshot.chapters.firstOrNull { it.id == id }?.let { chapter ->
                        chapterDb.put(chapter.id, chapter.body)
                    }
                }
                saveNow(snapshot)
            }
        }
    }

    /** Menyimpan yang tertunda tanpa menunggu debounce, dipakai jalur "Bagikan ke Tinta". */
    private fun persistNow() {
        saveJob?.cancel()
        runCatching {
            val snapshot = _state.value
            val ids = synchronized(writeLock) {
                dirtyBodies.toList().also { dirtyBodies.clear() }
            }
            ids.forEach { id ->
                snapshot.chapters.firstOrNull { it.id == id }?.let { chapter ->
                    chapterDb.put(chapter.id, chapter.body)
                }
            }
            saveNow(snapshot)
        }
    }

    fun flush() {
        saveJob?.cancel()
        runCatching {
            val snapshot = _state.value
            chapterDb.putAll(snapshot.chapters)
            synchronized(writeLock) { dirtyBodies.clear() }
            synchronized(writeLock) { saveNow(snapshot) }
        }
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
        val covers = buildList {
            _state.value.books.filter { it.id == id }.forEach { book ->
                book.coverImage?.let(::add)
                book.volumes.forEach { volume -> volume.coverImage?.let(::add) }
            }
            _state.value.chapters.filter { it.bookId == id }.forEach { chapter ->
                chapter.coverImage?.let(::add)
            }
        }
        update { data ->
            data.copy(
                books = data.books.filterNot { it.id == id },
                chapters = data.chapters.filterNot { it.bookId == id },
                snapshots = data.snapshots.filterNot { it.chapterId in chapterIds },
                lore = data.lore.filterNot { it.bookId == id },
                notes = data.notes.filterNot { it.bookId == id || it.chapterId in chapterIds },
                settings = data.settings.copy(
                    lastDocId = data.settings.lastDocId?.takeIf { it !in chapterIds }
                ),
            )
        }
        chapterDb.delete(chapterIds)
        covers.forEach(::dropCover)
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
        val cover = _state.value.books
            .firstOrNull { it.id == bookId }
            ?.volumes
            ?.firstOrNull { it.id == volumeId }
            ?.coverImage
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
        dropCover(cover)
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

    /** Membuat buku dari naskah impor: satu bab per judul yang ditemukan. */
    fun importBook(title: String, chapters: List<Pair<String, String>>): String {
        val bookId = createBook(title, emptyList(), 0)
        chapters.forEach { (chapterTitle, body) ->
            val id = createChapter(bookId, null, chapterTitle)
            if (body.isNotBlank()) updateChapterBody(id, body)
        }
        return bookId
    }

    /** Menampung teks yang dibagikan dari aplikasi lain ke buku "Kotak ide". */
    fun captureIdea(body: String): String {
        val existing = _state.value.books.firstOrNull { it.title.equals("Kotak ide", ignoreCase = true) }
        val bookId = existing?.id ?: createBook("Kotak ide", listOf("ide"), 0)
        val count = _state.value.chapters.count { it.bookId == bookId && it.deletedAt == null }
        val chapterId = createChapter(bookId, null, "Tempelan ${count + 1}")
        updateChapterBody(chapterId, body.trim())
        // Tulis langsung: aplikasi bisa ditutup sebelum debounce 400 ms selesai.
        persistNow()
        return bookId
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
        synchronized(writeLock) { dirtyBodies.add(id) }
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

    /** Geser urutan bab sesuai daftar id yang sudah disusun pengguna. */
    fun reorderChapters(bookId: String, orderedIds: List<String>) {
        if (orderedIds.isEmpty()) return
        val stamp = now()
        val position = orderedIds.withIndex().associate { it.value to it.index }
        update { data ->
            data.copy(
                chapters = data.chapters.map { chapter ->
                    if (chapter.bookId == bookId && position.containsKey(chapter.id)) {
                        chapter.copy(order = position.getValue(chapter.id), updatedAt = stamp)
                    } else {
                        chapter
                    }
                },
                books = data.books.map {
                    if (it.id == bookId) it.copy(updatedAt = stamp) else it
                },
            )
        }
    }

    private fun dropCover(name: String?) {
        if (name.isNullOrBlank()) return
        runCatching { Covers.delete(appContext, name) }
    }

    /** Hapus salinan sampul yang tidak dipakai siapa pun; berkas aktif selalu aman. */
    fun discardUnusedCover(name: String?) {
        if (name.isNullOrBlank()) return
        val data = _state.value
        val used = data.books.any { it.coverImage == name } ||
            data.books.any { book -> book.volumes.any { it.coverImage == name } } ||
            data.chapters.any { it.coverImage == name }
        if (!used) dropCover(name)
    }

    fun setBookCover(id: String, image: String?, ratio: String = "2:3") {
        val before = _state.value.books.firstOrNull { it.id == id }?.coverImage
        val stamp = now()
        update { data ->
            data.copy(
                books = data.books.map {
                    if (it.id == id) it.copy(coverImage = image, coverRatio = ratio, updatedAt = stamp) else it
                },
            )
        }
        if (before != image) dropCover(before)
    }

    fun setVolumeCover(bookId: String, volumeId: String, image: String?, ratio: String = "2:3") {
        val before = _state.value.books
            .firstOrNull { it.id == bookId }
            ?.volumes
            ?.firstOrNull { it.id == volumeId }
            ?.coverImage
        val stamp = now()
        update { data ->
            data.copy(
                books = data.books.map { book ->
                    if (book.id == bookId) {
                        book.copy(
                            volumes = book.volumes.map { volume ->
                                if (volume.id == volumeId) {
                                    volume.copy(coverImage = image, coverRatio = ratio)
                                } else {
                                    volume
                                }
                            },
                            updatedAt = stamp,
                        )
                    } else {
                        book
                    }
                },
            )
        }
        if (before != image) dropCover(before)
    }

    fun setChapterCover(id: String, image: String?, ratio: String = "2:3") {
        val before = _state.value.chapters.firstOrNull { it.id == id }?.coverImage
        val stamp = now()
        update { data ->
            data.copy(
                chapters = data.chapters.map {
                    if (it.id == id) it.copy(coverImage = image, coverRatio = ratio, updatedAt = stamp) else it
                },
            )
        }
        if (before != image) dropCover(before)
    }

    fun setFontFile(name: String?) {
        updateSettings { settings ->
            settings.copy(
                fontFile = name,
                font = if (name == null) "serif" else "custom",
            )
        }
    }

    fun addLore(bookId: String, kind: String, name: String, summary: String): String {
        val stamp = now()
        val id = UUID.randomUUID().toString()
        update { data ->
            data.copy(
                lore = data.lore + LoreEntry(
                    id = id,
                    bookId = bookId,
                    kind = kind,
                    name = name.ifBlank { "Tanpa nama" },
                    summary = summary,
                    createdAt = stamp,
                    updatedAt = stamp,
                ),
            )
        }
        return id
    }

    fun updateLore(id: String, kind: String, name: String, summary: String) {
        update { data ->
            data.copy(
                lore = data.lore.map { entry ->
                    if (entry.id == id) entry.copy(
                        kind = kind,
                        name = name.ifBlank { entry.name },
                        summary = summary,
                        updatedAt = now(),
                    ) else entry
                },
            )
        }
    }

    fun deleteLore(id: String) {
        update { data -> data.copy(lore = data.lore.filterNot { it.id == id }) }
    }

    fun addNote(chapterId: String, quote: String, body: String): String? {
        val chapter = chapterOf(_state.value, chapterId) ?: return null
        if (body.isBlank()) return null
        val stamp = now()
        val id = UUID.randomUUID().toString()
        update { data ->
            data.copy(
                notes = data.notes + MarginNote(
                    id = id,
                    bookId = chapter.bookId,
                    chapterId = chapterId,
                    quote = quote.trim(),
                    body = body.trim(),
                    createdAt = stamp,
                ),
            )
        }
        return id
    }

    fun toggleNote(id: String) {
        update { data ->
            data.copy(
                notes = data.notes.map {
                    if (it.id == id) it.copy(resolved = !it.resolved) else it
                },
            )
        }
    }

    fun deleteNote(id: String) {
        update { data -> data.copy(notes = data.notes.filterNot { it.id == id }) }
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
        val cover = _state.value.chapters.firstOrNull { it.id == id }?.coverImage
        update { data ->
            data.copy(
                chapters = data.chapters.filterNot { it.id == id },
                snapshots = data.snapshots.filterNot { it.chapterId == id },
                notes = data.notes.filterNot { it.chapterId == id },
                settings = data.settings.copy(lastDocId = data.settings.lastDocId?.takeIf { it != id }),
            )
        }
        chapterDb.delete(listOf(id))
        dropCover(cover)
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

    fun movePhrase(phrase: String, delta: Int) {
        updateSettings { settings ->
            val list = settings.phrases.toMutableList()
            val index = list.indexOf(phrase)
            if (index < 0) {
                settings
            } else {
                val target = (index + delta).coerceIn(0, list.lastIndex)
                if (target == index) {
                    settings
                } else {
                    list.removeAt(index)
                    list.add(target, phrase)
                    settings.copy(phrases = list)
                }
            }
        }
    }

    fun markBackupDone() {
        updateSettings { settings -> settings.copy(lastBackupAt = now()) }
    }

    /**
     * Setiap aplikasi ditutup: tulis satu cadangan ke folder pilihan pengguna (Drive,
     * Syncthing, atau folder lokal), lalu simpan juga salinan di folder aplikasi.
     */
    fun autoBackup(keep: Int = 3) {
        runCatching { writeToUserFolder() }
        val dir = backupDir ?: return
        runCatching {
            dir.mkdirs()
            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
                .format(java.util.Date())
            val target = File(dir, "tinta-$stamp.json")
            if (!target.exists()) target.writeText(exportJson())
            dir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json") }
                ?.sortedByDescending { it.name }
                ?.drop(keep)
                ?.forEach { it.delete() }
        }
    }

    /** Tulis cadangan terbaru ke folder yang dipilih lewat SAF. */
    fun backupToUserFolder(): Boolean = runCatching { writeToUserFolder() }.getOrDefault(false)

    private fun writeToUserFolder(): Boolean {
        val folder = _state.value.settings.backupFolder?.let(Uri::parse) ?: return false
        val resolver = appContext.contentResolver
        val treeId = runCatching { DocumentsContract.getTreeDocumentId(folder) }.getOrNull() ?: return false
        val directory = DocumentsContract.buildDocumentUriUsingTree(folder, treeId)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(folder, treeId)
        var existing: Uri? = null
        runCatching {
            resolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == BACKUP_FILE_NAME) {
                        existing = DocumentsContract.buildDocumentUriUsingTree(folder, cursor.getString(0))
                    }
                }
            }
        }
        val target = existing ?: DocumentsContract.createDocument(
            resolver,
            directory,
            "application/json",
            BACKUP_FILE_NAME,
        ) ?: return false
        val payload = exportJson().toByteArray(Charsets.UTF_8)
        resolver.openOutputStream(target, "wt")?.use { it.write(payload) } ?: return false
        markBackupDone()
        return true
    }

    fun exportJson(): String = json.encodeToString(
        AppData.serializer(),
        _state.value.copy(settings = _state.value.settings.copy(lastDocId = null))
    )

    /** Cadangan terenkripsi: PBKDF2 + AES-GCM, seluruhnya di perangkat. */
    fun exportEncrypted(password: String): ByteArray =
        Crypto.encrypt(exportJson().toByteArray(Charsets.UTF_8), password.toCharArray())

    fun importEncrypted(bytes: ByteArray, password: String): Result<Unit> = runCatching {
        val plain = Crypto.decrypt(bytes, password.toCharArray()).toString(Charsets.UTF_8)
        importJson(plain).getOrThrow()
    }

    fun importJson(text: String): Result<Unit> = runCatching {
        val parsed = json.decodeFromString(AppData.serializer(), text)
        require(parsed.schema == 1) { "Versi cadangan tidak dikenal." }
        require(parsed.books.isNotEmpty() || parsed.chapters.isEmpty()) { "Cadangan tidak berisi buku." }
        chapterDb.clear()
        chapterDb.putAll(parsed.chapters)
        saveNow(parsed)
        _state.value = parsed
    }

    fun resetAll() {
        val empty = AppData(settings = Settings())
        chapterDb.clear()
        saveNow(empty)
        _state.value = empty
    }
}
