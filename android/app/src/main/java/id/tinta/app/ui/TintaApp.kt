package id.tinta.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
import id.tinta.app.ui.screens.LoreScreen
import id.tinta.app.ui.screens.SearchScreen
import id.tinta.app.ui.screens.SettingsScreen
import id.tinta.app.ui.screens.StatsScreen
import id.tinta.app.ui.screens.TrashScreen
import id.tinta.app.ui.theme.LocalTintaColors
import id.tinta.app.util.Export
import id.tinta.app.util.Covers
import id.tinta.app.util.Text

@Composable
fun TintaApp(store: TintaStore) {
    val nav = rememberNavController()
    val colors = LocalTintaColors.current
    val context = LocalContext.current
    val data by store.state.collectAsStateWithLifecycle()
    var featureBookId by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<ByteArray?>(null) }

    fun writeExport(uri: Uri?) {
        val bytes = pendingExport ?: return
        pendingExport = null
        if (uri == null) return
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        }
    }

    val markdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri -> writeExport(uri) }
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> writeExport(uri) }
    val epubLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip"),
    ) { uri -> writeExport(uri) }
    val docxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        ),
    ) { uri -> writeExport(uri) }

    fun chaptersOf(bookId: String) = data.chapters
        .filter { it.bookId == bookId && it.deletedAt == null }
        .sortedBy { it.order }

    fun exportBook(bookId: String, pdf: Boolean) {
        val book = data.books.firstOrNull { it.id == bookId } ?: return
        val chapters = chaptersOf(bookId)
        val name = Text.slugify(book.title)
        if (pdf) {
            pendingExport = Export.bookPdf(book, chapters, Covers.bitmap(context, book.coverImage))
            pdfLauncher.launch("$name.pdf")
        } else {
            pendingExport = Export.bookMarkdown(book, chapters).toByteArray(Charsets.UTF_8)
            markdownLauncher.launch("$name.md")
        }
    }

    fun exportEpub(bookId: String) {
        val book = data.books.firstOrNull { it.id == bookId } ?: return
        pendingExport =
            Export.bookEpub(book, chaptersOf(bookId), Covers.bitmap(context, book.coverImage))
        epubLauncher.launch("${Text.slugify(book.title)}.epub")
    }

    fun exportDocx(bookId: String) {
        val book = data.books.firstOrNull { it.id == bookId } ?: return
        pendingExport = Export.bookDocx(book, chaptersOf(bookId))
        docxLauncher.launch("${Text.slugify(book.title)}.docx")
    }

    fun shareBook(bookId: String) {
        val book = data.books.firstOrNull { it.id == bookId } ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, book.title)
            putExtra(Intent.EXTRA_TEXT, Export.bookPlainText(book, chaptersOf(bookId)))
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan buku"))
    }

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
                    onExportMarkdown = { bookId -> exportBook(bookId, pdf = false) },
                    onExportPdf = { bookId -> exportBook(bookId, pdf = true) },
                    onExportEpub = { bookId -> exportEpub(bookId) },
                    onExportDocx = { bookId -> exportDocx(bookId) },
                    onImported = { imported ->
                        val id = store.importBook(
                            imported.title,
                            imported.chapters.map { it.title to it.body },
                        )
                        nav.navigate("book/$id")
                    },
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
                    onExportMarkdown = { exportBook(id, pdf = false) },
                    onExportPdf = { exportBook(id, pdf = true) },
                    onExportEpub = { exportEpub(id) },
                    onExportDocx = { exportDocx(id) },
                    onLore = { nav.navigate("lore/$id") },
                )
            }
            composable(
                "editor/{id}?panel={panel}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("panel") {
                        type = NavType.StringType
                        defaultValue = "none"
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                val panel = entry.arguments?.getString("panel")?.takeIf { it != "none" }
                EditorScreen(
                    store = store,
                    chapterId = id,
                    initialPanel = panel,
                    onLore = { bookId -> nav.navigate("lore/$bookId") },
                    onBack = { nav.popBackStack() },
                    onHistory = { nav.navigate("history/$id") },
                    onOpenChapter = { nextId ->
                        store.openChapter(nextId)
                        val bookId = data.chapters.firstOrNull { it.id == id }?.bookId
                        if (bookId != null) {
                            nav.navigate("editor/$nextId") {
                                popUpTo("book/$bookId") { inclusive = false }
                                launchSingleTop = true
                            }
                        } else {
                            nav.navigate("editor/$nextId") { launchSingleTop = true }
                        }
                    },
                )
            }
            composable(
                "lore/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id").orEmpty()
                LoreScreen(
                    store = store,
                    bookId = id,
                    onBack = { nav.popBackStack() },
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
            val lastChapter = chaptersOf(sheetBookId).maxByOrNull { it.updatedAt }

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
                        FeatureTarget.EXPORT_MARKDOWN -> exportBook(sheetBookId, pdf = false)
                        FeatureTarget.EXPORT_PDF -> exportBook(sheetBookId, pdf = true)
                        FeatureTarget.EXPORT_EPUB -> exportEpub(sheetBookId)
                        FeatureTarget.EXPORT_DOCX -> exportDocx(sheetBookId)
                        FeatureTarget.SHARE_BOOK -> shareBook(sheetBookId)
                    }
                },
            )
        }
    }
}
