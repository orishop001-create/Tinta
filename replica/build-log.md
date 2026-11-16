# Build log — Tinta

Date: 2026-11-15. Build target: static web app (single-file artifact + source).

## Screens

| screen | status | notes |
| --- | --- | --- |
| S01 Pustaka | done | resume strip, generated covers, tags, search, create/edit/delete with cover palette |
| S02 Buku | done | volumes, chapter rows, drag reorder, keyboard naik/turun, move dialog, soft delete, empty states |
| S03 Editor | done | autosave, Markdown toggle + preview, format bar, quick phrases, typewriter, rulings, focus, daily goal, export, duplicate/move/trash, history entry |
| S04 Riwayat | done | auto/manual snapshots, line diff, preview, restore with safety snapshot, empty state |
| S05 Cari & ganti | done | cross-book search, case/regex, scope, highlighted snippets, bulk replace with confirm and snapshots |
| S06 Statistik | done | today/streak/library/active-days cards, goal ring, 17-week heatmap, 7-day chart, recent list |
| S07 Sampah | done | restore, permanent delete confirm, empty-trash confirm, empty state |
| S08 Setelan | done | free-feature card, four themes, three font stacks, sliders, switches, goal, phrase editor, JSON backup/import, Markdown library export, reset, about/shortcuts |
| S09 Ekspor | done | TXT, MD, HTML, PNG, print/PDF; busy state; long-document guard |

## Harder than expected

- **Safe local persistence.** The first implementation could let a stale tab overwrite
  newer localStorage on reload. Added baseline comparison: a tab with no local edits adopts
  newer stored data instead of clobbering it; the active editor never auto-accepts a
  background write. Simultaneous editor tabs remain last-writer-wins (documented).
- **Diffing.** LCS with a 1.6M-cell guard and a prefix/suffix trim, so a 500-line chapter
  stays responsive. The diff view caps at 500 visible rows.
- **PNG export.** Canvas text wrapping, font readiness and a 15,000 px height guard. Long
  chapters fail with an explanation rather than an empty canvas.
- **Editor ergonomics.** Paired symbols, list continuation, list exit on empty Enter,
  tab indent, bold/italic shortcuts, caret centring in typewriter mode, and a textarea that
  grows with the page. All deliberately lightweight, no contenteditable.
- **Mobile editor.** Persistent quick bar and word pill must not cover the last paragraph;
  the page reserves 170 px at the bottom.

## Fixes during the build

- Removed a dead "Bersihkan" search button that didn't clear the query (the field already
  has a clear control).
- Removed the "Riwayat" link from trashed rows: history for a deleted doc led back to an
  editor that correctly refuses to open it.
- Book hero no longer says "0 bagian" for books without volumes.
- Storage-event sync added for already-open tabs; guards against stale-tab data loss.
- Editor state resets when switching documents (preview/focus/quick-add).
- Drag-over events stop propagation so section drops don't double-move a chapter.

## Verification performed

- `npm run build` — TypeScript + Vite build passes; single-file output ~602 KB (gzip ~268 KB).
- `python3 replica-design/contrast.py replica/design/tokens.json` — 44 pairs, 0 AA failures.
- Browser smoke (1440/1024 and 400 px): Pustaka, Buku, Editor write/preview, Riwayat,
  Cari, Statistik, Setelan (paper + night), Sampah empty state. No console errors observed.
- Typed into a chapter, waited, reloaded: text and theme persisted, status "Tersimpan".
- History: auto snapshots, line diff +2/−0, restore confirmed; body reverted to the older
  version and navigation returned to the editor.
- Search: "hujan" → 6 matches in 4 chapters; invalid regex shows inline error.
- Feature matrix: 20/20 must-haves, parity 89.7/100.
- Undo/redo burst grouping: Ctrl+Z removes a full typed burst, Ctrl+Shift+Z restores it.
- Not run: file downloads/print, JSON import, bulk replace execution, drag-and-drop,
  create/delete flows, screen-reader pass, axe, original-app screenshots comparison.

## Feature matrix updates

`replica/features.csv` is current. All must-haves are `yes`; should-haves are `yes` except
none; two could-haves are `partial` (image insertion via Markdown URL, custom font upload).
Native/cloud/app-lock rows are `skip` with reasons. Parity is 89.7.

## Next recommended mode

`replica-test` for the unexecuted flows (downloads, import, bulk replace, drag) and
`replica-diff` if original screenshots are legally captured later. Before any launch:
run the brand trademark/domain checks, add privacy policy, and optionally move storage to
IndexedDB for capacity.
