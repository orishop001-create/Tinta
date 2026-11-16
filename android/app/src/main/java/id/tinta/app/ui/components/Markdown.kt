package id.tinta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@Composable
fun MarkdownPreview(body: String, modifier: Modifier = Modifier) {
    val colors = LocalTintaColors.current
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        if (body.isBlank()) {
            Text("Pratinjau akan muncul di sini.", color = colors.muted)
        }
        body.lines().forEach { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> Spacer(Modifier.height(10.dp))
                line.startsWith("# ") -> {
                    Text(
                        inlineMarkdown(line.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.ink,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        inlineMarkdown(line.removePrefix("## ")),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.ink,
                        modifier = Modifier.padding(top = 7.dp, bottom = 5.dp),
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        inlineMarkdown(line.removePrefix("### ")),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink,
                        modifier = Modifier.padding(top = 5.dp, bottom = 4.dp),
                    )
                }
                line.startsWith("> ") -> {
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.accent)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            inlineMarkdown(line.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            color = colors.muted,
                        )
                    }
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•", color = colors.accent, fontSize = 18.sp)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            inlineMarkdown(line.drop(2)),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                            color = colors.ink,
                        )
                    }
                }
                Regex("^\\d+\\.\\s").containsMatchIn(line) -> {
                    val dot = line.indexOf('.')
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text(line.take(dot + 1), color = colors.accent, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            inlineMarkdown(line.drop(dot + 1).trimStart()),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
                            color = colors.ink,
                        )
                    }
                }
                line == "---" || line == "***" -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .height(1.dp)
                            .background(colors.border)
                    )
                }
                else -> {
                    Text(
                        inlineMarkdown(line),
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
        }
    }
}
