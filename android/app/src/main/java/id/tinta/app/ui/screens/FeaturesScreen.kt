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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
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
import id.tinta.app.ui.components.FeatureTile
import id.tinta.app.ui.components.coreTiles
import id.tinta.app.ui.components.reasons
import id.tinta.app.ui.theme.LocalTintaColors

private data class Highlight(val icon: ImageVector, val title: String, val body: String)

private val highlights = listOf(
    Highlight(
        icon = Icons.Filled.Edit,
        title = "Editor tanpa gangguan",
        body = "Simpan otomatis setiap jeda ketik, hitungan kata, dan pratinjau Markdown di layar yang sama.",
    ),
    Highlight(
        icon = Icons.Filled.History,
        title = "Riwayat yang tidak hilang",
        body = "Snapshot otomatis saat naskah dibuka dan tiap dua menit. Bandingkan baris, lalu pulihkan.",
    ),
    Highlight(
        icon = Icons.Filled.CloudOff,
        title = "Tinggal di perangkatmu",
        body = "Satu berkas JSON di penyimpanan privat. Tanpa akun, awan, iklan, atau pelacakan.",
    ),
)

@Composable
fun FeaturesScreen(onClose: () -> Unit) {
    val colors = LocalTintaColors.current

    Scaffold(
        containerColor = colors.bg,
        topBar = { FeaturesTopBar(onClose = onClose) },
        bottomBar = { FeaturesActionBar(onClose = onClose) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item { SectionBand("Fitur Tinta") }
            items(coreTiles.chunked(4)) { row -> TileRow(row) }

            item { SectionBand("Yang kamu dapat") }
            items(highlights) { highlight -> HighlightRow(highlight) }

            item { SectionBand("Alasan lain memakai Tinta") }
            items(reasons.chunked(4)) { row -> TileRow(row) }

            item { ClosingNote() }
        }
    }
}

@Composable
private fun FeaturesTopBar(onClose: () -> Unit) {
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
            text = "Fitur Tinta",
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
private fun TileRow(row: List<FeatureTile>) {
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
private fun Tile(tile: FeatureTile, modifier: Modifier = Modifier) {
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
private fun HighlightRow(highlight: Highlight) {
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
        text = "Semua fitur di halaman ini terbuka tanpa akun, langganan, atau pembelian. Tinta tumbuh dari kebiasaan menulis, bukan dari kunci fitur.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp),
    )
}

@Composable
private fun FeaturesActionBar(onClose: () -> Unit) {
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
                text = "Gratis selamanya",
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
