package id.tinta.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.screens.BookScreen
import id.tinta.app.ui.screens.EditorScreen
import id.tinta.app.ui.screens.HistoryScreen
import id.tinta.app.ui.screens.LibraryScreen
import id.tinta.app.ui.screens.ProScreen
import id.tinta.app.ui.screens.SearchScreen
import id.tinta.app.ui.screens.SettingsScreen
import id.tinta.app.ui.screens.StatsScreen
import id.tinta.app.ui.screens.TrashScreen
import id.tinta.app.ui.theme.LocalTintaColors

@Composable
fun TintaApp(store: TintaStore) {
    val nav = rememberNavController()
    val colors = LocalTintaColors.current

    Surface(modifier = Modifier.fillMaxSize(), color = colors.bg) {
        NavHost(
            navController = nav,
            startDestination = "library",
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            composable("library") {
                LibraryScreen(
                    store = store,
                    onOpenBook = { nav.navigate("book/$it") },
                    onOpenChapter = { id ->
                        store.openChapter(id)
                        nav.navigate("editor/$id")
                    },
                    onSearch = { nav.navigate("search") },
                    onStats = { nav.navigate("stats") },
                    onSettings = { nav.navigate("settings") },
                )
            }
            composable(
                "book/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                BookScreen(
                    store = store,
                    bookId = id,
                    onBack = { nav.popBackStack() },
                    onOpenChapter = { chapterId ->
                        store.openChapter(chapterId)
                        nav.navigate("editor/$chapterId")
                    },
                )
            }
            composable(
                "editor/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                EditorScreen(
                    store = store,
                    chapterId = id,
                    onBack = { nav.popBackStack() },
                    onHistory = { nav.navigate("history/$id") },
                )
            }
            composable(
                "history/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                HistoryScreen(
                    store = store,
                    chapterId = id,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("search") {
                SearchScreen(
                    store = store,
                    onBack = { nav.popBackStack() },
                    onOpenChapter = { id ->
                        store.openChapter(id)
                        nav.navigate("editor/$id")
                    },
                )
            }
            composable("stats") {
                StatsScreen(store = store, onBack = { nav.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(
                    store = store,
                    onBack = { nav.popBackStack() },
                    onTrash = { nav.navigate("trash") },
                    onPro = { nav.navigate("pro") },
                )
            }
            composable("pro") {
                ProScreen(onClose = { nav.popBackStack() })
            }
            composable("trash") {
                TrashScreen(
                    store = store,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
