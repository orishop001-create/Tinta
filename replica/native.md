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
