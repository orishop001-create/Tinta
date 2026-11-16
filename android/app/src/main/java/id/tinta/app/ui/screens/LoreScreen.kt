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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import id.tinta.app.data.LoreEntry
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.ConfirmDialog
import id.tinta.app.ui.components.EmptyPanel
import id.tinta.app.ui.components.IconAction
import id.tinta.app.ui.components.TagPill
import id.tinta.app.ui.components.TintaTextField
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.LocalTintaColors

/** Story Bible: kartu karakter, lokasi, dan istilah yang menempel pada satu buku. */
@Composable
fun LoreScreen(
    store: TintaStore,
    bookId: String,
    onBack: () -> Unit,
) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    val book = data.books.firstOrNull { it.id == bookId }
    var filter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<LoreEntry?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<LoreEntry?>(null) }

    val entries = data.lore
        .filter { it.bookId == bookId && (filter == null || it.kind == filter) }
        .sortedWith(compareBy({ it.kind }, { it.name.lowercase() }))

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TintaTopBar(
                title = "Story Bible",
                subtitle = book?.title ?: "Catatan dunia cerita",
                onBack = onBack,
                actions = { IconAction(Icons.Default.Add, "Kartu baru") { creating = true } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accentSoft)
                        .padding(16.dp),
                ) {
                    Text(
                        "Dunia cerita di satu tempat",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Simpan warna mata tokoh sampingan, nama kota fiksi, atau istilah yang dipakai " +
                            "berulang. Semua kartu tinggal di perangkat ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    LoreChip("Semua", filter == null) { filter = null }
                    LoreEntry.kinds.forEach { kind ->
                        LoreChip(LoreEntry.kindLabel(kind), filter == kind) { filter = kind }
                    }
                }
            }

            if (entries.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "Belum ada kartu",
                        body = "Mulai dari tokoh utama. Tulis ringkasan singkat yang bisa kamu buka " +
                            "tanpa keluar dari naskah.",
                        actionLabel = "Buat kartu pertama",
                        onAction = { creating = true },
                    )
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .clickable { editing = entry }
                            .padding(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                entry.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            TagPill(LoreEntry.kindLabel(entry.kind))
                            Spacer(Modifier.width(2.dp))
                            IconAction(Icons.Default.Delete, "Hapus kartu", tint = colors.muted) {
                                deleting = entry
                            }
                        }
                        if (entry.summary.isNotBlank()) {
                            Spacer(Modifier.height(5.dp))
                            Text(
                                entry.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        LoreEditorDialog(
            initial = editing,
            onDismiss = {
                creating = false
                editing = null
            },
            onSubmit = { kind, name, summary ->
                val current = editing
                if (current == null) {
                    store.addLore(bookId, kind, name, summary)
                } else {
                    store.updateLore(current.id, kind, name, summary)
                }
                creating = false
                editing = null
            },
        )
    }

    deleting?.let { entry ->
        ConfirmDialog(
            title = "Hapus “${entry.name}”?",
            message = "Kartu ini akan hilang dari Story Bible. Naskahmu tidak ikut berubah.",
            confirmLabel = "Hapus",
            destructive = true,
            onDismiss = { deleting = null },
            onConfirm = {
                store.deleteLore(entry.id)
                deleting = null
            },
        )
    }
}

@Composable
private fun LoreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTintaColors.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = colors.accentSoft,
            selectedLabelColor = colors.accent,
            labelColor = colors.muted,
        ),
    )
}

@Composable
private fun LoreEditorDialog(
    initial: LoreEntry?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
) {
    val colors = LocalTintaColors.current
    var kind by remember(initial?.id) { mutableStateOf(initial?.kind ?: LoreEntry.KIND_CHARACTER) }
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var summary by remember(initial?.id) { mutableStateOf(initial?.summary.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = {
            Text(
                if (initial == null) "Kartu baru" else "Ubah kartu",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    LoreEntry.kinds.forEach { option ->
                        LoreChip(LoreEntry.kindLabel(option), kind == option) { kind = option }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TintaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nama",
                    placeholder = "Misalnya: Anjani",
                )
                Spacer(Modifier.height(10.dp))
                TintaTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = "Ringkasan",
                    singleLine = false,
                    minLines = 4,
                    placeholder = "Ciri fisik, sifat, atau peran di cerita",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(kind, name.trim(), summary.trim()) },
                enabled = name.isNotBlank(),
            ) {
                Text("Simpan", color = if (name.isNotBlank()) colors.accent else colors.muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = colors.muted) }
        },
    )
}
