# Design system — Tinta

Source of truth: `src/styles/tokens.css` (runtime) and `replica/design/tokens.json`
(spec/contrast pairs). Raw hex values live only in the token layer and in the generated
cover palette; components consume `var(--role)`.

## Roles

| role | paper | sepia | night | graphite |
| --- | --- | --- | --- | --- |
| bg | `#f4efe4` | `#ebdfc9` | `#121615` | `#181b1e` |
| surface | `#fbf7ee` | `#f8eedb` | `#191f1d` | `#202428` |
| text | `#241f18` | `#2a2117` | `#ede7db` | `#e9e7e2` |
| text-muted | `#635949` | `#665741` | `#b6ac9b` | `#b1aaa0` |
| accent | `#1f6b5a` | `#1f6b5a` | `#83cdb1` | `#d7b475` |
| on-accent | `#fbf9f3` | `#fbf9f3` | `#0b1a15` | `#221b0e` |
| danger | `#9e3527` | `#993423` | `#f09b8b` | `#f19a8a` |
| editor-bg | `#fcf8f0` | `#faf1e1` | `#171d1b` | `#1c2023` |

The original's blue brand family is deliberately not reused. The new palette is warm paper
plus deep teal, with covers in clay, ink, moss, plum and wheat. `python3 contrast.py
replica/design/tokens.json` reports **44 pairs, 0 failing AA**, including input borders at
3:1 and faint metadata at 4.5:1.

## Type

- UI: Instrument Sans Variable; body/editor: Newsreader Variable; code: system mono.
- Scale: 10 / 11.5 / 13 / 15 / 19 / 21 / 30 / 46 px. Editor size is user-adjustable 14–26.
- Line height: UI 1.5; editor default 1.75, adjustable 1.3–2.4.
- Display and page titles use Newsreader 600 with -0.03em tracking; body uses 400.
- Editor width 720 px default (520–960); screen max width 1240 px; sidebar 260 px.

## Space, radius, elevation

- 4 px base: 4, 8, 12, 16, 24, 32, 48, 64.
- Radius: 6 (input/menu item), 8–10 (button/input), 16 (cards), 20 (modal/paper), pill.
- Shadows: `sm` for pills, `md` for hover cards, `lg` for dialogs; dark themes use deeper
  black shadows. Paper also carries a 1 px inner highlight.
- Motion: 120–200 ms, cubic-bezier(.2,.8,.2,1); reduced-motion respected.

## Component specs

**Button**
- variants: primary, secondary, ghost, danger, danger-ghost; sizes sm/md.
- states: hover, active (1 px down), focus-visible ring, disabled 45% + no pointer, loading
  (text keeps its space; export image disables the row).
- uses real `<button>`/`<a>`; 38 px min height, 44 px touch target via padding on mobile.

**Icon button** — 34 px, transparent, 9 px radius; active state uses accent-soft; always has
`aria-label`; used in editor header, row menus, and steppers.

**Menu (details/summary)** — trigger is a real summary with label; closes on outside click,
Escape, or item action; role=menu/menuitem; disabled items remain visible but inactive.

**Field** — label above input, 40 px control, 1 px input border at 3:1, focus ring 3 px
accent-soft; errors use danger text below. Inputs are labelled, `maxLength` enforced.

**Modal / confirm** — backdrop click closes, Escape closes, focus moves to the panel and is
trapped on Tab, body scroll locked; title and optional description linked via
`aria-labelledby`/`aria-describedby`; confirm dialogs state the irreversible consequence.

**Book cover** — generated art, no original assets: gradient using cover palette, a spine
line, grain, generated wordmark text rendered live, droplet stamp. Variants: sm on the
book hero, md on shelf cards.

**Volume accordion / chapter row** — volume header is draggable but also a button with
`aria-expanded`; chapter row has a real open button plus a menu. Dragging shows opacity and
an inset top line; menu offers naik/turun and move for keyboard users. Trash is soft.

**Editor paper** — 720 px column, 58 px desktop padding, serif prose at 19/1.75, optional
ruling background tied to `1lh`, title input, full-height growing textarea. Focus mode hides
the format bar, quick bar, word pill, crumbs and export; Esc exits.

**Format bar** — always available in write mode: undo/redo with disabled states, then (when
Markdown is on) bold, italic, heading, lists, quote, inline code, link, rule, date. Buttons
call selection transforms; all have labels/titles. With Markdown off, a single explanatory
row offers activation instead of implying unavailable formatting. Undo groups a typing
burst by a 700 ms pause and keeps 120 steps.

**Word pill** — fixed above the quick bar, tabular numbers, updates live; not an input.

**Quick-insert bar** — horizontal chip scroller, phrase add form, daily goal progress.
Persistent bottom reach on mobile.

**Heatmap** — 7×17 grid, 13 px cells, level 0–4 from target ratio, `<span role="img">` with
date/word aria-label, title tooltip, today outlined; legend uses the same tokens.

**Search result** — whole row is a link; matched terms wrapped in `<mark>`; shows book, count,
snippet, updated time; regex errors are inline danger text.

**Switch / range / theme swatch** — `role="switch"` + `aria-checked`; range has an associated
label and numeric output; theme options are pressed buttons with visible swatch previews.
