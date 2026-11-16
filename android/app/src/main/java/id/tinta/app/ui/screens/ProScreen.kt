package id.tinta.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.tinta.app.ui.theme.LocalTintaColors

private data class ProTile(val icon: ImageVector, val label: String)

private data class ProHighlight(val icon: ImageVector, val title: String, val body: String)

private val proTiles = listOf(
    ProTile(Icons.Filled.Palette, "Empat tema kertas"),
    ProTile(Icons.Filled.Code, "Pratinjau Markdown"),
    ProTile(Icons.Filled.Download, "Ekspor Markdown"),
    ProTile(Icons.Filled.Share, "Bagikan naskah"),
    ProTile(Icons.Filled.FindReplace, "Cari & ganti"),
    ProTile(Icons.AutoMirrored.Filled.MenuBook, "Buku, bagian, bab"),
    ProTile(Icons.Filled.History, "Riwayat versi"),
    ProTile(Icons.Filled.TextFields, "Tiga jenis huruf"),
    ProTile(Icons.Filled.Visibility, "Mode fokus"),
    ProTile(Icons.Filled.InsertChart, "Statistik harian"),
    ProTile(Icons.AutoMirrored.Filled.Label, "Tag buku"),
    ProTile(Icons.Filled.RestoreFromTrash, "Sampah & pulihkan"),
    ProTile(Icons.Filled.ContentCopy, "Duplikat naskah"),
    ProTile(Icons.Filled.AccountTree, "Pindah bagian"),
    ProTile(Icons.Filled.Save, "Cadangan JSON"),
    ProTile(Icons.Filled.Timer, "Estimasi baca"),
)

private val proHighlights = listOf(
    ProHighlight(
        icon = Icons.Filled.Edit,
        title = "Editor tanpa gangguan",
        body = "Simpan otomatis setiap jeda ketik, hitungan kata, dan pratinjau Markdown di layar yang sama.",
    ),
    ProHighlight(
        icon = Icons.Filled.History,
        title = "Riwayat yang tidak hilang",
        body = "Snapshot otomatis saat naskah dibuka dan tiap dua menit. Bandingkan baris, lalu pulihkan.",
    ),
    ProHighlight(
        icon = Icons.Filled.CloudOff,
        title = "Tinggal di perangkatmu",
        body = "Satu berkas JSON di penyimpanan privat. Tanpa akun, awan, iklan, atau pelacakan.",
    ),
)

private val proReasons = listOf(
    ProTile(Icons.Filled.AllInclusive, "Kata tanpa batas"),
    ProTile(Icons.Filled.Speed, "Menulis tetap lancar"),
    ProTile(Icons.Filled.NightsStay, "Mode malam"),
    ProTile(Icons.Filled.FormatSize, "Ukuran huruf"),
    ProTile(Icons.Filled.FormatLineSpacing, "Jarak baris"),
    ProTile(Icons.Filled.Today, "Target kata harian"),
    ProTile(Icons.Filled.Shield, "Tanpa iklan"),
    ProTile(Icons.Filled.Search, "Cari lintas buku"),
    ProTile(Icons.Filled.Lock, "Data milikmu"),
    ProTile(Icons.Filled.AutoStories, "Pustaka bertingkat"),
    ProTile(Icons.Filled.FormatQuote, "Frasa cepat"),
    ProTile(Icons.Filled.Edit, "Judul kapan saja"),
)

@Composable
fun ProScreen(onClose: () -> Unit) {
    val colors = LocalTintaColors.current

    Scaffold(
        containerColor = colors.bg,
        topBar = { ProTopBar(onClose = onClose) },
        bottomBar = { ProActionBar(onClose = onClose) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item { SectionBand("Tinta Pro") }
            items(proTiles.chunked(4)) { row -> TileRow(row) }

            item { SectionBand("Isi Tinta Pro") }
            items(proHighlights) { highlight -> HighlightRow(highlight) }

            item { SectionBand("Alasan lain memakai Tinta") }
            items(proReasons.chunked(4)) { row -> TileRow(row) }

            item { ClosingNote() }
        }
    }
}

@Composable
private fun ProTopBar(onClose: () -> Unit) {
    val colors = LocalTintaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Tutup",
                tint = colors.ink,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Tinta Pro",
            style = MaterialTheme.typography.titleLarge,
            color = colors.ink,
        )
    }
}

@Composable
private fun SectionBand(title: String) {
    val colors = LocalTintaColors.current
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(colors.surface)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

@Composable
private fun TileRow(row: List<ProTile>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        row.forEach { tile -> Tile(tile, Modifier.weight(1f)) }
        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun Tile(tile: ProTile, modifier: Modifier = Modifier) {
    val colors = LocalTintaColors.current
    Column(
        modifier = modifier.padding(horizontal = 5.dp, vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tile.icon,
            contentDescription = null,
            tint = colors.ink,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = tile.label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.ink,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

@Composable
private fun HighlightRow(highlight: ProHighlight) {
    val colors = LocalTintaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = highlight.icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = highlight.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = highlight.body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun ClosingNote() {
    val colors = LocalTintaColors.current
    Text(
        text = "Tinta tumbuh dari kebiasaan menulis, bukan dari langganan. Tidak ada iklan, akun, atau pelacakan di dalamnya.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp),
    )
}

@Composable
private fun ProActionBar(onClose: () -> Unit) {
    val colors = LocalTintaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Sudah aktif",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink,
                maxLines = 1,
            )
            Text(
                text = "Tanpa langganan",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
                maxLines = 1,
            )
        }
        Button(
            onClick = onClose,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent,
            ),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Lanjut", style = MaterialTheme.typography.labelLarge)
        }
    }
}
