package id.tinta.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.TintaApp
import id.tinta.app.ui.theme.TintaTheme

class MainActivity : ComponentActivity() {
    private lateinit var store: TintaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = TintaStore(applicationContext)
        setContent {
            val data by store.state.collectAsStateWithLifecycle()
            TintaTheme(data.settings.theme) {
                TintaApp(store)
            }
        }
    }

    override fun onStop() {
        store.flush()
        super.onStop()
    }
}
