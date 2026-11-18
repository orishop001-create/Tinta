package id.tinta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.SpellIssue

/** Daftar temuan ejaan; menekan satu baris membawa kursor ke kata itu di editor. */
@Composable
fun SpellcheckSheet(
    text: String,
    issues: List<SpellIssue>,
    onJump: (SpellIssue) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalTintaColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = {
            Text(
                if (issues.isEmpty()) "Ejaan bersih" else "${issues.size} temuan ejaan",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            if (issues.isEmpty()) {
                Text(
                    "Tidak ada kata baku, spasi, tanda baca, atau nama tokoh yang mencurigakan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    issues.forEach { issue ->
                        val start = (issue.start - 24).coerceAtLeast(0)
                        val end = (issue.end + 24).coerceAtMost(text.length)
                        val snippet = text.substring(start, end).replace(Regex("\\s+"), " ")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.bg)
                                .clickable { onJump(issue) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    issue.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.ink,
                                )
                                Text(
                                    "…$snippet…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted,
                                    maxLines = 2,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = colors.muted) }
        },
    )
}
