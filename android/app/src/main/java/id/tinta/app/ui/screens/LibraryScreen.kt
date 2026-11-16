package id.tinta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.Book
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.BookCover
import id.tinta.app.ui.components.ConfirmDialog
import id.tinta.app.ui.components.EmptyPanel
import id.tinta.app.ui.components.IconAction
import id.tinta.app.ui.components.TagPill
import id.tinta.app.ui.components.TintaTextField
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.CoverGradients
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Text

@Composable
fun LibraryScreen(
    store: TintaStore,
    onOpenBook: (String) -> Unit,
    onOpenChapter: (String) -> Unit,
    onSearch: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Book?>(null) }
    var deleting by remember { mutableStateOf<Book?>(null) }

    val lastChapter = data.settings.lastDocId?.let { id ->
        data.chapters.firstOrNull { it.id == id && it.deletedAt == null }
    }
    val lastBook = lastChapter?.let { chapter ->
        data.books.firstOrNull { it.id == chapter.bookId }
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TintaTopBar(
                title = "Tinta",
                subtitle = "Pustaka",
                actions = {
                    IconAction(Icons.Default.Search, "Cari", onClick = onSearch)
                    IconAction(Icons.Default.BarChart, "Statistik", onClick = onStats)
                    IconAction(Icons.Default.Settings, "Setelan", onClick = onSettings)
                    IconAction(Icons.Default.Add, "Buku baru") { showCreate = true }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (lastChapter != null && lastBook != null) {
                item {
                    ResumeCard(
                        book = lastBook,
                        chapterTitle = if (lastChapter.title.isBlank()) "Tanpa judul" else lastChapter.title,
                        updated = Text.relative(lastChapter.updatedAt),
                        onClick = { onOpenChapter(lastChapter.id) },
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Rak buku", style = MaterialTheme.typography.headlineSmall, color = colors.ink)
                    Text("${data.books.size} buku", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                }
            }

            if (data.books.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "Rakmu masih kosong",
                        body = "Mulai satu buku. Tidak perlu judul yang sempurna — judul bisa diubah kapan saja.",
                        actionLabel = "Buat buku pertama",
                        onAction = { showCreate = true },
                    )
                }
            } else {
                items(data.books, key = { it.id }) { book ->
                    val chapters = data.chapters.filter { it.bookId == book.id && it.deletedAt == null }
                    BookRow(
                        book = book,
                        chapterCount = chapters.size,
                        wordCount = chapters.sumOf { Text.wordCount(it.body) },
                        onClick = { onOpenBook(book.id) },
                        onEdit = { editing = book },
                        onDelete = { deleting = book },
                    )
                }
            }

            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.accentSoft)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            "Semua terbuka",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.accent,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tidak ada akun, iklan, atau versi Pro. Datamu tinggal di perangkat ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                }
            }
        }
    }

    if (showCreate || editing != null) {
        BookEditorDialog(
            initial = editing,
            onDismiss = {
                showCreate = false
                editing = null
            },
            onSubmit = { title, tags, cover ->
                if (editing == null) {
                    val id = store.createBook(title, tags, cover)
                    showCreate = false
                    onOpenBook(id)
                } else {
                    store.updateBook(editing!!.id, title, tags, cover)
                    editing = null
                }
            },
        )
    }

    deleting?.let { book ->
        ConfirmDialog(
            title = "Hapus “${book.title}”?",
            message = "Seluruh bab dan riwayat di dalam buku ini akan dihapus permanen dari perangkat. Tindakan ini tidak bisa dibatalkan.",
            confirmLabel = "Hapus buku",
            destructive = true,
            onDismiss = { deleting = null },
            onConfirm = {
                store.deleteBook(book.id)
                deleting = null
            },
        )
    }
}

@Composable
private fun ResumeCard(book: Book, chapterTitle: String, updated: String, onClick: () -> Unit) {
    val colors = LocalTintaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book, Modifier.width(45.dp).height(60.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "LANJUTKAN",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                chapterTitle,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${book.title} · $updated",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = colors.accent)
    }
}

@Composable
private fun BookRow(
    book: Book,
    chapterCount: Int,
    wordCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTintaColors.current
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BookCover(book, Modifier.width(46.dp).height(62.dp))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                book.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.tags.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    book.tags.take(3).forEach { TagPill(it) }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                "$chapterCount bab · $wordCount kata",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu buku", tint = colors.muted)
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                containerColor = colors.surface,
            ) {
                DropdownMenuItem(
                    text = { Text("Ubah buku", color = colors.ink) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = colors.muted) },
                    onClick = {
                        menu = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Hapus buku", color = colors.danger) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = colors.danger) },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun BookEditorDialog(
    initial: Book?,
    onDismiss: () -> Unit,
    onSubmit: (String, List<String>, Int) -> Unit,
) {
    val colors = LocalTintaColors.current
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var tags by remember(initial?.id) { mutableStateOf(initial?.tags?.joinToString(", ").orEmpty()) }
    var cover by remember(initial?.id) { mutableIntStateOf(initial?.cover ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        textContentColor = colors.muted,
        title = {
            Text(
                if (initial == null) "Buku baru" else "Ubah buku",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                TintaTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Judul buku",
                    placeholder = "Misalnya: Catatan perjalanan",
                )
                Spacer(Modifier.height(10.dp))
                TintaTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = "Tag (pisahkan dengan koma)",
                    placeholder = "jurnal, draf",
                )
                Spacer(Modifier.height(16.dp))
                Text("Sampul", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CoverGradients.forEachIndexed { index, gradient ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(gradient))
                                .then(
                                    if (cover == index) Modifier.border(2.dp, colors.accent, CircleShape)
                                    else Modifier
                                )
                                .clickable { cover = index },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Judul dan sampul hanya untukmu. Tidak ada data yang dikirim keluar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(title, tags.split(","), cover) },
                enabled = title.isNotBlank(),
            ) {
                Text("Simpan", color = if (title.isNotBlank()) colors.accent else colors.muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = colors.muted) }
        },
    )
}
