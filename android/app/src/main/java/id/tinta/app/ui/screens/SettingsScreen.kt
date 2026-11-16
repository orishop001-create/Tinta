package id.tinta.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.ConfirmDialog
import id.tinta.app.ui.components.PrimaryButton
import id.tinta.app.ui.components.SecondaryButton
import id.tinta.app.ui.components.TintaTextField
import id.tinta.app.ui.components.TintaTopBar
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.ui.theme.TintaPalettes
import java.util.Locale

@Composable
fun SettingsScreen(
    store: TintaStore,
    onBack: () -> Unit,
    onTrash: () -> Unit,
    onFeatures: () -> Unit,
) {
    val data by store.state.collectAsStateWithLifecycle()
    val colors = LocalTintaColors.current
    val context = LocalContext.current
    val settings = data.settings
    var confirmReset by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf<String?>(null) }
    var backupText by remember { mutableStateOf("") }
    var phraseDraft by remember { mutableStateOf("") }
    val trashCount = data.chapters.count { it.deletedAt != null }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(backupText.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                store.markBackupDone()
                Toast.makeText(context, "Cadangan JSON tersimpan.", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Gagal menyimpan cadangan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { importText = it }
                .onFailure {
                    Toast.makeText(context, "Gagal membaca berkas.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Scaffold(
        containerColor = colors.bg,
        topBar = { TintaTopBar("Setelan", subtitle = "Semua terbuka", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.accentSoft)
                        .padding(18.dp)
                ) {
                    Text(
                        "Gratis selamanya. Bukan uji coba.",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.accent,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Riwayat, pencarian, Markdown, ekspor, dan seluruh tema terbuka tanpa akun, iklan, atau kunci fitur.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onFeatures)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Lihat semua fitur Tinta",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accent,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            item { SectionLabel("Tampilan") }
            item {
                SettingCard {
                    Text("Tema", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TintaPalettes.forEach { palette ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(palette.bg)
                                    .border(
                                        width = if (settings.theme == palette.key) 2.dp else 1.dp,
                                        color = if (settings.theme == palette.key) palette.accent else palette.border,
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable {
                                        store.updateSettings { it.copy(theme = palette.key) }
                                    }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    palette.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.ink,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Jenis huruf editor",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("serif" to "Serif", "sans" to "Sans", "mono" to "Mono").forEach { (key, label) ->
                            FilterChip(
                                selected = settings.font == key,
                                onClick = { store.updateSettings { it.copy(font = key) } },
                                label = { Text(label) },
                                colors = filterChipColors(),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    SettingSlider(
                        label = "Ukuran huruf",
                        value = "${settings.fontSize.toInt()} sp",
                        valueFloat = settings.fontSize,
                        range = 14f..26f,
                        onChange = { value -> store.updateSettings { it.copy(fontSize = value) } },
                    )
                    SettingSlider(
                        label = "Jarak baris",
                        value = String.format(Locale("id", "ID"), "%.2f", settings.lineHeight),
                        valueFloat = settings.lineHeight,
                        range = 1.3f..2.4f,
                        onChange = { value -> store.updateSettings { it.copy(lineHeight = value) } },
                    )
                    SettingSlider(
                        label = "Target kata harian",
                        value = "${settings.dailyGoal} kata",
                        valueFloat = settings.dailyGoal.toFloat(),
                        range = 100f..2000f,
                        onChange = { value ->
                            store.updateSettings { it.copy(dailyGoal = (value / 50).toInt() * 50) }
                        },
                    )
                }
            }

            item { SectionLabel("Menulis") }
            item {
                SettingCard {
                    SettingSlider(
                        label = "Target kata per bab",
                        value = "${settings.chapterGoal} kata",
                        valueFloat = settings.chapterGoal.toFloat(),
                        range = 200f..5000f,
                        onChange = { value ->
                            store.updateSettings { it.copy(chapterGoal = (value / 100).toInt() * 100) }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { store.updateSettings { it.copy(typewriter = !it.typewriter) } }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Mode mesin tik",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.ink,
                            )
                            Text(
                                "Baris yang sedang ditulis tetap di tengah layar.",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                            )
                        }
                        Text(
                            if (settings.typewriter) "Aktif" else "Mati",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (settings.typewriter) colors.accent else colors.muted,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Frasa cepat", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ketuk frasa untuk menghapusnya dari bilah editor.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        settings.phrases.forEach { phrase ->
                            Box(
                                Modifier
                                    .clip(CircleShape)
                                    .background(colors.bg)
                                    .clickable { store.removePhrase(phrase) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Text(
                                    phrase,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.ink,
                                )
                            }
                        }
                    }
                }
            }

            item { SectionLabel("Data") }
            item {
                SettingCard {
                    Text("Cadangan lokal", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Data tinggal di perangkat ini. Buat cadangan JSON sebelum membersihkan data aplikasi atau berganti ponsel.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    val overdue = settings.lastBackupAt == 0L ||
                        System.currentTimeMillis() - settings.lastBackupAt > 7L * 24 * 60 * 60 * 1000
                    Text(
                        if (settings.lastBackupAt == 0L) {
                            "Belum pernah dicadangkan. Buat cadangan pertama sekarang."
                        } else {
                            "Terakhir dicadangkan ${id.tinta.app.util.Text.relative(settings.lastBackupAt)}" +
                                if (overdue) ". Sudah lewat 7 hari, waktunya mencadangkan lagi." else "."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) colors.danger else colors.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            "Cadangkan JSON",
                            onClick = {
                                backupText = store.exportJson()
                                backupLauncher.launch("tinta-cadangan.json")
                            },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            "Pulihkan",
                            onClick = { importLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onTrash)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Sampah",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "$trashCount naskah",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                        )
                    }
                }
            }

            item { SectionLabel("Tentang") }
            item {
                SettingCard {
                    Text("Tinta 1.0.0", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Editor menulis lokal-first. Dibangun ulang secara clean-room dengan Kotlin dan Jetpack Compose. Tidak ada WebView, akun, analitik, atau permintaan jaringan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Data disimpan sebagai JSON di penyimpanan privat aplikasi. Tidak ada enkripsi atau kunci aplikasi; itu tidak akan memberi keamanan nyata di perangkat yang tidak aman.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
            }

            item {
                SecondaryButton(
                    text = "Hapus semua data",
                    onClick = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Hapus semua data?",
            message = "Seluruh buku, naskah, riwayat, dan statistik akan dihapus dari perangkat. Buat cadangan JSON terlebih dahulu jika masih diperlukan.",
            confirmLabel = "Hapus semua",
            destructive = true,
            onDismiss = { confirmReset = false },
            onConfirm = {
                store.resetAll()
                confirmReset = false
                Toast.makeText(context, "Semua data dihapus.", Toast.LENGTH_SHORT).show()
            },
        )
    }

    importText?.let { text ->
        ConfirmDialog(
            title = "Pulihkan cadangan?",
            message = "Data saat ini akan diganti dengan isi berkas cadangan. Tindakan ini tidak bisa dibatalkan.",
            confirmLabel = "Pulihkan",
            destructive = true,
            onDismiss = { importText = null },
            onConfirm = {
                store.importJson(text)
                    .onSuccess {
                        store.markBackupDone()
                        Toast.makeText(context, "Cadangan dipulihkan.", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(context, it.message ?: "Berkas cadangan tidak valid.", Toast.LENGTH_LONG).show()
                    }
                importText = null
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalTintaColors.current.muted,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, start = 2.dp),
    )
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTintaColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun SettingSlider(
    label: String,
    value: String,
    valueFloat: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    val colors = LocalTintaColors.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelSmall, color = colors.accent)
    }
    Slider(
        value = valueFloat.coerceIn(range),
        onValueChange = onChange,
        valueRange = range,
        colors = SliderDefaults.colors(
            thumbColor = colors.accent,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.border.copy(alpha = 0.45f),
        ),
    )
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = LocalTintaColors.current.accentSoft,
    selectedLabelColor = LocalTintaColors.current.accent,
    labelColor = LocalTintaColors.current.muted,
)
