# Recon map — Pure Writer → Tinta

Clean-room recon for a **web rebuild of Pure Writer's core writing loop**, with every
premium gate removed. We studied public product pages, the public user manual and
independent reviews. No original source, assets, copy, fonts, icons or private endpoints
were used, and no account was accessed.

- Original: Pure Writer — Writing & Notes (Android/iOS/desktop), Drakeet.
- Rebuild: **Tinta**, a local-first web app. Rebranded name, palette, icons, sample prose and UI copy.
- Scope: the writing loop plus the supporting tools users open every day. Not native OS
  integrations, cloud accounts, or a store app.
- Requirement that overrides parity: **no paywall, trial, account, ads or locked feature.**

## Sources

| source | URL | what it gave us |
| --- | --- | --- |
| Google Play listing | https://play.google.com/store/apps/details?id=com.drakeet.purewriter | feature inventory, positioning, platform summary |
| Official product page | https://writer.drakeet.com/ | books/volumes, autosave, history, export list, Markdown, quick input |
| Public user manual | https://writer.drakeet.com/wiki_en | Markdown/LaTeX behaviour, history and typewriter mental model |
| SSPAI review | https://sspai.com/post/51173 | real usage notes: shortcut bar, formatting, novel structure, never-lose-text |
| NoteApps review | https://www.noteapps.ca/purewriter | independent feature tour and limitations |
| imgradeone notes | https://imgradeone.com/thoughts/purewriter | export/Pro gating observed by a user, backup behaviour |

No screenshots from the original are shipped. `replica/screens/` is intentionally empty;
the clone screens in `replica/clone-screens/` are captures of our own build.

## Core loop

Open a book → open a chapter → write without a save button → trust that the text,
history, word count and backup are already handled.

## Screen inventory

| ID | screen | route | purpose | key components | states considered |
| --- | --- | --- | --- | --- | --- |
| S01 | Pustaka | `#/pustaka` | choose a book, resume writing, find/tag books | book cards, generated covers, search, tag chips, resume strip, create/edit/delete dialogs | empty shelf, no filter results, long titles, six tags, mobile |
| S02 | Buku | `#/buku/:id` | organise a book into volumes and chapters | hero, volume accordions, chapter rows, drag handles, menus, move dialog | empty book, empty volume, many chapters, dragged row, missing book |
| S03 | Editor | `#/tulis/:docId` | distraction-free writing | header, Markdown format bar, paper, title input, textarea, preview, word pill, quick-insert bar, export dialog, focus/typewriter | empty body, Markdown off, preview, focus, saved/saving, very long text, deleted/missing doc, mobile |
| S04 | Riwayat | `#/riwayat/:docId` | inspect and restore past versions | version list, preview/diff tabs, line diff, restore confirmation | no versions, identical version, large diff, restore, missing doc |
| S05 | Cari & ganti | `#/cari` | find across all books and replace in bulk | query, case/regex toggles, book scope, replace field, result snippets, replace confirmation | no query, no results, invalid regex, many matches, zero-width pattern |
| S06 | Statistik | `#/statistik` | show writing rhythm | four stat cards, goal ring, 17-week heatmap, 7-day chart, recent list | no activity, goal reached, best day, mobile |
| S07 | Sampah | `#/sampah` | recover or permanently remove deleted chapters | trash list, restore, purge, empty-trash confirmation | empty trash, many items, purge confirmation |
| S08 | Setelan | `#/pengaturan` | personalise the writing environment and data | free-feature card, four theme swatches, font choices, sliders, switches, goal, phrase editor, backup/import/reset | changed theme, long phrases, invalid import, reset confirmation |
| S09 | Ekspor | modal over S03 | take writing out, at no cost | five export options, busy state, note with counts | TXT/MD/HTML, image generation, print/PDF, long document error |

## User flows

