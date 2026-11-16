# Brand — Tinta

The original name, wordmark, palette, assets and voice are not shipped. This is a working
rebrand for the clean-room web rebuild.

## Name

**Tinta** — Indonesian for ink. Short, pronounceable, directly tied to writing, and not a
pun on the original name.

Candidates considered: **Tinta**, Larik, Aksara, Kertas, Jeda, Rangkai, Pena, Lembar.
Cut to five: Tinta, Larik, Aksara, Kertas, Jeda. Chosen for meaning, brevity and a mark that
can be drawn as a single ink drop.

### Checks — to run honestly

These are screening checks, not legal clearance, and **none were executed in this build**.

| check | where | status |
| --- | --- | --- |
| US trademark classes 9/42 | tmsearch.uspto.gov | to run |
| EU trademark | euipo.europa.eu / TMview | to run |
| Canada | ised-isde.canada.ca | to run |
| global | WIPO Global Brand Database | to run |
| domain | registrar / `whois tinta.app` etc. | to run |
| App Store / Play exact name | both stores | to run |
| handles @tinta | X, Instagram, TikTok, GitHub | to run |
| web "Tinta + writing app" | search | to run |
| trademark lawyer before spending money | — | recommended |

Do not launch the name until those rows are filled in. "Tinta" is a common word and many
writing-adjacent products may use it; the screening is mandatory.

## Palette

Different colour family from the original's blue: warm paper and deep **teal**.

- Paper `#F4EFE4`, surface `#FBF7EE`, ink `#241F18`, muted `#635949`.
- Accent `#1F6B5A` (dark theme `#83CDB1`; graphite `#D7B475`).
- Covers: clay `#B96A45`, ink-blue cover `#2C5D6B`, moss `#4F6B45`, plum `#79506A`,
  wheat `#C79A47` — used only for generated book covers, not as the brand accent.
- All text/background pairs pass WCAG AA; 44 pairs, 0 failures.

## Logo brief

- Idea: a drop of ink that doubles as a nib; one shape, one small breathing stroke.
- Mark type: app-icon symbol (liquid drop in a rounded square) plus the lowercase wordmark
  **tinta** set in Newsreader.
- Must work at 16 px favicon and 1024 px app icon, in one colour, on paper and on night ink.
- Deliverables: SVG mark + wordmark, 1024×1024 icon without transparency, favicon set,
  1200×630 social image.
- Must not resemble the original's mark in shape, colour pair or letterform trick. Place the
  original and drafts side by side before approving.
- Shipped interim: a custom inline SVG drop (favicon + sidebar), no original artwork.

## Voice

Three words: **tenang, jernih, jujur** — calm, clear, honest.

- Calm, not precious. Short sentences, no mystic writing-talk.
- Clear, not clinical. Say "Tersimpan", not "Autosave state: success".
- Honest, not salesy. Say "Data tinggal di peramban ini", not "Aman selamanya".
- Do: lead with the user's intent; explain irreversible actions; stay in Indonesian.
- Don't: use "Pro", "upgrade", "unlock", fake urgency, exclamation marks, or imitate
  another writing app's slogans.

Ten rewritten strings: "Semua terbuka", "Gratis selamanya. Bukan uji coba.", "Tersimpan",
"Menyimpan…", "Rakmu masih kosong", "Tidak ada yang cocok", "Belum ada riwayat.",
"Data tinggal di peramban ini", "Taruh di sampah dulu, bukan langsung hilang",
"Tulisanmu lebih penting daripada aplikasinya."

## Sweep

To run after all text is final:

```bash
python3 /tmp/replica-skill/replica-brand/sweep.py . --config replica/brand.json
```

Result recorded in `replica/build-log.md`. The shipped UI, README, index.html, manifest and
icons contain no original name, domain, colour or asset.
