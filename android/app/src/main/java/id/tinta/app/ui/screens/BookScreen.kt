package id.tinta.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.Book
import id.tinta.app.data.Chapter
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.BookCover
import id.tinta.app.ui.components.ConfirmDialog
import id.tinta.app.ui.components.EmptyPanel
import id.tinta.app.ui.components.IconAction
import id.tinta.app.ui.components.PrimaryButton
import id.tinta.app.ui.components.TagPill
import id.tinta.app.ui.components.TintaTextField
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Text

@Composable
fun BookScreen(
    store: TintaStore,
    bookId: String,
    onBack: () -> Unit,
    onOpenChapter: (String) -> Unit,
) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    val book = data.books.firstOrNull { it.id == bookId }
    var showAddChapter by remember { mutableStateOf(false) }
    var showAddVolume by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var deleteBook by remember { mutableStateOf(false) }
    var deleteVolume by remember { mutableStateOf<String?>(null) }
    var deleteChapter by remember { mutableStateOf<Chapter?>(null) }
    var moveChapter by remember { mutableStateOf<Chapter?>(null) }

    if (book == null) {
        Scaffold(
            containerColor = colors.bg,
            topBar = { TintaTopBar("Buku", onBack = onBack) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                EmptyPanel(
                    title = "Buku tidak ditemukan",
                    body = "Buku ini mungkin sudah dihapus dari perangkat.",
                    actionLabel = "Kembali ke pustaka",
                    onAction = onBack,
                )
            }
        }
        return
    }

    val chapters = data.chapters
        .filter { it.bookId == book.id && it.deletedAt == null }
        .sortedBy { it.order }
    val groups = book.volumes.map { volume ->
        volume to chapters.filter { it.volumeId == volume.id }
    } + (null to chapters.filter { it.volumeId == null || book.volumes.none { volume -> volume.id == it.volumeId } })

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TintaTopBar(
                title = book.title,
                subtitle = "${chapters.size} bab · ${chapters.sumOf { Text.wordCount(it.body) }} kata",
                onBack = onBack,
                actions = {
                    IconAction(Icons.Default.Add, "Bab baru") { showAddChapter = true }
                    Box {
                        IconAction(Icons.Default.MoreVert, "Menu buku") { showMenu = true }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = colors.surface,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tambah bagian", color = colors.ink) },
                                onClick = {
                                    showMenu = false
                                    showAddVolume = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus buku", color = colors.danger) },
                                onClick = {
                                    showMenu = false
                                    deleteBook = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BookCover(book, Modifier.width(52.dp).height(70.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        if (book.tags.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                book.tags.forEach { TagPill(it) }
                            }
                            Spacer(Modifier.height(7.dp))
                        }
                        Text(
                            "Dibuat ${Text.shortDate(book.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Tersunting ${Text.relative(book.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                    }
                }
            }

            if (chapters.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "Belum ada bab",
                        body = "Tambahkan bab pertama. Kamu bisa mulai menulis tanpa judul yang rapi.",
                        actionLabel = "Tulis bab pertama",
                        onAction = { showAddChapter = true },
                    )
                }
            } else {
                groups.forEach { (volume, docs) ->
                    if (docs.isNotEmpty() || volume != null) {
                        item(key = "header-${volume?.id ?: "none"}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 9.dp, bottom = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    volume?.title ?: "Tanpa bagian",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.ink,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${docs.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted,
                                )
                                if (volume != null) {
                                    IconButton(onClick = { deleteVolume = volume.id }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus bagian",
                                            tint = colors.muted,
                                        )
                                    }
                                }
                            }
                        }
                        items(docs, key = { it.id }) { chapter ->
                            ChapterRow(
                                chapter = chapter,
                                onClick = { onOpenChapter(chapter.id) },
                                onMove = { moveChapter = chapter },
                                onDuplicate = {
                                    store.duplicateChapter(chapter.id)?.let(onOpenChapter)
                                },
                                onDelete = { deleteChapter = chapter },
                            )
                        }
                    }
                }
            }

            item {
                SecondaryAddButton(
                    text = "Tambah bab",
                    onClick = { showAddChapter = true },
                )
            }
        }
    }

    if (showAddChapter) {
        AddChapterDialog(
            volumes = book.volumes,
            onDismiss = { showAddChapter = false },
            onSubmit = { title, volumeId ->
                val id = store.createChapter(book.id, volumeId, title)
                showAddChapter = false
                onOpenChapter(id)
            },
        )
    }

    if (showAddVolume) {
        AddVolumeDialog(
            onDismiss = { showAddVolume = false },
            onSubmit = { title ->
                store.addVolume(book.id, title)
                showAddVolume = false
            },
        )
    }

    if (deleteBook) {
        ConfirmDialog(
            title = "Hapus “${book.title}”?",
            message = "Seluruh bab dan riwayat di buku ini akan hilang permanen dari perangkat.",
            confirmLabel = "Hapus buku",
            destructive = true,
            onDismiss = { deleteBook = false },
            onConfirm = {
                store.deleteBook(book.id)
                deleteBook = false
                onBack()
            },
        )
    }

    deleteVolume?.let { volumeId ->
        val volume = book.volumes.firstOrNull { it.id == volumeId }
        ConfirmDialog(
            title = "Hapus bagian?",
            message = "Bab di dalam “${volume?.title ?: "bagian ini"}” tidak dihapus; mereka dipindahkan ke Tanpa bagian.",
            confirmLabel = "Hapus bagian",
            destructive = true,
            onDismiss = { deleteVolume = null },
            onConfirm = {
                store.deleteVolume(book.id, volumeId)
                deleteVolume = null
            },
        )
    }

    deleteChapter?.let { chapter ->
        ConfirmDialog(
            title = "Pindahkan ke sampah?",
            message = "“${chapter.title}” bisa dipulihkan dari Sampah di Setelan.",
            confirmLabel = "Pindahkan",
            destructive = true,
            onDismiss = { deleteChapter = null },
            onConfirm = {
                store.deleteChapter(chapter.id)
                deleteChapter = null
            },
        )
    }

    moveChapter?.let { chapter ->
        MoveChapterDialog(
            volumes = book.volumes,
            currentVolumeId = chapter.volumeId,
            onDismiss = { moveChapter = null },
            onSubmit = { volumeId ->
                store.moveChapter(chapter.id, volumeId)
                moveChapter = null
            },
        )
    }
}

