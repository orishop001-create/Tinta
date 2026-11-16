package id.tinta.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import id.tinta.app.data.Chapter
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.ConfirmDialog
import id.tinta.app.ui.components.EmptyPanel
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Text

@Composable
fun TrashScreen(
    store: TintaStore,
    onBack: () -> Unit,
) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    val deleted = data.chapters
        .filter { it.deletedAt != null }
        .sortedByDescending { it.deletedAt }
    var purge by remember { mutableStateOf<Chapter?>(null) }

    Scaffold(
        containerColor = colors.bg,
        topBar = { TintaTopBar("Sampah", subtitle = "Naskah yang dipindahkan", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (deleted.isEmpty()) {
                item {
                    EmptyPanel(
                        title = "Sampah kosong",
                        body = "Naskah yang dipindahkan ke sampah akan muncul di sini dan bisa dipulihkan.",
                        actionLabel = "Kembali",
                        onAction = onBack,
                    )
                }
            } else {
                item {
                    Text(
                        "Naskah di sini tidak tampil di pustaka. Pemulihan mengembalikannya ke buku asal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                items(deleted, key = { it.id }) { chapter ->
                    val book = data.books.firstOrNull { it.id == chapter.bookId }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(colors.surface)
                            .padding(15.dp)
                    ) {
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${book?.title ?: "Buku terhapus"} · ${Text.relative(chapter.deletedAt ?: chapter.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            Text.snippet(chapter.body, 110),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { store.restoreChapter(chapter.id) }) {
                                Text("Pulihkan", color = colors.accent)
                            }
                            TextButton(onClick = { purge = chapter }) {
                                Text("Hapus permanen", color = colors.danger)
                            }
                        }
                    }
                }
            }
        }
    }

    purge?.let { chapter ->
        ConfirmDialog(
            title = "Hapus permanen?",
            message = "“${chapter.title}” dan riwayatnya akan hilang selamanya. Ini tidak bisa dibatalkan.",
            confirmLabel = "Hapus permanen",
            destructive = true,
            onDismiss = { purge = null },
            onConfirm = {
                store.purgeChapter(chapter.id)
                purge = null
            },
        )
    }
}
