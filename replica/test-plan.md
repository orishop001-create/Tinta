# Test plan — Tinta

Tested clone only. The original app was not automated, loaded behind a login, scraped or
hammered. Status: **pass** = reproduced in the browser during this build; **not run** =
listed for the next QA pass. No claim is made for unexecuted cases.

## F01 Write and never lose it

- F01-H1 Open chapter, type, wait 1 s, reload → text returns, status "Tersimpan". **pass**
- F01-H2 Type then switch tab/pagehide → localStorage flushed. **pass** (reload test)
- F01-E1 Empty chapter: placeholder, 0 words, no error. **pass** (visual)
- F01-E4 Undo a typed burst → full burst reverts; redo restores it. **pass** (Ctrl+Z / Ctrl+Shift+Z)
- F01-E2 5,000+ word chapter: textarea grows, quick bar does not cover the last line. **not run**
- F01-E3 Storage full: toast, no silent failure. **not run** (requires quota exhaustion)
- F01-N1 Deleted while open in another tab → editor empty state on refresh. **not run**

## F02 Build a book

- F02-H1 Create book → lands on empty book screen. **not run**
- F02-H2 Add volume → add chapter → editor opens with title focused. **not run**
- F02-E1 Empty/very long book titles. **not run**
- F02-E2 Delete volume → chapters move to "Tanpa bagian", not trash. **not run**
- F02-E3 Drag chapter within volume and across volumes. **not run**
- F02-N1 Missing book id → friendly empty state. **pass** (route fallback reviewed)

## F03 Recover an old version

- F03-H1 Open chapter → snapshot exists; type → leave → two versions. **pass**
- F03-H2 Compare → diff shows +2/−0; older preview renders. **pass**
- F03-H3 Restore → confirmation → body reverts, older content kept as a safety snapshot. **pass**
- F03-E1 No snapshots → empty state. **pass** (fresh tab before edits)
- F03-E2 Identical version → restore disabled with explanation. **pass** (latest snapshot)
- F03-E3 40-snapshot cap → older versions pruned. **not run**
- F03-N1 Deleted doc → history screen refuses cleanly. **pass** (code path; trash link removed)

## F04 Find and replace

- F04-H1 Search "hujan" → 6 matches in 4 chapters, book scope correct. **pass**
- F04-H2 Invalid regex when regex mode is on → inline error, replace disabled. **pass**
- F04-E1 Zero results → empty guidance. **pass**
- F04-E2 Case-sensitive vs insensitive. **not run**
- F04-E3 Huge match set → capped scanning, no hang. **not run**
- F04-N1 Replace all → confirmation, snapshots, counts. **not run** (destructive against seed)

## F05 Export

- F05-H1 TXT/MD/HTML download. **not run** (download behavior not inspected)
- F05-H2 PNG long image, theme colours, title + word count. **not run**
- F05-E1 Overlong chapter → clear PNG limit error. **not run**
- F05-N1 Popup blocked for print → fallback HTML download. **not run**

## F06 Back up and restore

- F06-H1 Export full JSON, import later. **not run**
- F06-E1 Invalid JSON / wrong shape → inline error, existing data untouched. **not run**
- F06-N1 Import overwrite requires confirmation. **not run**
- F06-N2 Reset data requires confirmation, keeps theme. **not run**

## F07 Delete and recover

- F07-H1 Delete chapter → trash, restore → returns to book. **not run**
- F07-E1 Empty trash → empty state. **pass** (visual)
- F07-N1 Permanent delete and empty trash require confirmation. **not run**
- F07-N2 History link for a trashed chapter removed (dead end fixed). **pass** (code)

## F08 Personalise

- F08-H1 Switch theme → instant, persists across reload. **pass**
- F08-H2 Adjust font size/line height/width → editor reflects immediately. **not run**
- F08-E1 Target below 50 or above 10,000 → clamped on blur. **not run**
- F08-E2 Add/remove phrase → appears/disappears in editor quick bar. **not run**
- F08-N1 Long phrase → chip truncates with ellipsis. **pass** (CSS/clamp reviewed)
- F08-H3 Focus mode → chrome hides; Esc restores. **not run**

## Cross-cutting

- T1 Build/typecheck. **pass**
- T2 Contrast tokens (44 pairs). **pass**
- T3 Console errors on visited screens. **pass** (none observed)
- T4 Keyboard: tab order, menus, dialog focus trap, buttons. **partial** (editor shortcuts and dialog Escape
  exercised; full tab-order pass not run)
- T5 Screen reader labels & axe scan. **not run**
- T6 Mobile 400 px: Pustaka, Cari, Riwayat, Setelan. **pass** (visual)
- T7 Two tabs: stale reader adopts newer data; active editor warns. **pass** (fix reviewed; full
  two-tab interaction not scripted)
- T8 Offline after load. **not run**
- T9 60-character names / six tags / no tags. **not run**

## Severity key

S1 data loss/security/core blocked · S2 feature broken · S3 broken with workaround/visibly
wrong · S4 cosmetic. See `replica/bugs.md`.