@Composable
private fun SecondaryAddButton(text: String, onClick: () -> Unit) {
    val colors = LocalTintaColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = colors.accent)
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    onClick: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTintaColors.current
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, top = 13.dp, bottom = 13.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                chapter.title.ifBlank { "Tanpa judul" },
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${Text.wordCount(chapter.body)} kata · ${Text.relative(chapter.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = colors.border)
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu bab", tint = colors.muted)
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                containerColor = colors.surface,
            ) {
                DropdownMenuItem(
                    text = { Text("Pindah bagian", color = colors.ink) },
                    leadingIcon = { Icon(Icons.Default.DriveFileMove, null, tint = colors.muted) },
                    onClick = {
                        menu = false
                        onMove()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Duplikat", color = colors.ink) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = colors.muted) },
                    onClick = {
                        menu = false
                        onDuplicate()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Pindahkan ke sampah", color = colors.danger) },
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
private fun AddChapterDialog(
    volumes: List<id.tinta.app.data.Volume>,
    onDismiss: () -> Unit,
    onSubmit: (String, String?) -> Unit,
) {
    val colors = LocalTintaColors.current
    var title by remember { mutableStateOf("") }
    var volumeId by remember { mutableStateOf<String?>(volumes.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        textContentColor = colors.muted,
        title = { Text("Bab baru", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                TintaTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Judul bab",
                    placeholder = "Bisa diubah nanti",
                )
                if (volumes.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Masukkan ke", style = MaterialTheme.typography.labelLarge, color = colors.ink)
                    Spacer(Modifier.height(6.dp))
                    Column {
                        val options = listOf<Pair<String?, String>>(null to "Tanpa bagian") +
                            volumes.map { it.id to it.title }
                        options.forEach { (id, label) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { volumeId = id }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = volumeId == id,
                                    onClick = { volumeId = id },
                                    colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                                )
                                Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(title, volumeId) },
                enabled = title.isNotBlank(),
            ) {
                Text("Tulis", color = if (title.isNotBlank()) colors.accent else colors.muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = colors.muted) }
        },
    )
}

@Composable
private fun AddVolumeDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    val colors = LocalTintaColors.current
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = { Text("Bagian baru", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                TintaTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nama bagian",
                    placeholder = "Bagian I",
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Bagian membantu mengelompokkan bab. Kamu bisa memindahkan bab kapan saja.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(title) },
                enabled = title.isNotBlank(),
            ) {
                Text("Tambah", color = if (title.isNotBlank()) colors.accent else colors.muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = colors.muted) }
        },
    )
}

@Composable
private fun MoveChapterDialog(
    volumes: List<id.tinta.app.data.Volume>,
    currentVolumeId: String?,
    onDismiss: () -> Unit,
    onSubmit: (String?) -> Unit,
) {
    val colors = LocalTintaColors.current
    var selected by remember { mutableStateOf(currentVolumeId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = { Text("Pindah bagian", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                val options = (listOf<Pair<String?, String>>(null to "Tanpa bagian") +
                    volumes.map { it.id to it.title })
                options.forEach { (id, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = id }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == id,
                            onClick = { selected = id },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accent),
                        )
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.ink)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selected) }) {
                Text("Pindahkan", color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = colors.muted) }
        },
    )
}
