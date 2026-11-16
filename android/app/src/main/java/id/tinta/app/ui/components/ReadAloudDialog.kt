package id.tinta.app.ui.components

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.tinta.app.ui.theme.LocalTintaColors
import java.util.Locale

/**
 * Membacakan naskah dengan mesin suara bawaan perangkat. Tidak ada audio yang
 * dikirim ke server; sorotan kata mengikuti callback onRangeStart dari mesin TTS.
 */
@Composable
fun ReadAloudDialog(
    text: String,
    onDismiss: () -> Unit,
) {
    val colors = LocalTintaColors.current
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    var speaking by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var highlight by remember { mutableStateOf<IntRange?>(null) }
    val plain = remember(text) { text.trim() }

    DisposableEffect(Unit) {
        val holder = arrayOfNulls<TextToSpeech>(1)
        val created = TextToSpeech(context) { status ->
            val tts = holder[0]
            if (status != TextToSpeech.SUCCESS || tts == null) {
                problem = "Mesin suara tidak tersedia di perangkat ini."
                return@TextToSpeech
            }
            val indonesian = tts.setLanguage(Locale("id", "ID"))
            if (indonesian == TextToSpeech.LANG_MISSING_DATA ||
                indonesian == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts.setLanguage(Locale.getDefault())
            }
            tts.setSpeechRate(0.95f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    speaking = true
                }

                override fun onDone(utteranceId: String?) {
                    speaking = false
                    highlight = null
                }

                @Deprecated("Kompatibilitas mesin suara lama")
                override fun onError(utteranceId: String?) {
                    speaking = false
                    problem = "Mesin suara menghentikan bacaan."
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (start in 0..end && end <= plain.length) {
                        highlight = start until end
                    }
                }
            })
            ready = true
        }
        holder[0] = created
        engine = created
        onDispose {
            created.stop()
            created.shutdown()
        }
    }

    LaunchedEffect(highlight) {
        val range = highlight ?: return@LaunchedEffect
        if (plain.isEmpty()) return@LaunchedEffect
        val fraction = (range.first.toFloat() / plain.length).coerceIn(0f, 1f)
        runCatching { scroll.animateScrollTo((fraction * scroll.maxValue).toInt()) }
    }

    fun read() {
        val tts = engine ?: return
        if (plain.isEmpty()) return
        highlight = null
        val result = tts.speak(plain, TextToSpeech.QUEUE_FLUSH, null, "tinta-baca")
        if (result != TextToSpeech.SUCCESS) {
            problem = "Bacaan gagal dimulai. Coba lagi."
        }
    }

    val annotated = remember(highlight, plain, colors) {
        buildAnnotatedString {
            val range = highlight
            if (range == null || range.first < 0 || range.last >= plain.length) {
                append(plain)
            } else {
                append(plain.substring(0, range.first))
                withStyle(SpanStyle(background = colors.accentSoft, color = colors.ink)) {
                    append(plain.substring(range.first, range.last + 1))
                }
                append(plain.substring(range.last + 1))
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            engine?.stop()
            onDismiss()
        },
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = { Text("Baca nyaring", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    "Mesin suara perangkat membacakan naskah ini. Kata yang sedang dibaca disorot, " +
                        "jadi telinga dan mata bisa menangkap kalimat yang janggal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                )
                if (problem != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        problem!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.danger,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    annotated,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                    ),
                    color = colors.ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.bg)
                        .padding(12.dp)
                        .verticalScroll(scroll),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (speaking) {
                        engine?.stop()
                        speaking = false
                        highlight = null
                    } else {
                        read()
                    }
                },
                enabled = ready && plain.isNotEmpty(),
            ) {
                Text(
                    if (speaking) "Berhenti" else "Bacakan",
                    color = if (ready) colors.accent else colors.muted,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                engine?.stop()
                onDismiss()
            }) {
                Text("Tutup", color = colors.muted)
            }
        },
    )
}
