package id.tinta.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import id.tinta.app.ui.theme.LocalTintaColors

@Composable
fun FindReplaceDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    replacement: String,
    onReplacementChange: (String) -> Unit,
    matches: Int,
    current: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalTintaColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = { Text("Cari & ganti", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    label = { Text("Cari") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = replacement,
                    onValueChange = onReplacementChange,
                    singleLine = true,
                    label = { Text("Ganti dengan") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (matches == 0) "Tidak ada kecocokan" else "${current.coerceAtLeast(1)} dari $matches kecocokan",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onPrev, enabled = matches > 0) { Text("Sebelumnya", color = colors.accent) }
                    TextButton(onClick = onNext, enabled = matches > 0) { Text("Berikutnya", color = colors.accent) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onReplace, enabled = matches > 0) { Text("Ganti", color = colors.accent) }
                    TextButton(onClick = onReplaceAll, enabled = matches > 0) {
                        Text("Ganti semua", color = colors.accent)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = colors.muted) }
        },
    )
}

@Composable
fun OutlineDialog(
    headings: List<MarkdownHeading>,
    onJump: (MarkdownHeading) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalTintaColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = { Text("Kerangka bab", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (headings.isEmpty()) {
                    Text(
                        "Belum ada heading. Tulis `## Judul` atau tekan tombol H2 untuk membuat kerangka.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.muted,
                    )
                }
                headings.forEach { heading ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onJump(heading) }
                            .padding(start = ((heading.level - 1).coerceIn(0, 5) * 14).dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
                    ) {
                        Text(
                            heading.title.ifBlank { "Tanpa judul" },
                            style = if (heading.level <= 1) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            color = colors.ink,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = colors.muted) }
        },
    )
}
