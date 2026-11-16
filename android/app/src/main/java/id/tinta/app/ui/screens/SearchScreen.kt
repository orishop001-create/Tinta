package id.tinta.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.Chapter
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.ConfirmDialog
import id.tinta.app.ui.components.EmptyPanel
import id.tinta.app.ui.components.TintaTextField
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Text
import kotlin.math.max
import kotlin.math.min

private data class SearchHit(val chapter: Chapter, val count: Int, val snippet: String)

@Composable
fun SearchScreen(
    store: TintaStore,
    onBack: () -> Unit,
    onOpenChapter: (String) -> Unit,
) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var regexMode by remember { mutableStateOf(false) }
    var bookFilter by remember { mutableStateOf<String?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }

    val pattern = remember(query, regexMode, caseSensitive) {
        if (query.isBlank()) null
        else runCatching {
            Regex(
                if (regexMode) query else Regex.escape(query),
                if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE),
            )
        }.getOrNull()
    }
    val invalid = query.isNotBlank() && pattern == null

    val hits = remember(pattern, data.chapters, bookFilter) {
        if (pattern == null) emptyList()
        else data.chapters
            .filter { it.deletedAt == null && (bookFilter == null || it.bookId == bookFilter) }
            .mapNotNull { chapter ->
                val matches = pattern.findAll(chapter.body).toList()
                if (matches.isEmpty()) null
                else {
                    val first = matches.first().range.first
                    val start = max(0, first - 55)
                    val end = min(chapter.body.length, first + 130)
                    SearchHit(
                        chapter = chapter,
                        count = matches.size,
                        snippet = chapter.body.substring(start, end).replace(Regex("\\s+"), " ").trim(),
                    )
                }
            }
            .sortedByDescending { it.count }
    }
    val total = hits.sumOf { it.count }

    Scaffold(
        containerColor = colors.bg,
        topBar = { TintaTopBar("Cari & ganti", subtitle = "Lintas buku", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                TintaTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Kata atau pola",
                    placeholder = "Cari di semua naskah",
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = caseSensitive,
                        onClick = { caseSensitive = !caseSensitive },
                        label = { Text("Aa") },
                        colors = chipColors(),
                    )
                    FilterChip(
                        selected = regexMode,
                        onClick = { regexMode = !regexMode },
                        label = { Text(".* regex") },
                        colors = chipColors(),
                    )
                    FilterChip(
                        selected = bookFilter == null,
                        onClick = { bookFilter = null },
                        label = { Text("Semua buku") },
                        colors = chipColors(),
                    )
                    data.books.forEach { book ->
                        FilterChip(
                            selected = bookFilter == book.id,
                            onClick = { bookFilter = book.id },
                            label = { Text(book.title, maxLines = 1) },
                            colors = chipColors(),
                        )
                    }
                }
            }

            if (invalid) {
                item {
                    Text(
                        "Pola regex tidak valid. Periksa tanda kurung dan garis miring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.danger,
                    )
                }
            } else if (query.isNotBlank()) {
                item {
                    Text(
                        "$total kecocokan di ${hits.size} naskah",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
            } else {
                item {
                    EmptyPanel(
                        title = "Cari di seluruh pustaka",
                        body = "Ketik kata untuk menemukan naskah. Nyalakan regex atau bedakan huruf besar-kecil bila perlu.",
                    )
                }
            }

            if (query.isNotBlank() && !invalid) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.weight(1f)) {
                            TintaTextField(
                                value = replacement,
                                onValueChange = { replacement = it },
                                label = "Ganti dengan",
                                placeholder = "Boleh kosong",
                            )
                        }
                        TextButton(
                            onClick = { confirmReplace = true },
                            enabled = total > 0,
                        ) {
                            Text(
                                "Ganti semua",
                                color = if (total > 0) colors.accent else colors.muted,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            items(hits, key = { it.chapter.id }) { hit ->
                val book = data.books.firstOrNull { it.id == hit.chapter.bookId }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.surface)
                        .clickable { onOpenChapter(hit.chapter.id) }
                        .padding(15.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            hit.chapter.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${hit.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.accentSoft)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        "${book?.title ?: "Tanpa buku"} · ${Text.relative(hit.chapter.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "…${hit.snippet}…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (confirmReplace) {
        ConfirmDialog(
            title = "Ganti $total kecocokan?",
            message = "Perubahan berlaku di ${hits.size} naskah. Versi sebelum penggantian disimpan ke riwayat masing-masing naskah.",
            confirmLabel = "Ganti semua",
            destructive = true,
            onDismiss = { confirmReplace = false },
            onConfirm = {
                confirmReplace = false
                val changed = store.replaceAll(
                    query = query,
                    replacement = replacement,
                    regex = regexMode,
                    caseSensitive = caseSensitive,
                    bookId = bookFilter,
                )
                if (changed < 0) {
                    Toast.makeText(context, "Pola regex tidak valid.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "$changed kecocokan diganti.", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = LocalTintaColors.current.accentSoft,
    selectedLabelColor = LocalTintaColors.current.accent,
    labelColor = LocalTintaColors.current.muted,
)
