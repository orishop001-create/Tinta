package id.tinta.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.TintaApp
import id.tinta.app.ui.components.LockScreen
import id.tinta.app.ui.theme.TintaTheme

class MainActivity : FragmentActivity() {
    private lateinit var store: TintaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = TintaStore(applicationContext)
        if (savedInstanceState == null) captureShared(intent)
        setContent {
            val data by store.state.collectAsStateWithLifecycle()
            var unlocked by remember { mutableStateOf(false) }
            TintaTheme(data.settings.theme) {
                if (data.settings.lock && !unlocked) {
                    LockScreen(onUnlocked = { unlocked = true })
                } else {
                    TintaApp(store)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureShared(intent)
    }

    /** "Bagikan ke Tinta": teks dari aplikasi lain masuk ke buku Kotak ide. */
    private fun captureShared(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type?.startsWith("text/") != true) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (text.isBlank()) return
        intent.removeExtra(Intent.EXTRA_TEXT)
        runCatching { store.captureIdea(text) }.onSuccess {
            Toast.makeText(this, "Tersimpan di Kotak ide", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        store.autoBackup()
        store.flush()
        super.onStop()
    }
}
