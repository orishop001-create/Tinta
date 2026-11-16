package id.tinta.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Text
import java.time.LocalDate

@Composable
fun StatsScreen(store: TintaStore, onBack: () -> Unit) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    val alive = data.chapters.filter { it.deletedAt == null }
    val totalWords = alive.sumOf { Text.wordCount(it.body) }
    val daily = data.daily.associate { it.date to it.words }
    val today = LocalDate.now()
    val todayWords = daily[today.toString()] ?: 0
    val goal = data.settings.dailyGoal.coerceAtLeast(1)
    val streakStart = if (todayWords > 0) today else today.minusDays(1)
    var streak = 0
    var cursor = streakStart
    while ((daily[cursor.toString()] ?: 0) > 0) {
        streak++
        cursor = cursor.minusDays(1)
    }
    val days = (83 downTo 0).map { today.minusDays(it.toLong()) }
    val recent = alive.sortedByDescending { it.updatedAt }.take(5)

    Scaffold(
        containerColor = colors.bg,
        topBar = { TintaTopBar("Statistik", subtitle = "Ritme menulismu", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$todayWords",
                            style = MaterialTheme.typography.headlineLarge,
                            color = colors.ink,
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "/ $goal kata hari ini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.muted,
                            modifier = Modifier.padding(bottom = 5.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (todayWords.toFloat() / goal).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = colors.accent,
                        trackColor = colors.bg,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (todayWords >= goal) "Target hari ini tercapai. Simpan energimu untuk besok."
                        else "${goal - todayWords} kata lagi menuju target. Tidak harus selesai hari ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Runtutan", "$streak hari", "hari berisi tulisan", Modifier.weight(1f))
                    StatCard("Total kata", "$totalWords", "di seluruh pustaka", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Naskah", "${alive.size}", "naskah aktif", Modifier.weight(1f))
                    StatCard("Buku", "${data.books.size}", "buku di rak", Modifier.weight(1f))
                }
            }

            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp)
                ) {
                    Text("12 minggu terakhir", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(3.dp))
                    Text("Setiap kotak satu hari. Makin pekat, makin banyak kata.", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        days.chunked(7).forEach { week ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                week.forEach { day ->
                                    val words = daily[day.toString()] ?: 0
                                    val ratio = (words.toFloat() / goal).coerceIn(0f, 1f)
                                    val color = when {
                                        words == 0 -> colors.border.copy(alpha = 0.30f)
                                        ratio < 0.25f -> colors.accent.copy(alpha = 0.28f)
                                        ratio < 0.55f -> colors.accent.copy(alpha = 0.50f)
                                        ratio < 0.9f -> colors.accent.copy(alpha = 0.74f)
                                        else -> colors.accent
                                    }
                                    Box(
                                        Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sedikit", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                        Spacer(Modifier.width(6.dp))
                        listOf(0.28f, 0.50f, 0.74f, 1f).forEach { alpha ->
                            Box(
                                Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.accent.copy(alpha = alpha))
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Banyak", style = MaterialTheme.typography.labelSmall, color = colors.muted)
                    }
                }
            }

            item {
                Text(
                    "Terakhir disentuh",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            items(recent, key = { it.id }) { chapter ->
                val book = data.books.firstOrNull { it.id == chapter.bookId }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${book?.title ?: "Tanpa buku"} · ${Text.wordCount(chapter.body)} kata",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                    }
                    Text(
                        Text.relative(chapter.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    val colors = LocalTintaColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(15.dp))
            .background(colors.surface)
            .padding(15.dp)
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.muted)
        Spacer(Modifier.height(5.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = colors.ink, fontWeight = FontWeight.SemiBold)
        Text(sub, style = MaterialTheme.typography.labelSmall, color = colors.muted)
    }
}
