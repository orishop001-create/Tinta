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
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import id.tinta.app.data.Chapter
import id.tinta.app.data.LoreEntry
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.ReadAloudDialog
import id.tinta.app.ui.theme.TintaPalettes
import id.tinta.app.util.Tesaurus
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    store: TintaStore,
    chapterId: String,
    onBack: () -> Unit,
    onHistory: () -> Unit,
    onOpenChapter: (String) -> Unit,
    initialPanel: String? = null,
    onLore: (String) -> Unit = {},
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
    var showType by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showRead by remember { mutableStateOf(false) }
    var noteQuote by remember { mutableStateOf<String?>(null) }
    var noteDraft by remember { mutableStateOf("") }
    var loreCard by remember { mutableStateOf<LoreEntry?>(null) }
    var typing by remember { mutableStateOf(false) }
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

    LaunchedEffect(initialPanel) {
        when (initialPanel) {
            "typography" -> showType = true
            "notes" -> showNotes = true
            "read" -> showRead = true
        }
    }

    LaunchedEffect(typing) {
        if (typing) {
            delay(2500)
            typing = false
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
    val selStart = min(value.selection.start, value.selection.end).coerceIn(0, value.text.length)
    val selEnd = max(value.selection.start, value.selection.end).coerceIn(0, value.text.length)
    val selectedText = if (selEnd > selStart) value.text.substring(selStart, selEnd) else ""
    val synonyms = if (selectedText.isNotBlank() && selectedText.length <= 40) {
        Tesaurus.synonyms(context, selectedText)
    } else {
        emptyList()
    }
    val matchingLore = if (selectedText.isNotBlank()) {
        data.lore.firstOrNull {
            it.bookId == chapter.bookId && it.name.equals(selectedText.trim(), ignoreCase = true)
        }
    } else {
        null
    }
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
                    IconAction(Icons.Default.FormatSize, "Cepat tata letak") { showType = true }
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
                                text = {
                                    Text(
                                        "Catatan pinggir (${data.notes.count { it.chapterId == chapterId && !it.resolved }})",
                                        color = colors.ink,
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.StickyNote2, null, tint = colors.muted) },
                                onClick = {
                                    menu = false
                                    showNotes = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Baca nyaring", color = colors.ink) },
                                leadingIcon = { Icon(Icons.Default.VolumeUp, null, tint = colors.muted) },
                                onClick = {
                                    menu = false
                                    showRead = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Story Bible", color = colors.ink) },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = colors.muted) },
                                onClick = {
                                    menu = false
                                    onLore(chapter.bookId)
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
                    onValueChange = { next ->
                        typing = true
                        applyEdit(next)
                    },
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

            if (selectedText.isNotBlank() && !preview && !focusMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.accentSoft)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "“${selectedText.trim().take(22)}”",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    synonyms.take(3).forEach { word ->
                        Text(
                            word,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.bg)
                                .clickable {
                                    value = value.copy(
                                        text = value.text.replaceRange(selStart, selEnd, word),
                                        selection = TextRange(selStart + word.length),
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    if (matchingLore != null) {
                        Text(
                            "Kartu ${matchingLore.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.bg)
                                .clickable { loreCard = matchingLore }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    Text(
                        "Catatan",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.ink,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.bg)
                            .clickable {
                                noteQuote = selectedText.trim()
                                noteDraft = ""
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            if (!focusMode && !preview && !typing) {
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

    if (showType) {
        ModalBottomSheet(
            onDismissRequest = { showType = false },
            containerColor = colors.surface,
        ) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 28.dp)) {
                Text("Tata letak", style = MaterialTheme.typography.titleLarge, color = colors.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Perubahan langsung terlihat pada naskah di belakang panel ini.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Ukuran huruf · ${data.settings.fontSize.toInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.ink,
                )
                Slider(
                    value = data.settings.fontSize,
                    onValueChange = { size -> store.updateSettings { it.copy(fontSize = size) } },
                    valueRange = 14f..30f,
                    colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
                )
                Text(
                    "Jarak baris · ${String.format(Locale("id", "ID"), "%.2f", data.settings.lineHeight)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.ink,
                )
                Slider(
                    value = data.settings.lineHeight,
                    onValueChange = { height -> store.updateSettings { it.copy(lineHeight = height) } },
                    valueRange = 1.2f..2.4f,
                    colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
                )
                Spacer(Modifier.height(8.dp))
                Text("Jenis huruf", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("serif" to "Serif", "sans" to "Sans", "mono" to "Mono").forEach { (key, label) ->
                        StyleChip(
                            label = label,
                            selected = data.settings.font == key,
                            onClick = { store.updateSettings { it.copy(font = key) } },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Tema", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TintaPalettes.forEach { palette ->
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.bg)
                                .clickable { store.updateSettings { it.copy(theme = palette.key) } }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                palette.key.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.ink,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Contoh paragraf. Ia melangkah pelan ke arah jendela, menghitung setiap " +
                        "ketukan hujan yang jatuh di kusen.",
                    style = TextStyle(
                        fontFamily = when (data.settings.font) {
                            "sans" -> FontFamily.SansSerif
                            "mono" -> FontFamily.Monospace
                            else -> FontFamily.Serif
                        },
                        fontSize = data.settings.fontSize.sp,
                        lineHeight = (data.settings.fontSize * data.settings.lineHeight).sp,
                    ),
                    color = colors.ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bg)
                        .padding(14.dp),
                )
            }
        }
    }

    if (showNotes) {
        val notes = data.notes.filter { it.chapterId == chapterId }.sortedByDescending { it.createdAt }
        AlertDialog(
            onDismissRequest = { showNotes = false },
            containerColor = colors.surface,
            titleContentColor = colors.ink,
            title = { Text("Catatan pinggir", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (notes.isEmpty()) {
                        Text(
                            "Blok satu kalimat di naskah, lalu ketuk Catatan pada bilah kuning untuk " +
                                "menempelkan catatan revisi tanpa mengubah teks utama.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                    notes.forEach { note ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.bg)
                                .padding(10.dp),
                        ) {
                            if (note.quote.isNotBlank()) {
                                Text(
                                    "“${note.quote.take(80)}”",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted,
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(
                                note.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (note.resolved) colors.muted else colors.ink,
                            )
                            Row {
                                Text(
                                    if (note.resolved) "Buka lagi" else "Tandai selesai",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    modifier = Modifier
                                        .clickable { store.toggleNote(note.id) }
                                        .padding(end = 16.dp, top = 6.dp),
                                )
                                Text(
                                    "Hapus",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.danger,
                                    modifier = Modifier
                                        .clickable { store.deleteNote(note.id) }
                                        .padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotes = false }) { Text("Tutup", color = colors.muted) }
            },
        )
    }

    noteQuote?.let { quote ->
        AlertDialog(
            onDismissRequest = { noteQuote = null },
            containerColor = colors.surface,
            titleContentColor = colors.ink,
            title = { Text("Catatan pinggir", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        "“${quote.take(90)}”",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    TintaTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        label = "Catatan revisi",
                        placeholder = "Cek lagi logika adegan ini",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.addNote(chapterId, quote, noteDraft)
                        noteDraft = ""
                        noteQuote = null
                    },
                    enabled = noteDraft.isNotBlank(),
                ) {
                    Text("Simpan", color = if (noteDraft.isNotBlank()) colors.accent else colors.muted)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteQuote = null }) { Text("Batal", color = colors.muted) }
            },
        )
    }

    loreCard?.let { entry ->
        AlertDialog(
            onDismissRequest = { loreCard = null },
            containerColor = colors.surface,
            titleContentColor = colors.ink,
            title = { Text(entry.name, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(
                        LoreEntry.kindLabel(entry.kind),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        entry.summary.ifBlank { "Belum ada ringkasan untuk kartu ini." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { loreCard = null }) { Text("Tutup", color = colors.muted) }
            },
        )
    }

    if (showRead) {
        ReadAloudDialog(text = value.text, onDismiss = { showRead = false })
    }
}

@Composable
private fun StyleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTintaColors.current
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.accent else colors.muted,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.accentSoft else colors.bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
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