**F01 Write and never lose it** (core)
`S01 → S02 → S03 → type` — 2 clicks before typing.
Edge cases: refresh mid-sentence, tab hidden, storage full, deleted while open, no
Markdown, preview during writing. Happy path has **zero save clicks**.

**F02 Build a book**
`S01 create book → S02 add volume → add chapter → editor` — 3–4 clicks.
Edge: blank title, duplicate names, delete volume with chapters, move chapter across books.

**F03 Recover an old version**
`S03 menu → S04 pick version → diff → restore → confirm` — 4–5 clicks.
Edge: no snapshots, identical snapshot, restore undo (safety snapshot), 40-version cap.

**F04 Find and replace across the library**
`S05 type → review matches → optional replace → confirm` — 2–4 clicks.
Edge: regex off/on, case sensitivity, book scope, zero results, empty replacement.

**F05 Export**
`S03 Ekspor → choose TXT/MD/HTML/image/PDF` — 2–3 clicks.
Edge: very long document for PNG, popup blocked for print, image fonts not loaded.

**F06 Back up and restore**
`S08 Data → export JSON / import JSON → confirm overwrite` — 2–3 clicks.
Edge: invalid JSON, missing arrays, imported theme, import over current data.

**F07 Delete and recover a chapter**
`S02 menu → pindahkan ke sampah → S07 pulihkan` — 2–4 clicks.
Edge: permanently delete, empty trash, restore to the original book.

**F08 Personalise the page**
`S08 theme/font/sliders/goal → S03` — no save click; persists across reloads.
Edge: four themes, three font stacks, contrast, long phrases, target 50–10,000.

## Components

Book card and generated cover; page header; tag filter; resume card; volume accordion;
chapter row with drag state; dropdown menu; modal and confirm dialog; toast; editor paper;
Markdown format bar; quick-insert chips; word pill; goal ring; heatmap; week bars; version
row; diff row; search result; theme swatch; range slider; switch; phrase chip.

## Inferred data model

```
Book      id, title, author, color, tags[], created_at, updated_at           high
Volume    id, book_id, title, order, created_at                              high
Doc       id, book_id, volume_id?, title, body, markdown, order,            high
          created_at, updated_at, deleted_at?
Snapshot  id, doc_id, title, body, markdown, reason, created_at              high
Settings  theme, font, size, line_height, paragraph_spacing, width,          high
          typewriter, rulings, daily_goal, phrases[], last_doc_id
DailyStat day (local date), words_added                                     medium
```

Evidence: book/volume/chapter hierarchy from the public product page; autosave/history from
the listing and manual; word counts and writing stats from the listing and reviews; quick
input and Markdown from reviews/manual. `DailyStat` is our mechanism for a visible writing
heatmap; the original tracks daily statistics, but its exact storage shape is not public, so
confidence is medium. Attachments, audio and account entities are out of scope.

## Deliberately not cloned / skipped

| item | why |
| --- | --- |
| Original logo, assets, fonts, UI copy, sample texts | owned by the original; we generated our own |
| Cloud backup via Dropbox/OneDrive/WebDAV and multi-device sync | requires accounts/backends; replaced by free local JSON backup and library Markdown export |
| Android/iOS widgets, share targets, IME-adjacent system integration | native platform features, not a web capability |
| App lock / password protection | localStorage cannot provide real security; would be misleading |
| Premium entitlement, purchase flow, account system | intentionally removed: all features are free |
| Direct EPUB/DOCX generation and read-aloud TTS | not built; printable PDF, HTML, Markdown and TXT cover the export need; TTS could use the Web Speech API later |
| LaTeX rendering | Markdown preview is intentionally small and safe |

## Size

**M** for the web slice: 9 screens, 8 flows, 6 local entities, no backend. The hardest
parts are safe persistence across reloads/tabs, line-level history diffs, and keeping the
editor chrome out of the way. A native replica would be **XL** and is out of scope.

Next: `replica/architecture.md`.
