package id.tinta.app.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.Chapter
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.ConfirmDialog
import id.tinta.app.ui.components.IconAction
import id.tinta.app.ui.components.MarkdownPreview
import id.tinta.app.ui.components.TintaTextField
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun EditorScreen(
    store: TintaStore,
    chapterId: String,
    onBack: () -> Unit,
    onHistory: () -> Unit,
    onOpenChapter: (String) -> Unit,
) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    val chapter = data.chapters.firstOrNull { it.id == chapterId && it.deletedAt == null }
    val book = chapter?.let { current -> data.books.firstOrNull { it.id == current.bookId } }
    val context = LocalContext.current

    if (chapter == null) {
        Scaffold(
            containerColor = colors.bg,
            topBar = { TintaTopBar("Naskah", onBack = onBack) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                Text(
                    "Naskah ini tidak ada atau sudah dipindahkan ke sampah.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.muted,
                )
            }
        }
        return
    }

    var value by remember(chapterId) {
        mutableStateOf(TextFieldValue(chapter.body, TextRange(chapter.body.length)))
    }
    var title by remember(chapterId) { mutableStateOf(chapter.title) }
    var preview by remember(chapterId) { mutableStateOf(false) }
    var focusMode by remember(chapterId) { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showPhraseDialog by remember { mutableStateOf(false) }
    var exportBody by remember { mutableStateOf("") }
    var showToc by remember { mutableStateOf(false) }
    var sprintLeft by remember { mutableStateOf(0L) }
    var sprintRunning by remember { mutableStateOf(false) }
    val undoStack = remember(chapterId) { mutableStateListOf<String>() }
    val redoStack = remember(chapterId) { mutableStateListOf<String>() }
    var lastEditAt by remember(chapterId) { mutableStateOf(0L) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    fun applyEdit(next: TextFieldValue) {
        if (next.text != value.text) {
            val stamp = System.currentTimeMillis()
            if (stamp - lastEditAt > 700) {
                undoStack.add(value.text)
                if (undoStack.size > 60) undoStack.removeAt(0)
                redoStack.clear()
                lastEditAt = stamp
            }
        }
        value = next
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(value.text)
        value = TextFieldValue(previous, TextRange(previous.length))
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(value.text)
        value = TextFieldValue(next, TextRange(next.length))
    }

    val latestText by rememberUpdatedState(value.text)
    val latestTitle by rememberUpdatedState(title)

    DisposableEffect(chapterId) {
        onDispose {
            store.updateChapterBody(chapterId, latestText)
            store.updateChapterTitle(chapterId, latestTitle)
        }
    }

    LaunchedEffect(chapterId) {
        store.snapshotIfChanged(chapterId)
        while (isActive) {
            delay(120_000)
            store.snapshotIfChanged(chapterId)
        }
    }

    LaunchedEffect(sprintRunning) {
        while (sprintRunning && sprintLeft > 0) {
            delay(1000)
            sprintLeft -= 1000
        }
        if (sprintLeft <= 0) sprintRunning = false
    }

    LaunchedEffect(value.selection.end, data.settings.typewriter, scrollState.maxValue) {
        if (!data.settings.typewriter) return@LaunchedEffect
        val caret = value.selection.end.coerceIn(0, value.text.length)
        val lines = value.text.take(caret).count { it == '\n' }
        val lineHeightPx = with(density) {
            (data.settings.fontSize * data.settings.lineHeight).dp.toPx()
        }
        val viewport = scrollState.viewportSize
        if (viewport > 0) {
            val target = (lines * lineHeightPx).toInt() - viewport / 2
            scrollState.animateScrollTo(target.coerceIn(0, scrollState.maxValue))
        }
    }

    LaunchedEffect(value.text) {
        delay(550)
        store.updateChapterBody(chapterId, value.text)
    }
    LaunchedEffect(title) {
        delay(550)
        store.updateChapterTitle(chapterId, title)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(exportBody.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, "Markdown tersimpan.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal menyimpan berkas.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val words = Text.wordCount(value.text)
    val saved = value.text == chapter.body
    val status = if (saved) "Tersimpan" else "Menyimpan…"

    Scaffold(
        containerColor = colors.editor,
        topBar = {
            TintaTopBar(
                title = book?.title ?: "Menulis",
                subtitle = status,
                onBack = {
                    store.updateChapterBody(chapterId, value.text)
                    store.updateChapterTitle(chapterId, title)
                    store.snapshotIfChanged(chapterId)
                    onBack()
                },
                actions = {
                    IconAction(Icons.AutoMirrored.Filled.Undo, "Urungkan") { undo() }
                    IconAction(Icons.AutoMirrored.Filled.Redo, "Ulangi") { redo() }
                    IconAction(Icons.AutoMirrored.Filled.List, "Daftar isi") { showToc = true }
                    IconAction(Icons.Default.History, "Riwayat") {
                        store.updateChapterBody(chapterId, value.text)
                        store.updateChapterTitle(chapterId, title)
                        onHistory()
                    }
                    Box {
                        IconAction(Icons.Default.MoreVert, "Menu naskah") { menu = true }
                        DropdownMenu(
                            expanded = menu,
                            onDismissRequest = { menu = false },
                            containerColor = colors.surface,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Riwayat versi", color = colors.ink) },
                                leadingIcon = { Icon(Icons.Default.History, null, tint = colors.muted) },
                                onClick = {
                                    menu = false
                                    store.updateChapterBody(chapterId, value.text)
                                    onHistory()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (chapter.markdown) "Matikan Markdown" else "Nyalakan Markdown",
                                        color = colors.ink,
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = colors.muted) },
                                onClick = {
                                    menu = false
                                    store.toggleMarkdown(chapterId)
                                    if (chapter.markdown) preview = false
                                },
                            )
                            if (chapter.markdown) {
                                DropdownMenuItem(
                                    text = { Text(if (preview) "Kembali menulis" else "Pratinjau Markdown", color = colors.ink) },
                                    leadingIcon = {
                                        Icon(
                                            if (preview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            null,
                                            tint = colors.muted,
                                        )
                                    },
                                    onClick = {
                                        menu = false
                                        preview = !preview
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (focusMode) "Keluar mode fokus" else "Mode fokus", color = colors.ink) },
                                leadingIcon = { Icon(Icons.Default.VisibilityOff, null, tint = colors.muted) },
                                onClick = {
                                    menu = false
                                    focusMode = !focusMode
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (data.settings.typewriter) "Matikan mode mesin tik" else "Mode mesin tik",
                                        color = colors.ink,
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.VerticalAlignCenter, null, tint = colors.muted)
                                },
                                onClick = {
                                    menu = false
                                    store.updateSettings { it.copy(typewriter = !it.typewriter) }
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (sprintRunning) "Hentikan sprint" else "Sprint 15 menit",
                                        color = colors.ink,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (sprintRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        null,
                                        tint = colors.muted,
                                    )
                                },
                                onClick = {
                                    menu = false
                                    if (sprintRunning) {
                                        sprintRunning = false
                                        sprintLeft = 0L
                                    } else {
                                        sprintLeft = 15L * 60L * 1000L
                                        sprintRunning = true
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Bagikan teks", color = colors.ink) },
                                leadingIcon = { Icon(Icons.Default.Share, null, tint = colors.muted) },
                                onClick = {
                                    menu = false
                                    shareText(context, title, value.text)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Ekspor Markdown", color = colors.ink) },
                                leadingIcon = { Icon(Icons.Default.Check, null, tint = colors.muted) },
                                onClick = {
                                    menu = false
                                    exportBody = buildString {
                                        append("# ").append(title.ifBlank { "Tanpa judul" }).append("\n\n")
                                        append(value.text)
                                    }
                                    exportLauncher.launch("${Text.slugify(title)}.md")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Pindahkan ke sampah", color = colors.danger) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = colors.danger) },
                                onClick = {
                                    menu = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!focusMode) {
                val siblings = data.chapters
                    .filter { it.bookId == chapter.bookId && it.deletedAt == null }
                    .sortedBy { it.order }
                val chapterIndex = siblings.indexOfFirst { it.id == chapter.id }
                val previousChapter = siblings.getOrNull(chapterIndex - 1)
                val followingChapter = siblings.getOrNull(chapterIndex + 1)
                EditorBottomBar(
                    markdown = chapter.markdown,
                    preview = preview,
                    onTogglePreview = { preview = !preview },
                    onPhrase = { showPhraseDialog = true },
                    words = words,
                    chars = value.text.length,
                    onPrev = previousChapter?.let { sibling ->
                        {
                            store.updateChapterBody(chapterId, value.text)
                            store.updateChapterTitle(chapterId, title)
                            onOpenChapter(sibling.id)
                        }
                    },
                    onNext = followingChapter?.let { sibling ->
                        {
                            store.updateChapterBody(chapterId, value.text)
                            store.updateChapterTitle(chapterId, title)
                            onOpenChapter(sibling.id)
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            if (!focusMode) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.replace("\n", "") },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 25.sp,
                        lineHeight = 32.sp,
                        color = colors.ink,
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box {
                            if (title.isBlank()) {
                                Text(
                                    "Tanpa judul",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colors.muted.copy(alpha = 0.7f),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 14.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (data.settings.chapterGoal > 0) {
                            "$words / ${data.settings.chapterGoal} kata"
                        } else {
                            "$words kata"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    if (sprintLeft > 0) {
                        Text("·", color = colors.border)
                        Text(
                            String.format(
                                Locale("id", "ID"),
                                "sprint %d:%02d",
                                sprintLeft / 60_000,
                                (sprintLeft / 1_000) % 60,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                        )
                    }
                    Text("·", color = colors.border)
                    Text(
                        "sekitar ${Text.readingMinutes(words)} menit baca",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        status,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (saved) colors.muted else colors.accent,
                    )
                }
                if (data.settings.chapterGoal > 0) {
                    val chapterProgress = (words.toFloat() / data.settings.chapterGoal).coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(colors.border.copy(alpha = 0.3f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(chapterProgress)
                                .height(2.dp)
                                .background(colors.accent),
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border.copy(alpha = 0.45f))
                )
            }

            if (preview && chapter.markdown) {
                MarkdownPreview(
                    body = value.text,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState),
                ) {
                BasicTextField(
                    value = value,
                    onValueChange = { next -> applyEdit(next) },
                    textStyle = TextStyle(
                        fontFamily = when (data.settings.font) {
                            "sans" -> FontFamily.SansSerif
                            "mono" -> FontFamily.Monospace
                            else -> FontFamily.Serif
                        },
                        fontSize = data.settings.fontSize.sp,
                        lineHeight = (data.settings.fontSize * data.settings.lineHeight).sp,
                        color = colors.ink,
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box {
                            if (value.text.isBlank()) {
                                Text(
                                    "Mulai menulis…",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = data.settings.fontSize.sp,
                                    ),
                                    color = colors.muted.copy(alpha = 0.55f),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                )
                }
            }

            if (!focusMode && !preview) {
                FormatBar(
                    markdown = chapter.markdown,
                    onWrap = { before, after ->
                        value = wrapSelection(value, before, after)
                    },
                    onInsert = { text ->
                        value = insertAtCursor(value, text)
                    },
                )
                QuickPhraseBar(
                    phrases = data.settings.phrases,
                    onInsert = { phrase -> value = insertAtCursor(value, phrase) },
                    onAdd = { showPhraseDialog = true },
                )
            }
        }
    }

    if (showToc) {
        val toc = data.chapters
            .filter { it.bookId == chapter.bookId && it.deletedAt == null }
            .sortedBy { it.order }
        AlertDialog(
            onDismissRequest = { showToc = false },
            containerColor = colors.surface,
            titleContentColor = colors.ink,
            title = { Text(book?.title ?: "Daftar isi", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    toc.forEach { sibling ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    store.updateChapterBody(chapterId, value.text)
                                    store.updateChapterTitle(chapterId, title)
                                    showToc = false
                                    if (sibling.id != chapter.id) onOpenChapter(sibling.id)
                                }
                                .padding(horizontal = 6.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                sibling.title.ifBlank { "Tanpa judul" },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (sibling.id == chapter.id) colors.accent else colors.ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${Text.wordCount(sibling.body)} kata",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showToc = false }) {
                    Text("Tutup", color = colors.muted)
                }
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Pindahkan ke sampah?",
            message = "Naskah “${chapter.title}” bisa dipulihkan dari Sampah di Setelan.",
            confirmLabel = "Pindahkan",
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                store.updateChapterBody(chapterId, value.text)
                store.deleteChapter(chapterId)
                confirmDelete = false
                onBack()
            },
        )
    }

    if (showPhraseDialog) {
        var phrase by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPhraseDialog = false },
            containerColor = colors.surface,
            titleContentColor = colors.ink,
            title = { Text("Frasa cepat", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "Ketuk frasa di bawah editor untuk menyisipkannya. Tambahkan frasa yang sering kamu pakai.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    TintaTextField(
                        value = phrase,
                        onValueChange = { phrase = it },
                        label = "Frasa baru",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.addPhrase(phrase)
                        phrase = ""
                        showPhraseDialog = false
                    },
                    enabled = phrase.isNotBlank(),
                ) {
                    Text("Tambah", color = if (phrase.isNotBlank()) colors.accent else colors.muted)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPhraseDialog = false }) {
                    Text("Tutup", color = colors.muted)
                }
            },
        )
    }
}

@Composable
private fun EditorBottomBar(
    markdown: Boolean,
    preview: Boolean,
    onTogglePreview: () -> Unit,
    onPhrase: () -> Unit,
    words: Int,
    chars: Int,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
) {
    val colors = LocalTintaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "$words kata · $chars karakter",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onPrev?.invoke() }, enabled = onPrev != null) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Bab sebelumnya",
                    tint = if (onPrev != null) colors.muted else colors.border,
                )
            }
            IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Bab berikutnya",
                    tint = if (onNext != null) colors.muted else colors.border,
                )
            }
            Text(
                "Frasa",
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPhrase)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
            if (markdown) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (preview) colors.accentSoft else colors.bg)
                        .clickable(onClick = onTogglePreview)
                        .padding(horizontal = 11.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (preview) "Tulis" else "Pratinjau",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (preview) colors.accent else colors.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatBar(
    markdown: Boolean,
    onWrap: (String, String) -> Unit,
    onInsert: (String) -> Unit,
) {
    val colors = LocalTintaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FormatChip("B", bold = true) { onWrap("**", "**") }
        FormatChip("I", italic = true) { onWrap("*", "*") }
        FormatChip("H2") { onInsert("\n## ") }
        FormatChip("•") { onInsert("\n- ") }
        FormatChip("“”") { onInsert("“”") }
        FormatChip("—") { onInsert("—") }
        FormatChip("…") { onInsert("…") }
        FormatChip("Tanggal") {
            onInsert(SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(Date()))
        }
        if (!markdown) {
            Text(
                "Markdown mati · format tetap tersimpan sebagai teks",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                modifier = Modifier.padding(start = 6.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun FormatChip(
    label: String,
    bold: Boolean = false,
    italic: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalTintaColors.current
    Text(
        label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            fontStyle = if (italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
            fontFamily = if (label == "B" || label == "I" || label == "H2") FontFamily.Serif else FontFamily.SansSerif,
        ),
        color = colors.ink,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun QuickPhraseBar(
    phrases: List<String>,
    onInsert: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalTintaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Sisipkan",
            style = MaterialTheme.typography.labelSmall,
            color = colors.muted,
        )
        phrases.forEach { phrase ->
            Text(
                phrase,
                style = MaterialTheme.typography.labelSmall,
                color = colors.ink,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.bg)
                    .clickable { onInsert(phrase) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Text(
            "+",
            style = MaterialTheme.typography.labelLarge,
            color = colors.accent,
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.accentSoft)
                .clickable(onClick = onAdd)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun wrapSelection(value: TextFieldValue, before: String, after: String): TextFieldValue {
    val start = min(value.selection.start, value.selection.end).coerceAtLeast(0)
    val end = max(value.selection.start, value.selection.end).coerceAtMost(value.text.length)
    val selected = value.text.substring(start, end)
    val next = value.text.replaceRange(start, end, before + selected + after)
    return value.copy(
        text = next,
        selection = TextRange(start + before.length, start + before.length + selected.length),
    )
}

private fun insertAtCursor(value: TextFieldValue, insert: String): TextFieldValue {
    val start = min(value.selection.start, value.selection.end).coerceAtLeast(0)
    val end = max(value.selection.start, value.selection.end).coerceAtMost(value.text.length)
    val next = value.text.replaceRange(start, end, insert)
    return value.copy(text = next, selection = TextRange(start + insert.length))
}

private fun shareText(context: android.content.Context, title: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Tanpa judul" })
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan naskah"))
}
