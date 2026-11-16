package id.tinta.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.tinta.app.data.TintaStore
import id.tinta.app.ui.components.FeatureSheet
import id.tinta.app.ui.components.FeatureTarget
import id.tinta.app.ui.screens.BookScreen
import id.tinta.app.ui.screens.EditorScreen
import id.tinta.app.ui.screens.FeaturesScreen
import id.tinta.app.ui.screens.HistoryScreen
import id.tinta.app.ui.screens.LibraryScreen
import id.tinta.app.ui.screens.SearchScreen
import id.tinta.app.ui.screens.SettingsScreen
import id.tinta.app.ui.screens.StatsScreen
import id.tinta.app.ui.screens.TrashScreen
import id.tinta.app.ui.theme.LocalTintaColors

@Composable
fun TintaApp(store: TintaStore) {
    val nav = rememberNavController()
    val colors = LocalTintaColors.current
    val data by store.state.collectAsStateWithLifecycle()
    var featureBookId by remember { mutableStateOf<String?>(null) }

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
                    onFeatures = { bookId -> featureBookId = bookId },
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
                    onFeatures = { featureBookId = id },
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
                    onFeatures = { nav.navigate("features") },
                )
            }
            composable("features") {
                FeaturesScreen(onClose = { nav.popBackStack() })
            }
            composable("trash") {
                TrashScreen(
                    store = store,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        val sheetBookId = featureBookId
        if (sheetBookId != null) {
            val lastChapter = data.chapters
                .filter { it.bookId == sheetBookId && it.deletedAt == null }
                .maxByOrNull { it.updatedAt }

            FeatureSheet(
                onDismiss = { featureBookId = null },
                onOpen = { target ->
                    featureBookId = null
                    val chapterId = lastChapter?.id
                    val fallback = "book/$sheetBookId"
                    when (target) {
                        FeatureTarget.LIBRARY -> nav.popBackStack("library", inclusive = false)
                        FeatureTarget.BOOK -> nav.navigate(fallback) { launchSingleTop = true }
                        FeatureTarget.EDITOR -> {
                            if (chapterId != null) {
                                store.openChapter(chapterId)
                                nav.navigate("editor/$chapterId") { launchSingleTop = true }
                            } else {
                                nav.navigate(fallback) { launchSingleTop = true }
                            }
                        }
                        FeatureTarget.HISTORY -> {
                            if (chapterId != null) {
                                nav.navigate("history/$chapterId") { launchSingleTop = true }
                            } else {
                                nav.navigate(fallback) { launchSingleTop = true }
                            }
                        }
                        FeatureTarget.SEARCH -> nav.navigate("search") { launchSingleTop = true }
                        FeatureTarget.STATS -> nav.navigate("stats") { launchSingleTop = true }
                        FeatureTarget.TRASH -> nav.navigate("trash") { launchSingleTop = true }
                        FeatureTarget.SETTINGS -> nav.navigate("settings") { launchSingleTop = true }
                    }
                },
            )
        }
    }
}
