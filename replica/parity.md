# Parity — Tinta vs the original writing app

Scope: the web slice mapped in `recon.md`. Screenshot/layout diff was **not** run: the
original is a native Android app and no public screenshot was captured; copying its exact
layout would also fight the deliberate rebrand. This report scores function, not pixels.

## Scores

- **Feature parity: 89.7 / 100** (`python3 parity.py replica/features.csv`)
- **Must-haves: 20 / 20 (100%)**
- Should-haves: 13 / 13 done
- Could-haves: 2 partial, 8 missing, 4 skipped with reasons
- Areas: Organisasi 100, Keamanan 100, Pencarian 100, Statistik 100, Pustaka 100,
  Tampilan 92.9, Editor 90.4, Cadangan 75, Ekspor 69.2, Aksesibilitas 0 (TTS only),
  Sinkronisasi 0 (cloud only).

## Missing, in build order

1. Bacakan naskah (TTS) — could. Web Speech API is available; not implemented.
2. Automatic local backup files — could. Autosave exists; file backup is manual.
3. Dual editors side by side — could. Focused single editor only.
4. LaTeX math — could. Safe Markdown subset only.
5. DOCX export — could. HTML/PDF substitute.
6. EPUB export — could. Library Markdown + print PDF substitute.
7. Export selected text only — could.
8. Split long image into a set — could.
9. Cloud sync / Dropbox / OneDrive / WebDAV — could, but requires a backend and accounts;
   deliberately replaced by free local JSON backup.
10. Image insertion — partial: Markdown image URLs work; no upload picker.
11. Custom font upload — partial: three open font stacks, no file upload.

## Behaviour differences

| flow | original | clone | decision |
| --- | --- | --- | --- |
| Core write | 2 taps into a chapter, autosave always | 2 clicks, autosave always | keep |
| History | auto history + diff + restore | same, 40 per doc, safety snapshot before restore | keep |
| Premium features | some export/cloud features gated | every local feature free, no paywall/account | **requested change** |
| Cloud backup | Dropbox/OneDrive/WebDAV | manual full JSON export/import, all free | keep for local-first |
| Export set | TXT/MD/PDF/image/DOCX/EPUB | TXT/MD/HTML/PNG/print-PDF + whole-library MD | partial, honest |
| Search | across chapters, regex, case | across books, regex, case, scope, replace | parity |
| Statistics | daily statistics/heatmap | daily heatmap, streak, goal, recent | parity |
| Platform | native Android/iOS/desktop | responsive web, offline after load | scope |
| Multi-device | cloud account sync | none; local per browser | scope |

Clicks to finish the core flow: original ~2, clone 2 (open book, open chapter; typing
requires no save action). No regression.

## Verdict

**Shippable for the web scope.** All must-haves are done, feature parity is above 80, and
there are no open S1/S2 bugs. It is not "better than the original": cloud sync, native
integrations, EPUB/DOCX and TTS are absent, and no user-research fixes have been applied.
It is a credible, fully free local-first writing app, not a claim of a perfect clone.

## Top five next

1. IndexedDB storage (capacity and speed) with migration from localStorage.
2. EPUB + DOCX export using an MIT archive library.
3. TTS proofreading with Web Speech API and sentence highlighting.
4. Optional encrypted cloud sync (or a documented Dropbox adapter) — only if users ask.
5. Automatic local snapshot backups + restore-from-file diff.
