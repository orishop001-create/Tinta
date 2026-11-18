package id.tinta.app.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.TintaStore
import id.tinta.app.util.AppLock
import id.tinta.app.util.Fonts
import id.tinta.app.ui.components.ConfirmDialog
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
    var encryptedText by remember { mutableStateOf<ByteArray?>(null) }
    var passwordFor by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<ByteArray?>(null) }
    val activity = context as? FragmentActivity
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

    val encryptedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val bytes = encryptedText
        encryptedText = null
        if (uri == null || bytes == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        }.onSuccess {
            store.markBackupDone()
            Toast.makeText(context, "Cadangan terenkripsi tersimpan.", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Gagal menyimpan berkas.", Toast.LENGTH_SHORT).show()
        }
    }

    val encryptedImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.onSuccess { bytes ->
            pendingImport = bytes
            password = ""
            passwordFor = "import"
        }.onFailure {
            Toast.makeText(context, "Gagal membaca berkas.", Toast.LENGTH_SHORT).show()
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        store.updateSettings { it.copy(backupFolder = uri.toString()) }
        Toast.makeText(context, "Folder cadangan disimpan.", Toast.LENGTH_SHORT).show()
    }

    val fontLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = Fonts.install(context, uri)
        if (name == null) {
            Toast.makeText(
                context,
                "Berkas font tidak dikenali. Pakai TTF atau OTF.",
                Toast.LENGTH_LONG,
            ).show()
        } else {
            store.setFontFile(name)
            Toast.makeText(context, "Font dipakai di editor.", Toast.LENGTH_SHORT).show()
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
                }
            }
            item {
                SettingCard {
                    Text("Font kustom", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (settings.fontFile == null) {
                            "Impor TTF/OTF milikmu sendiri untuk dipakai di editor naskah."
                        } else {
                            "Font milikmu aktif di editor. Bisa diganti atau dikembalikan ke bawaan."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            "Impor font",
                            onClick = {
                                fontLauncher.launch(
                                    arrayOf(
                                        "font/*",
                                        "application/x-font-ttf",
                                        "application/x-font-opentype",
                                        "application/octet-stream",
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (settings.fontFile != null) {
                            SecondaryButton(
                                "Pakai bawaan",
                                onClick = { store.setFontFile(null) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            item {
                SettingCard {
                    Text("Frasa cepat", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Geser urutan dengan ‹ ›, hapus dengan ×.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        settings.phrases.forEach { phrase ->
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.bg)
                                    .padding(start = 10.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    phrase,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.ink,
                                )
                                Text(
                                    "‹",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.muted,
                                    modifier = Modifier
                                        .clickable { store.movePhrase(phrase, -1) }
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                                Text(
                                    "›",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.muted,
                                    modifier = Modifier
                                        .clickable { store.movePhrase(phrase, 1) }
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                                Text(
                                    "×",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.danger,
                                    modifier = Modifier
                                        .clickable { store.removePhrase(phrase) }
                                        .padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }

            item { SectionLabel("Keamanan") }
            item {
                SettingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Kunci aplikasi", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                            Text(
                                "Sidik jari atau PIN perangkat diminta setiap Tinta dibuka.",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                            )
                        }
                        Switch(
                            checked = settings.lock,
                            onCheckedChange = { next ->
                                when {
                                    !next -> store.updateSettings { it.copy(lock = false) }
                                    !AppLock.isAvailable(context) -> Toast.makeText(
                                        context,
                                        "Perangkat ini belum punya sidik jari atau PIN.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    activity == null -> Toast.makeText(
                                        context,
                                        "Kunci belum bisa diaktifkan di layar ini.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    else -> AppLock.prompt(
                                        activity = activity,
                                        onSuccess = {
                                            store.updateSettings { it.copy(lock = true) }
                                            Toast.makeText(context, "Kunci aplikasi aktif.", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { reason ->
                                            Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()
                                        },
                                    )
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = colors.accent),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Naskah mentah tidak pernah dienkripsi otomatis supaya pencarian tetap instan; " +
                            "berkas cadangan yang kamu ekspor bisa dikunci dengan kata sandi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted,
                    )
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
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Cadangan otomatis juga ditulis ke folder aplikasi setiap aplikasi ditutup; tiga berkas terbaru disimpan.",
                        style = MaterialTheme.typography.labelSmall,
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryButton(
                            "Cadangan terenkripsi",
                            onClick = {
                                password = ""
                                passwordFor = "export"
                            },
                            modifier = Modifier.weight(1f),
                        )
                        SecondaryButton(
                            "Buka .tintaenc",
                            onClick = { encryptedImportLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                SettingCard {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { folderLauncher.launch(null) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Folder cadangan otomatis",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.ink,
                            )
                            Text(
                                if (settings.backupFolder == null) {
                                    "Pilih folder Drive, Syncthing, atau penyimpanan HP."
                                } else {
                                    "Setiap aplikasi ditutup, cadangan ditulis ke folder ini."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.muted,
                            )
                        }
                        Text(
                            if (settings.backupFolder == null) "Pilih" else "Ganti",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accent,
                        )
                    }
                    if (settings.backupFolder != null) {
                        Text(
                            "Cadangkan ke folder sekarang",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.accent,
                            modifier = Modifier
                                .clickable {
                                    val ok = store.backupToUserFolder()
                                    Toast.makeText(
                                        context,
                                        if (ok) "Cadangan ditulis ke folder pilihan." else "Folder tidak bisa ditulis.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
            item {
                SettingCard {
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
                    Text("Tinta 1.1.0", style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Editor menulis lokal-first. Dibangun ulang secara clean-room dengan Kotlin dan Jetpack Compose. Tidak ada WebView, akun, analitik, atau permintaan jaringan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Metadata disimpan sebagai JSON di penyimpanan privat aplikasi, sedangkan badan naskah " +
                            "disimpan per bab di SQLite agar naskah raksasa tetap ringan dibuka. Kunci aplikasi " +
                            "memakai sidik jari/PIN perangkat, dan cadangan bisa dienkripsi AES-GCM dengan kata " +
                            "sandi yang hanya kamu yang tahu.",
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

    passwordFor?.let { mode ->
        AlertDialog(
            onDismissRequest = {
                passwordFor = null
                pendingImport = null
                password = ""
            },
            containerColor = colors.surface,
            titleContentColor = colors.ink,
            title = {
                Text(
                    if (mode == "export") "Cadangan terenkripsi" else "Buka cadangan terenkripsi",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    Text(
                        "Buat kata sandi yang kuat. Tanpa kata sandi ini, berkas tidak bisa dibuka lagi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                    Spacer(Modifier.height(10.dp))
                    TintaTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Kata sandi",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pass = password
                        val pending = pendingImport
                        passwordFor = null
                        pendingImport = null
                        password = ""
                        if (mode == "export") {
                            encryptedText = store.exportEncrypted(pass)
                            encryptedLauncher.launch("tinta-cadangan.tintaenc")
                        } else if (pending != null) {
                            store.importEncrypted(pending, pass)
                                .onSuccess {
                                    store.markBackupDone()
                                    Toast.makeText(context, "Cadangan dipulihkan.", Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast.makeText(
                                        context,
                                        it.message ?: "Kata sandi salah atau berkas rusak.",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }
                        }
                    },
                    enabled = password.length >= 6,
                ) {
                    Text(
                        if (mode == "export") "Cadangkan" else "Pulihkan",
                        color = if (password.length >= 6) colors.accent else colors.muted,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    passwordFor = null
                    pendingImport = null
                    password = ""
                }) {
                    Text("Batal", color = colors.muted)
                }
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
    // Nilai sementara selama jari menggeser: state aplikasi baru ditulis sekali saat jari lepas,
    // supaya tidak ada JSON yang disusun ulang puluhan kali per detik.
    var draft by remember(valueFloat) { mutableStateOf(valueFloat) }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelSmall, color = colors.accent)
    }
    Slider(
        value = draft.coerceIn(range),
        onValueChange = { draft = it },
        onValueChangeFinished = {
            if (draft != valueFloat) onChange(draft)
        },
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
