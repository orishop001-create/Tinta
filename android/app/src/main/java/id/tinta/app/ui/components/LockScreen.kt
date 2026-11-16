package id.tinta.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.AppLock

/** Layar kunci yang muncul sebelum naskah dibuka saat kunci aplikasi aktif. */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val colors = LocalTintaColors.current
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    fun unlock() {
        val activity = context as? FragmentActivity
        if (activity == null) {
            onUnlocked()
            return
        }
        message = null
        AppLock.prompt(
            activity = activity,
            onSuccess = onUnlocked,
            onError = { reason -> message = reason },
        )
    }

    LaunchedEffect(Unit) { unlock() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.width(40.dp).height(40.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Tinta terkunci",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.ink,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Naskahmu hanya terbuka setelah sidik jari atau PIN perangkat cocok.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.muted,
                textAlign = TextAlign.Center,
            )
            if (message != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    message!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.danger,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Buka dengan sidik jari / PIN", onClick = ::unlock)
        }
    }
}
