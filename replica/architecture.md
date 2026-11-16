# Architecture — Tinta

## Stack

| layer | choice | why |
| --- | --- | --- |
| web app | Vite 8 + React 19 + TypeScript 7 | fastest path to a focused SPA with no server dependency |
| styling | hand-written CSS with token variables in `src/styles/tokens.css` | themes and editor typography are the product; no utility-class indirection |
| editor state | one React context store in `src/lib/store.tsx` | small app, pure actions, no need for a state library |
| persistence | `localStorage` key `tinta.v1`, debounced 350 ms + flush on hide/close | works offline, no account, no backend, survives reload |
| history | snapshots in the same store, max 40 per doc | enough recovery without a database |
| fonts | Newsreader Variable + Instrument Sans Variable via Fontsource (OFL) | open, self-hosted, inline in the single-file build |
| icons | Lucide (MIT) | open icon set; no original assets |
| routing | hash router in `src/lib/router.ts` | static single-file deployment, deep links, browser back |
| export | Blob downloads, print view, Canvas 2D | no paid API and no extra dependencies |
| build output | one self-contained `dist/index.html` | an artifact that can be opened offline or served from any static host |

No backend, database, auth, payments, analytics, cookies or external requests. That is a
deliberate consequence of removing premium gating: there is no account to manage and
nothing to sell. A small [reference SQL schema](schema.sql) exists for a future optional
sync backend; the app does not use it today.

## Routes

| route | screen | reads | writes |
| --- | --- | --- | --- |
| `#/pustaka` | S01 Pustaka | books, live docs, settings.lastDocId | create/update/delete book |
| `#/buku/:id` | S02 Buku | book, volumes, docs | volumes, docs, reorder, move, trash |
| `#/tulis/:docId` | S03 Editor | doc, book, volume, settings, today's stats | doc content, markdown flag, snapshots, settings |
| `#/riwayat/:docId` | S04 Riwayat | doc, snapshots | snapshot, restore |
| `#/cari` | S05 Cari & ganti | all live docs, books | bulk replace + snapshots |
| `#/statistik` | S06 Statistik | live docs, stats, settings.goal | none |
| `#/sampah` | S07 Sampah | trashed docs, books | restore, purge, empty trash |
| `#/pengaturan` | S08 Setelan | all data + settings | settings, import, export, reset |
| modal | S09 Ekspor | current doc | file downloads / print window |

## Local schema

Record model (TypeScript types in `src/lib/types.ts`):

```
Book 1 ── * Volume 1 ── * Doc 1 ── * Snapshot
Book 1 ──────────────── * Doc          (volume is optional)
Settings (singleton)   DailyStat: { "YYYY-MM-DD": wordsAdded }
```

- IDs are UUID strings from `crypto.randomUUID()`.
- Times are epoch milliseconds (local-first, no timezone conversion needed for ordering;
  day buckets use the user's local date).
- Docs are soft-deleted with `deletedAt`; purge removes the doc and its snapshots.
- `markdown` is per doc, so one book can mix plain text and Markdown chapters.
- Snapshots store a copy of title/body/markdown plus a reason (`auto`, `manual`,
  `before-restore`).
- Deleting a volume nulls its docs' `volumeId`; deleting a book cascades docs, volumes and
  snapshots.
- Stats track **positive word deltas** per local day. Deleting or restoring text does not
  subtract from the day, which matches the "words you added" mental model.
- `lastDocId` powers the resume card.

`replica/schema.sql` mirrors this as SQLite/D1 DDL for a future sync service, with owner
columns, foreign keys, checks, and indexes. Access rule for a future backend: every query
filtered by the authenticated `user_id`; no client-supplied owner IDs. Not active now.

## Store API

All mutations are synchronous pure reducers over the whole app state:
`createBook`, `updateBook`, `deleteBook`, `createVolume`, `renameVolume`, `deleteVolume`,
`createDoc`, `updateDoc`, `deleteDoc`, `restoreDoc`, `purgeDoc`, `emptyTrash`,
`duplicateDoc`, `moveDoc`, `placeDocs`, `reorderVolumes`, `snapshot`, `restoreSnapshot`,
`updateSettings`, `importData`, `resetData`.

Persistence policy:
1. Every state change schedules a 350 ms write.
2. `pagehide`, `beforeunload` and `visibilitychange→hidden` flush immediately.
3. If this tab has **no local edits** since its baseline and storage contains different,
   newer data, it adopts the stored payload instead of overwriting it.
4. A tab that is actively editing (`#/tulis/...`) never accepts a background overwrite; it
   shows a toast. Simultaneous typing in two editor tabs remains last-writer-wins and is
   documented as a known limitation.
5. A quota failure surfaces a toast telling the user to export a backup.

## API / routes

There is no server API. The equivalent "routes" are the store actions above plus the
browser-only export handlers in `src/lib/export.ts`:

| handler | does | who can call |
| --- | --- | --- |
| `exportDoc` | TXT/MD/HTML Blob download | local user gesture |
| `openPrintView` | opens a self-contained print page | local user gesture |
| `exportDocImage` | Canvas PNG (max 15,000 CSS px tall) | local user gesture |
| `exportLibrary` | one Markdown file for all books | local user gesture |
| backup import | JSON validation → replace state after confirm | local user |

No original endpoints, no scraping, no network calls.

## The parts that bite

- **localStorage size** (~5 MB). Long novels can fill it. Surfaced, not hidden; TXT/MD
  export is always available. A real product would move to IndexedDB.
- **Two tabs / last write wins.** Guarded with baseline comparison and a warning, not a CRDT.
- **Clearing site data** destroys the library. The UI repeats this in Settings and offers a
  full JSON backup.
- **History memory** is capped at 40 snapshots per doc. Large bodies × 40 can be heavy.
- **PNG limits.** Very long chapters hit the 15,000 px guard and get a clear error instead of
  a broken image.
- **Popup blockers** on print. Falls back to downloading the print HTML.
- **Regex safety.** User-supplied patterns are scanned with a match cap (5,000 per field) and
  zero-width patterns are rejected for replacement. This is a local single-user tool, so
  ReDoS is a self-inflicted risk, not a server risk.
- **Time zones.** Day boundaries use the device's local date; no UTC conversion, which is
  what a single-user writing app wants.
- **Accessibility.** Focus states, real buttons, labelled inputs, dialog focus trap, keyboard
  alternatives for drag-and-drop, AA contrast across four themes.
- **Fonts.** Self-hosted open fonts; no licensed original typeface.

## Build order

1. Vertical slice: library → book → editor → autosaved chapter → reload. ✅
2. Must-haves: three-level organisation, history/restore, trash, Markdown, search/replace,
   stats, themes, typography, focus, export/backup. ✅
3. Should-haves: image export, print PDF, library export, tags, typewriter, rulings, typeset,
   regex search, daily goal, drag reorder. ✅
4. Polish: dark themes, contrast pass, mobile shell, cross-tab guard. ✅
5. Optional later: IndexedDB, EPUB/DOCX, TTS, optional encrypted sync. Not promised.

Next: `replica/design/tokens.json` and `/replica-design`.
