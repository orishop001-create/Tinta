# Native Kotlin build — Tinta

The user asked for a native Kotlin app after the web prototype. This document records that
second build; the web report (`parity.md`, `build-log.md`) still describes the browser
prototype and must not be read as native evidence.

## Decision

- Android native, Kotlin 2.0.21, Jetpack Compose (Material 3), single activity.
- No WebView, no remote API, no INTERNET permission.
- Local JSON store (`filesDir/tinta.json`) with atomic writes and a `StateFlow`; no Room in
  v1 to keep the build small and backup inspectable.
- Navigation Compose, generated covers, system serif/sans/mono (no bundled fonts).

## Must-have coverage

| original core | native v1 | note |
| --- | --- | --- |
| Books / volumes / chapters | yes | create, rename, move chapter, duplicate, soft delete |
| Always-on autosave | yes | 550 ms debounce plus flush on stop/dispose |
| Version history + restore | yes | auto, 2-minute, manual, pre-restore, 40 per chapter |
| Cross-chapter search | yes | all books, book filter, case, regex |
| Replace all | yes | confirm, snapshots before replace |
| Markdown | partial | safe subset + preview + format bar |
| Export / backup | partial | share text, save Markdown, full JSON backup |
| Themes / typography | yes | four palettes, 3 fonts, size/line height |
| Statistics | yes | goal, streak, 12-week heatmap, recent |
| Trash | yes | restore, permanent delete confirm |
| Premium removal | yes | no account, paywall, ads, trial, or locked feature |
| Cloud sync / TTS / PDF / DOCX / EPUB | no | documented, not faked |
| App lock | no | would be misleading on local JSON |

## Checks actually run

