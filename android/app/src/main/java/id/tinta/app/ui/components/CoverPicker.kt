package id.tinta.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Covers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dekode sampul di luar main thread, dengan cache supaya gulir tidak tersendat. */
@Composable
fun rememberCover(name: String?, max: Int = 480): ImageBitmap? {
    val context = LocalContext.current
    val key = name.orEmpty()
    var image by remember(key, max) { mutableStateOf(Covers.cached(key, max)?.asImageBitmap()) }
    LaunchedEffect(key, max) {
        if (key.isBlank() || image != null) return@LaunchedEffect
        image = withContext(Dispatchers.IO) { Covers.bitmap(context, key, max)?.asImageBitmap() }
    }
    return image
}

/** Sampul kecil untuk bagian dan bab; hanya tampil kalau gambarnya ada. */
@Composable
fun CoverThumb(image: String?, ratio: String, modifier: Modifier = Modifier) {
    val picture = rememberCover(image, 240) ?: return
    Image(
        bitmap = picture,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .then(
                Covers.aspect(ratio, picture.width, picture.height)
                    ?.let { Modifier.aspectRatio(it) } ?: Modifier,
            )
            .clip(RoundedCornerShape(5.dp)),
    )
}

/** Pratinjau dan pilihan bingkai sebelum sampul disimpan. */
@Composable
fun CoverFrameDialog(
    image: String?,
    initialRatio: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val colors = LocalTintaColors.current
    val context = LocalContext.current
    val bitmap = remember(image) { Covers.bitmap(context, image) }
    var ratio by remember(image) { mutableStateOf(initialRatio.ifBlank { "2:3" }) }
    val aspect = Covers.aspect(ratio, bitmap?.width ?: 0, bitmap?.height ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.ink,
        title = { Text("Bingkai sampul", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Pratinjau sampul",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(150.dp)
                                .then(aspect?.let { Modifier.aspectRatio(it) } ?: Modifier)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Text(
                            "Gambar tidak terbaca. Coba pilih berkas lain.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.danger,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("2:3", "9:16", "asli").forEach { option ->
                        Text(
                            Covers.label(option) + if (option == "2:3") " · disarankan" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ratio == option) colors.accent else colors.muted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (ratio == option) colors.accentSoft else colors.bg)
                                .clickable { ratio = option }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "2:3 adalah rasio standar sampul buku dan paling aman untuk pustaka, EPUB, " +
                        "maupun cetak. 9:16 pas untuk sampul bergaya layar ponsel, sedangkan Potret " +
                        "asli mengikuti rasio fotomu sendiri.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.muted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(ratio) }, enabled = bitmap != null) {
                Text("Pakai bingkai", color = if (bitmap != null) colors.accent else colors.muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = colors.muted) }
        },
    )
}
