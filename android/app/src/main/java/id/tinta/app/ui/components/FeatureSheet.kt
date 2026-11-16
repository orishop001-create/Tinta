package id.tinta.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoStories
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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import id.tinta.app.ui.theme.LocalTintaColors

data class FeatureTile(val icon: ImageVector, val label: String)

val coreTiles = listOf(
    FeatureTile(Icons.Filled.Palette, "Empat tema kertas"),
    FeatureTile(Icons.Filled.Code, "Pratinjau Markdown"),
    FeatureTile(Icons.Filled.Download, "Ekspor Markdown"),
    FeatureTile(Icons.Filled.Share, "Bagikan naskah"),
    FeatureTile(Icons.Filled.FindReplace, "Cari & ganti"),
    FeatureTile(Icons.AutoMirrored.Filled.MenuBook, "Buku, bagian, bab"),
    FeatureTile(Icons.Filled.History, "Riwayat versi"),
    FeatureTile(Icons.Filled.TextFields, "Tiga jenis huruf"),
    FeatureTile(Icons.Filled.Visibility, "Mode fokus"),
    FeatureTile(Icons.Filled.InsertChart, "Statistik harian"),
    FeatureTile(Icons.AutoMirrored.Filled.Label, "Tag buku"),
    FeatureTile(Icons.Filled.RestoreFromTrash, "Sampah & pulihkan"),
    FeatureTile(Icons.Filled.ContentCopy, "Duplikat naskah"),
    FeatureTile(Icons.Filled.AccountTree, "Pindah bagian"),
    FeatureTile(Icons.Filled.Save, "Cadangan JSON"),
    FeatureTile(Icons.Filled.Timer, "Estimasi baca"),
)

val reasons = listOf(
    FeatureTile(Icons.Filled.AllInclusive, "Kata tanpa batas"),
    FeatureTile(Icons.Filled.Speed, "Menulis tetap lancar"),
    FeatureTile(Icons.Filled.NightsStay, "Mode malam"),
    FeatureTile(Icons.Filled.FormatSize, "Ukuran huruf"),
    FeatureTile(Icons.Filled.FormatLineSpacing, "Jarak baris"),
    FeatureTile(Icons.Filled.Today, "Target kata harian"),
    FeatureTile(Icons.Filled.Shield, "Tanpa iklan"),
    FeatureTile(Icons.Filled.Search, "Cari lintas buku"),
    FeatureTile(Icons.Filled.Lock, "Data milikmu"),
    FeatureTile(Icons.Filled.AutoStories, "Pustaka bertingkat"),
    FeatureTile(Icons.Filled.FormatQuote, "Frasa cepat"),
    FeatureTile(Icons.Filled.Edit, "Judul kapan saja"),
)

val sheetFeatures: List<FeatureTile> = coreTiles + reasons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureSheet(onDismiss: () -> Unit) {
    val colors = LocalTintaColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        contentColor = colors.ink,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.border) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, bottom = 18.dp),
        ) {
            Text(
                text = "Semua fitur",
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${sheetFeatures.size} fitur terbuka. Gratis, tanpa langganan.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(sheetFeatures.chunked(2)) { row -> FeatureSheetRow(row) }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Tanpa akun, iklan, atau kunci fitur.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.muted,
            )
        }
    }
}

@Composable
private fun FeatureSheetRow(row: List<FeatureTile>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.forEach { tile -> FeatureSheetCell(tile, Modifier.weight(1f)) }
        if (row.size == 1) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeatureSheetCell(tile: FeatureTile, modifier: Modifier = Modifier) {
    val colors = LocalTintaColors.current
    Row(
        modifier = modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = tile.icon,
            contentDescription = null,
            tint = colors.muted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = tile.label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            maxLines = 2,
        )
    }
}