- `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**.
- `./gradlew :app:lintDebug` → **BUILD SUCCESSFUL**, 0 lint errors (warnings only:
  dependency updates, adaptive-icon monochrome, unused color).
- `aapt dump badging` → package `id.tinta.app`, versionName `1.0.0`, minSdk 26, target 35,
  label Tinta.
- `apksigner verify --print-certs` → debug certificate `CN=Android Debug`.
- APK contains `classes.dex`, `AndroidManifest.xml`, `resources.arsc`.
- Not run: emulator/device install, launch, UI flow, rotation, process death, export/share,
  import, or accessibility. `/dev/kvm` exists but the build user has no access, so no
  emulator was started. This is the main risk.

## Clean-room / brand

No original code, assets, fonts, screenshots, name, domain, or palette were used. The native
app keeps the Tinta name, warm-paper/deep-teal palette, generated covers and original
Indonesian sample prose from the web build. Brand and trademark screening still need the
checks listed in `brand.md` before any store release.

## Risks before calling it launchable

1. No runtime smoke test; Compose layouts can still overflow on small screens or with large
   font settings.
2. `TintaStore` writes on a background coroutine, but abrupt process death can lose the last
   <400 ms; the UI labels unsaved state and `onStop` flushes.
3. JSON size and no encryption: fine for a personal notebook, not for sensitive/legal data.
4. No release keystore; the artifact is debug-signed.
5. No undo/redo toolbar; only platform text undo and version history.
6. No volume/chapter drag reorder.

## Recommended next

1. Physical-device smoke matrix (API 26, 33, 35; portrait/landscape; 200% font).
2. Instrumented Compose tests for create → write → reload → history restore → search replace.
3. Room + migration, then JSON import compatibility fixtures.
4. Undo/redo and PDF export.
5. Release signing, privacy statement, and store listing assets (only after brand checks).

## 2026-11-16 — Halaman Fitur Tinta

The user asked for the feature showcase Pure Writer opens from its menu, rebuilt in Tinta. It
sits behind **Setelan → Lihat semua fitur Tinta** (`features` route), the closest thing this
single-activity app has to a main menu.

- Section bands + four-column icon grid: `coreTiles` and `reasons` in `ui/screens/FeaturesScreen.kt`.
- Three highlight rows (`highlights`) carry the detail the original puts under a second band.
- The same catalogue opens as a two-column bottom sheet (`ui/components/FeatureSheet.kt`) from
  the three-dot menus of the book screen, each chapter row and each library book card, so the
  list is one tap away while writing instead of a separate screen. Every row is tappable and
  carries a `FeatureTarget` handled once in `TintaApp`; rows that need a document open the
  book's most recently edited chapter, and fall back to the book screen when it has none.
- Every listed feature is free: the sticky action bar says so and returns to writing. No
  purchase flow, price, tier or licence check exists because Tinta has no billing.
- Deliberately left out, per the user's instruction: read-aloud/TTS, accessibility services
  that need a server, and every AI writer feature. Cloud sync stays out as before.

Checks: `./gradlew :app:assembleDebug :app:lintDebug` run locally on a portable JDK 17 +
Android SDK 35 (see the run notes in the commit), and the GitHub Actions workflow rebuilds the
debug APK and source archive on push.

## 2026-11-16 — Ekspor buku

- `util/Export.kt` builds a whole book on device: Markdown (`bookMarkdown`), plain text
  (`bookPlainText`) and a paginated A4 PDF via `android.graphics.pdf.PdfDocument` (`bookPdf`).
  Volumes and chapter order are preserved; no dependency was added.
- The book screen's three-dot menu gained "Ekspor buku (Markdown)" and "Ekspor buku (PDF)",
  both writing through the system create-document picker (`ActivityResultContracts.CreateDocument`).
- The feature sheet's export rows now run from `TintaApp`, so "Ekspor Markdown", the new
  "Ekspor PDF" and "Bagikan buku" work from any screen that opens the sheet.

## 2026-11-16 — Batch 1 blueprint (editor)

- Undo/redo stack in `EditorScreen` (60 steps, 700 ms grouping) with `AutoMirrored.Filled.Undo`/`Redo`
  in the top bar; the stack is editor-local and never touches the JSON store.
- Typewriter scrolling: the body field now lives in a scrolling column and a `LaunchedEffect`
  centres the caret line when `Settings.typewriter` is on (toggle in the editor menu or Settings).
- Chapter navigation: bottom bar chevrons open the previous/next chapter of the same book, and a
  list icon opens a table-of-contents dialog. Both route through `onOpenChapter` in `TintaApp`,
  which pops back to the book screen so the back stack does not grow per chapter.
- `Settings` gained `chapterGoal`, `typewriter` and `lastBackupAt` (all defaulted, so old JSON loads).
  The editor shows "kata / target" plus a thin progress bar, and a 15-minute sprint timer in the menu.
- Settings gained a "Menulis" card: per-chapter target, typewriter toggle, and a phrase manager
  (tap a phrase to delete it, add via the editor dialog).
- Settings now shows when the last JSON backup happened and warns after 7 days; a successful
  backup or restore stamps `lastBackupAt`.

## 2026-11-16 — Batch 2 blueprint (penerbitan)

- PDF export gained a title page, a copyright line and a table of contents before the chapters.
- `Export.bookEpub` writes a valid EPUB 3 container by hand (stored `mimetype`, container.xml,
  content.opf with manifest/spine, nav.xhtml and one XHTML per chapter) using only `java.util.zip`.
- `Export.bookDocx` writes minimal OOXML (`[Content_Types].xml`, `_rels/.rels`,
  `word/document.xml`) with bold headings, so Word/Google Docs open it without a library.
- The book menu and the feature sheet expose Markdown, PDF, EPUB and DOCX through the system
  create-document picker; nothing leaves the device.

## 2026-11-16 — Cadangan otomatis saat aplikasi ditutup

- `TintaStore.autoBackup()` writes `tinta-yyyyMMdd-HHmm.json` into the app's external files
  folder (`Android/data/id.tinta.app/files/cadangan`) on every `onStop`, keeping the three newest
  copies. No storage permission and no SAF dialog is needed, and Settings mentions it next to the
  manual backup row.

## 2026-11-16 — Sisa blueprint tanpa server

- **Story Bible**: `LoreEntry` cards (karakter/lokasi/istilah) per book, `LoreScreen` from the book
  menu, and a profile card reachable from the editor.
- **Note pinggir**: `MarginNote` quotes a selection without touching the manuscript; menu badge,
  resolve/delete, and a yellow context pill when text is selected.
- **Cepat tata letak**: an `Aa` action opens a bottom sheet with font size, line height, typeface,
  theme, and a live sample.
- **Progressive disclosure**: format bars hide while typing and return after a short pause; selection
  shows the contextual pill (tesaurus, comment, character card).
- **Baca nyaring**: on-device `TextToSpeech` with per-word highlight (no audio leaves the device).
- **Tesaurus offline**: bundled `assets/tesaurus.json`, original pairs, no licence issue.
- **Kunci aplikasi**: `BiometricPrompt` + device PIN gate on launch (`Settings.lock`), enabled from
  Settings after a successful check.
- **Cadangan terenkripsi**: PBKDF2-HMAC-SHA256 (150k rounds) + AES-GCM, written as `.tintaenc`.
- **Folder cadangan**: SAF tree picker stored in `Settings.backupFolder`; every `onStop` writes the
  backup there so Drive/Syncthing syncs it, plus the app folder copies.
- **Naskah per bab**: bodies live in `tinta-naskah.db` (SQLite via `SQLiteOpenHelper`), the JSON keeps
  metadata only, so a keystroke no longer rewrites the whole library.
- **Drag-and-drop**: a hold-and-drag handle in every chapter row calls `reorderChapters`.
- **PDF siap cetak**: 54pt print margins and centred page numbers on every page.
- **Rilis**: `signingConfig` from `keystore.properties` (ignored) and `.github/workflows/release.yml`
  building a signed release APK per tag, uploaded to GitHub Releases.
- **Urutan frasa cepat**: `movePhrase` exposed as ‹ › next to each chip.

## 2026-11-16 — Sampul kustom dan font milik pengguna

- `Book`, `Volume`, and `Chapter` gained `coverImage` + `coverRatio`. `Covers.save()` copies the picked
  picture into `filesDir/sampul`, so no gallery permission is needed afterwards.
- Cover pickers live in the library book menu, the book menu, the section menu, and the chapter menu;
  each opens `CoverFrameDialog` with three frames — **2:3 (recommended)**, 9:16, and the photo's own
  portrait ratio — plus a live preview before saving.
- `BookCover` draws the image with the chosen aspect (gradient stays as fallback); `CoverThumb` shows
  small covers in section headers and chapter rows.
- Custom fonts: `Fonts.install()` copies a TTF/OTF into `filesDir/font` and validates it with
  `Typeface.createFromFile`; Settings → Menulis → "Impor font" activates it, and the editor + the Aa
  sheet use it through `FontFamily(Font(file))`. "Pakai bawaan" reverts to the bundled families.

## 2026-11-17 — Bingkai ulang, pembersihan berkas, sampul di ekspor

- Every cover menu gained a frame-only entry ("Atur bingkai …") that reopens `CoverFrameDialog`
  with the stored image, so 2:3 / 9:16 / portrait can change without picking a file again.
  Cancelling a frame-only dialog keeps the file; cancelling a fresh pick deletes the copy.
- `TintaStore` drops the previous cover file whenever a cover is replaced, and removes every cover
  (book, its sections, its chapters) when a book or section is deleted or a chapter is purged.
  Soft-deleted chapters keep their file so Trash restore still shows the cover.
- `Export.bookPdf` draws the book cover on the title page (max 200×280 pt, centred).
- `Export.bookEpub` embeds `OEBPS/cover.jpg`, adds `cover.xhtml`, the `cover-image` manifest item and
  a leading spine entry, plus the legacy `<meta name="cover">` hint.

## 2026-11-17 — Perbaikan bingkai bab dan gulir yang lebih ringan

- Bug: "Bingkai sampul bab" now resets `pendingIsNew`, and every dismiss path calls
  `TintaStore.discardUnusedCover()`, which refuses to delete any file still referenced by a book,
  section, or chapter. Frame-only paths therefore never touch the stored image.
- `Covers` keeps an `LruCache` of decoded bitmaps; `rememberCover()` decodes on `Dispatchers.IO`
  and reuses the cache, so list scrolling no longer decodes images on the main thread.
- `BookCover`/`CoverThumb` request 480/240 px copies instead of 720 px, lowering memory churn.
- Word totals are no longer recomputed per frame: the library derives a per-book stats map once,
  the book screen memoises its chapter list and word total, and chapter rows remember their count.
- The editor body no longer sits inside `verticalScroll`; the constrained `BasicTextField` scrolls
  its own layout, so dragging a long chapter only redraws visible lines.
- Lazy lists declare `contentType` so Compose can reuse row layouts while scrolling.
