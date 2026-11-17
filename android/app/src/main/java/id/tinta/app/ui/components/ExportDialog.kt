package id.tinta.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.tinta.app.ui.theme.LocalTintaColors

/** Satu pintu ekspor: pengguna memilih format di sini, bukan dari menu panjang. */
@Composable
fun ExportBookDialog(
    title: String,
    onDismiss: () -> Unit,
    onMarkdown: () -> Unit,
    onPdf: () -> Unit,
    onEpub: () -> Unit,
    onDocx: () -> Unit,
) {
    val colors = LocalTintaColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = { Text("Ekspor buku", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "Pilih format untuk “${title.ifBlank { "Tanpa judul" }}”. Semua dibuat di " +
                        "perangkat ini; tidak ada yang diunggah.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
                Spacer(Modifier.height(10.dp))
                ExportOption(
                    label = "Markdown (.md)",
                    detail = "Teks polos, judul bab, siap dipindah ke editor lain",
                    onClick = {
                        onDismiss()
                        onMarkdown()
                    },
                )
                ExportOption(
                    label = "PDF (.pdf)",
                    detail = "Siap cetak: sampul, margin, dan nomor halaman",
                    onClick = {
                        onDismiss()
                        onPdf()
                    },
                )
                ExportOption(
                    label = "EPUB (.epub)",
                    detail = "Untuk pembaca e-book, lengkap dengan sampul dan daftar isi",
                    onClick = {
                        onDismiss()
                        onEpub()
                    },
                )
                ExportOption(
                    label = "DOCX (.docx)",
                    detail = "Untuk Word atau Google Docs",
                    onClick = {
                        onDismiss()
                        onDocx()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = colors.muted) }
        },
    )
}

@Composable
private fun ExportOption(label: String, detail: String, onClick: () -> Unit) {
    val colors = LocalTintaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
            Text(detail, style = MaterialTheme.typography.labelSmall, color = colors.muted)
        }
    }
}
