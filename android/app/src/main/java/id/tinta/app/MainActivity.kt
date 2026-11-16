package id.tinta.app

import android.os.Bundle
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

    override fun onStop() {
        store.autoBackup()
        store.flush()
        super.onStop()
    }
}
