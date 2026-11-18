package id.tinta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.ui.theme.TintaColors

fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                    ) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}

/**
 * Satu blok tampilan pratinjau. Pratinjau dipecah per blok (paragraf, heading, butir, kutipan)
 * lalu digambar oleh `LazyColumn`, bukan satu kolom raksasa, sehingga hanya blok yang terlihat
 * yang diukur dan digambar. Blok tidak berubah selama naskah tidak berubah.
 */
private enum class BlockKind { GAP, H1, H2, H3, QUOTE, BULLET, NUMBERED, RULE, TEXT }

private data class PreviewBlock(
    val kind: BlockKind,
    val text: String,
    val marker: String = "",
)

/** `12. ` di awal baris, atau null bila bukan butir bernomor. */
private fun numberedPrefix(line: String): String? {
    val dot = line.indexOf('.')
    if (dot <= 0 || dot > 6) return null
    if (!line.take(dot).all { it.isDigit() }) return null
    if (line.getOrNull(dot + 1) != ' ') return null
    return line.substring(0, dot + 1)
}

private fun previewBlocks(body: String): List<PreviewBlock> {
    val blocks = mutableListOf<PreviewBlock>()
    val paragraph = StringBuilder()

    fun flush() {
        if (paragraph.isNotEmpty()) {
            blocks.add(PreviewBlock(BlockKind.TEXT, paragraph.toString()))
            paragraph.setLength(0)
        }
    }

    body.lines().forEach { raw ->
        val line = raw.trimEnd()
        when {
            line.isBlank() -> {
                flush()
                blocks.add(PreviewBlock(BlockKind.GAP, ""))
            }
            line.startsWith("# ") -> {
                flush()
                blocks.add(PreviewBlock(BlockKind.H1, line.removePrefix("# ")))
            }
            line.startsWith("## ") -> {
                flush()
                blocks.add(PreviewBlock(BlockKind.H2, line.removePrefix("## ")))
            }
            line.startsWith("### ") -> {
                flush()
                blocks.add(PreviewBlock(BlockKind.H3, line.removePrefix("### ")))
            }
            line.startsWith("> ") -> {
                flush()
                blocks.add(PreviewBlock(BlockKind.QUOTE, line.removePrefix("> ")))
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                flush()
                blocks.add(PreviewBlock(BlockKind.BULLET, line.drop(2)))
            }
            numberedPrefix(line) != null -> {
                flush()
                val prefix = numberedPrefix(line)!!
                blocks.add(
                    PreviewBlock(
                        kind = BlockKind.NUMBERED,
                        text = line.drop(prefix.length).trimStart(),
                        marker = prefix,
                    )
                )
            }
            line == "---" || line == "***" -> {
                flush()
                blocks.add(PreviewBlock(BlockKind.RULE, ""))
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(line)
            }
        }
    }
    flush()
    return blocks
}

@Composable
fun MarkdownPreview(body: String, modifier: Modifier = Modifier) {
    val blocks = remember(body) { previewBlocks(body) }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 24.dp),
    ) {
        if (blocks.isEmpty()) {
            item { Text("Pratinjau akan muncul di sini.", color = LocalTintaColors.current.muted) }
        }
        items(blocks.size, key = { index -> index }) { index ->
            PreviewBlockView(blocks[index])
        }
    }
}

@Composable
private fun PreviewBlockView(block: PreviewBlock) {
    val colors = LocalTintaColors.current
    when (block.kind) {
        BlockKind.GAP -> Spacer(Modifier.height(10.dp))
        BlockKind.H1 -> Text(
            inlineMarkdown(block.text),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.ink,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        BlockKind.H2 -> Text(
            inlineMarkdown(block.text),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.ink,
            modifier = Modifier.padding(top = 7.dp, bottom = 5.dp),
        )
        BlockKind.H3 -> Text(
            inlineMarkdown(block.text),
            style = MaterialTheme.typography.titleMedium,
            color = colors.ink,
            modifier = Modifier.padding(top = 5.dp, bottom = 4.dp),
        )
        BlockKind.QUOTE -> Row(Modifier.padding(vertical = 4.dp)) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                inlineMarkdown(block.text),
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = colors.muted,
            )
        }
        BlockKind.BULLET -> Row(Modifier.padding(vertical = 3.dp)) {
            Text("•", color = colors.accent, fontSize = 18.sp)
            Spacer(Modifier.width(9.dp))
            Text(
                inlineMarkdown(block.text),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                color = colors.ink,
            )
        }
        BlockKind.NUMBERED -> Row(Modifier.padding(vertical = 3.dp)) {
            Text(block.marker, color = colors.accent, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(7.dp))
            Text(
                inlineMarkdown(block.text),
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                color = colors.ink,
            )
        }
        BlockKind.RULE -> Box(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .height(1.dp)
                .background(colors.border)
        )
        BlockKind.TEXT -> Text(
            inlineMarkdown(block.text),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                lineHeight = 28.sp,
            ),
            color = colors.ink,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}
