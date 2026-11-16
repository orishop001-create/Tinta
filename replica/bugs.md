# Bugs — Tinta

No open S1 or S2 bugs at handoff. Everything below was reproduced, then fixed or recorded
as a known limitation.

## Fixed

### B01 — S1 — stale tab could overwrite newer work
- **Steps:** open the app in tab A; in tab B type and save a new snapshot; return to tab A
  and reload.
- **Expected:** tab A loads the newest library.
- **Actual (before):** tab A persisted its stale in-memory state on reload and removed the
  newer snapshots. Reproduced as "0 versi" while storage held two snapshots.
- **Fix:** baseline-aware persistence. A tab with no local edits adopts newer stored data
  instead of writing; the active editor never accepts a background overwrite and warns the
  user. Storage events refresh non-editor tabs. Commit: `fix: protect local data across tabs`.
- **Verified:** type + reload persistence; history 2 versions; restore end-to-end.
- **Residual:** two editors typing simultaneously is still last-writer-wins.

### B02 — S3 — history link dead-ends for trashed chapters
- **Steps:** Sampah → a trashed chapter → Riwayat → "Kembali menulis".
- **Expected:** either restore first or no history action in trash.
- **Actual:** editor correctly rejects a deleted doc, leaving a dead end.
- **Fix:** removed the Riwayat action from trashed rows. Commit: `fix: remove dead-end history link from trash`.
- **Verified:** trash screen renders without the action.

### B03 — S4 — "Bersihkan" search control did nothing
- **Steps:** type a query → click "Bersihkan".
- **Expected:** clear the query.
- **Actual:** only reset an unrelated modal state; the field already had an X clear button.
- **Fix:** removed the redundant control. Commit: `fix: drop no-op search clear button`.
- **Verified:** search field clear icon works; search flow tested.

## Known limitations (not counted as bugs)

- **L1 (S3):** two editor tabs editing the same chapter at once can overwrite each other.
  No merge or CRDT. The UI warns on a background write.
- **L2 (S3):** localStorage ~5 MB; an extremely large library can hit the quota. A toast
  asks for a backup; no automatic IndexedDB migration.
- **L3 (S3):** PNG export refuses documents taller than 15,000 CSS px. PDF/TXT remain.
- **L4 (S2 for some users):** no cloud sync/account. Backups are manual JSON files. This is a
  deliberate local-first consequence of removing the premium cloud offering.
- **L5 (S3):** no DOCX/EPUB/TTS/LaTeX. HTML, Markdown, TXT, PNG and print-to-PDF exist.
- **L6 (S3):** switching tabs at the exact moment of a write can still race; no server clock
  to arbitrate.
- **L7 (S4):** canvas PNG image is single-page; no long-image splitting.
- **L8 (S4):** import replaces all data; no merge import.

## To check next

- Actual file download + print dialog on Chromium, Firefox and Safari.
- JSON import of a hand-edited/partial backup.
- Drag-and-drop reorder with touch and keyboard-only.
- 5,000-word and 100,000-character chapters (performance and quota).
- Screen reader pass over the editor paper and diff view (aria roles for the diff table are
  minimal).
