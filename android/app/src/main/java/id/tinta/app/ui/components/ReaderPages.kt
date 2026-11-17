package id.tinta.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.BoxWithConstraints
import id.tinta.app.ui.theme.LocalTintaColors

/**
 * Pratinjau berhalaman: naskah dipotong mengikuti ukuran layar dan gaya tulis yang dipilih,
 * sehingga penulis melihat di mana halaman pecah sebelum diekspor.
 */
@Composable
fun ReaderPages(
    title: String,
    body: String,
    style: TextStyle,
    onClose: () -> Unit,
) {
    val colors = LocalTintaColors.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = colors.bg) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title.ifBlank { "Tanpa judul" },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = colors.muted)
                    }
                }

                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val paragraphs = remember(body) { MarkdownDocument.parse(body).plainParagraphs() }
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
                    val heightPx = with(density) { (maxHeight - 26.dp).toPx() }.toInt().coerceAtLeast(1)
                    val pages = remember(paragraphs, widthPx, heightPx, style, measurer) {
                        paginate(paragraphs, measurer, style, widthPx, heightPx)
                    }
                    val pager = rememberPagerState { pages.size }
                    Column(Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pager,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) { index ->
                            Text(
                                text = pages[index],
                                style = style,
                                color = colors.ink,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 10.dp),
                            )
                        }
                        Text(
                            "${pager.currentPage + 1} / ${pages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.muted,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

private fun measureLines(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    widthPx: Int,
): Int = measurer.measure(
    text = AnnotatedString(text.ifEmpty { " " }),
    style = style,
    constraints = Constraints(maxWidth = widthPx),
).lineCount

private fun lineHeight(measurer: TextMeasurer, style: TextStyle, widthPx: Int): Int =
    measurer.measure(
        text = AnnotatedString("Ag"),
        style = style,
        constraints = Constraints(maxWidth = widthPx),
    ).size.height.coerceAtLeast(1)

private fun largestPrefix(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    widthPx: Int,
    maxLines: Int,
): Int {
    var low = 1
    var high = text.length
    var best = 0
    while (low <= high) {
        val mid = (low + high) / 2
        if (measureLines(measurer, text.substring(0, mid), style, widthPx) <= maxLines) {
            best = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    if (best == 0) return 0
    val space = text.lastIndexOf(' ', best - 1)
    return if (space >= 1) space else best
}

private fun paginate(
    paragraphs: List<String>,
    measurer: TextMeasurer,
    style: TextStyle,
    widthPx: Int,
    heightPx: Int,
): List<String> {
    if (paragraphs.isEmpty()) return listOf("")
    val capacity = (heightPx / lineHeight(measurer, style, widthPx)).coerceAtLeast(2)
    val pages = mutableListOf<String>()
    val current = mutableListOf<String>()
    var used = 0

    fun flush() {
        if (current.isNotEmpty()) {
            pages.add(current.joinToString("\n\n"))
            current.clear()
        }
        used = 0
    }

    for (paragraph in paragraphs) {
        var remaining = paragraph
        while (remaining.isNotEmpty()) {
            val lines = measureLines(measurer, remaining, style, widthPx)
            val room = capacity - used
            if (lines <= room) {
                current.add(remaining)
                used += lines + 1
                remaining = ""
                continue
            }
            if (room <= 1) {
                flush()
                continue
            }
            val take = largestPrefix(measurer, remaining, style, widthPx, room)
            if (take <= 0) {
                flush()
                continue
            }
            current.add(remaining.substring(0, take).trim())
            flush()
            remaining = remaining.substring(take).trim()
        }
    }
    flush()
    return pages.ifEmpty { listOf("") }
}
