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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.Snapshot
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.ConfirmDialog
import id.tinta.app.ui.components.EmptyPanel
import id.tinta.app.ui.components.IconAction
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Diff
import id.tinta.app.util.DiffKind
import id.tinta.app.util.Text

@Composable
fun HistoryScreen(
    store: TintaStore,
    chapterId: String,
    onBack: () -> Unit,
) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    val chapter = data.chapters.firstOrNull { it.id == chapterId && it.deletedAt == null }
    var selected by remember { mutableStateOf<Snapshot?>(null) }
    var confirmRestore by remember { mutableStateOf<Snapshot?>(null) }

    if (chapter == null) {
        Scaffold(
            containerColor = colors.bg,
            topBar = { TintaTopBar("Riwayat", onBack = onBack) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                EmptyPanel(
                    title = "Riwayat tidak tersedia",
                    body = "Naskah ini sudah dipindahkan ke sampah atau dihapus.",
                    actionLabel = "Kembali",
                    onAction = onBack,
                )
            }
        }
        return
    }

    val snapshots = data.snapshots
        .filter { it.chapterId == chapterId }
        .sortedByDescending { it.createdAt }

    Scaffold(
        containerColor = colors.bg,
        topBar = {
            TintaTopBar(
                title = "Riwayat",
                subtitle = chapter.title,
                onBack = onBack,
                actions = {
                    IconAction(Icons.Default.Add, "Simpan versi") {
                        store.manualSnapshot(chapterId)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                VersionCard(
                    label = "VERSI SEKARANG",
                    time = Text.relative(chapter.updatedAt),
                    words = Text.wordCount(chapter.body),
                    body = chapter.body,
                    accent = true,
                    onClick = null,
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Versi tersimpan",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                    )
                    Text(
                        "${snapshots.size} dari 40",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
            }

            if (snapshots.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "Belum ada versi lain",
                        body = "Versi dibuat saat naskah dibuka, setiap dua menit saat menulis, dan saat kamu menekan +.",
                        actionLabel = "Simpan versi sekarang",
                        onAction = { store.manualSnapshot(chapterId) },
                    )
                }
            } else {
                items(snapshots, key = { it.id }) { snapshot ->
                    VersionCard(
                        label = when (snapshot.kind) {
                            "manual" -> "MANUAL"
                            "sebelum-pulih" -> "SEBELUM PULIH"
                            "sebelum-ganti" -> "SEBELUM GANTI"
                            else -> "OTOMATIS"
                        },
                        time = "${Text.shortDate(snapshot.createdAt)} · ${Text.timeOnly(snapshot.createdAt)}",
                        words = Text.wordCount(snapshot.body),
                        body = snapshot.body,
                        accent = false,
                        onClick = { selected = snapshot },
                    )
                }
            }
        }
    }

    selected?.let { snapshot ->
        SnapshotDialog(
            snapshot = snapshot,
            current = chapter.body,
            onDismiss = { selected = null },
            onRestore = {
                selected = null
                confirmRestore = snapshot
            },
        )
    }

    confirmRestore?.let { snapshot ->
        ConfirmDialog(
            title = "Pulihkan versi ini?",
            message = "Isi naskah sekarang akan diganti. Versi saat ini tetap disimpan sebagai “sebelum pulih”.",
            confirmLabel = "Pulihkan",
            onDismiss = { confirmRestore = null },
            onConfirm = {
                store.restoreSnapshot(snapshot.id)
                confirmRestore = null
                onBack()
            },
        )
    }
}

@Composable
private fun VersionCard(
    label: String,
    time: String,
    words: Int,
    body: String,
    accent: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = LocalTintaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (accent) colors.accentSoft else colors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (accent) colors.accent else colors.muted,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text("$words kata", style = MaterialTheme.typography.labelSmall, color = colors.muted)
        }
        Spacer(Modifier.height(5.dp))
        Text(time, style = MaterialTheme.typography.labelSmall, color = colors.muted)
        Spacer(Modifier.height(8.dp))
        Text(
            Text.snippet(body, 190).ifBlank { "Naskah kosong" },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SnapshotDialog(
    snapshot: Snapshot,
    current: String,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
) {
    val colors = LocalTintaColors.current
    val diff = remember(snapshot.id, current) { Diff.lines(snapshot.body, current) }
    val added = diff.count { it.kind == DiffKind.ADD }
    val removed = diff.count { it.kind == DiffKind.REMOVE }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Bandingkan versi", style = MaterialTheme.typography.titleLarge, color = colors.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Text.shortDate(snapshot.createdAt)} · +$added / −$removed baris terhadap versi sekarang",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    diff.take(350).forEach { line ->
                        val bg = when (line.kind) {
                            DiffKind.ADD -> colors.accent.copy(alpha = 0.13f)
                            DiffKind.REMOVE -> colors.danger.copy(alpha = 0.12f)
                            DiffKind.SAME -> Color.Transparent
                        }
                        val prefix = when (line.kind) {
                            DiffKind.ADD -> "+ "
                            DiffKind.REMOVE -> "− "
                            DiffKind.SAME -> "  "
                        }
                        Text(
                            prefix + line.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = when (line.kind) {
                                DiffKind.ADD -> colors.accent
                                DiffKind.REMOVE -> colors.danger
                                DiffKind.SAME -> colors.muted
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Tutup", color = colors.muted) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onRestore) {
                        Text("Pulihkan", color = colors.accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
