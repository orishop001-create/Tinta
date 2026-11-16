-- Tinta — reference schema for an OPTIONAL future sync backend.
-- The shipped app is local-first and uses localStorage; this DDL is not executed.
-- Target: SQLite / Cloudflare D1. Times are Unix epoch milliseconds.
-- Access rule: every row is filtered by the authenticated user's id; the client never
-- supplies the owner.

PRAGMA foreign_keys = ON;

CREATE TABLE users (
  id          TEXT PRIMARY KEY,
  email       TEXT NOT NULL UNIQUE,
  created_at  INTEGER NOT NULL
);

CREATE TABLE books (
  id          TEXT PRIMARY KEY,
  user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title       TEXT NOT NULL CHECK (length(trim(title)) > 0),
  author      TEXT NOT NULL DEFAULT '',
  color       TEXT NOT NULL DEFAULT 'clay',
  tags        TEXT NOT NULL DEFAULT '[]', -- JSON array, max 6 enforced in app
  created_at  INTEGER NOT NULL,
  updated_at  INTEGER NOT NULL
);
CREATE INDEX books_owner_updated ON books (user_id, updated_at DESC);

CREATE TABLE volumes (
  id          TEXT PRIMARY KEY,
  user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  book_id     TEXT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
  title       TEXT NOT NULL,
  position    REAL NOT NULL DEFAULT 0,
  created_at  INTEGER NOT NULL
);
CREATE INDEX volumes_book_position ON volumes (book_id, position);

CREATE TABLE docs (
  id          TEXT PRIMARY KEY,
  user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  book_id     TEXT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
  volume_id   TEXT REFERENCES volumes(id) ON DELETE SET NULL,
  title       TEXT NOT NULL DEFAULT '',
  body        TEXT NOT NULL DEFAULT '',
  markdown    INTEGER NOT NULL DEFAULT 0 CHECK (markdown IN (0, 1)),
  position    REAL NOT NULL DEFAULT 0,
  created_at  INTEGER NOT NULL,
  updated_at  INTEGER NOT NULL,
  deleted_at  INTEGER
);
CREATE INDEX docs_book_position ON docs (book_id, volume_id, position);
CREATE INDEX docs_owner_updated ON docs (user_id, updated_at DESC);
CREATE INDEX docs_trash ON docs (user_id, deleted_at);

CREATE TABLE snapshots (
  id          TEXT PRIMARY KEY,
  user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  doc_id      TEXT NOT NULL REFERENCES docs(id) ON DELETE CASCADE,
  title       TEXT NOT NULL DEFAULT '',
  body        TEXT NOT NULL DEFAULT '',
  markdown    INTEGER NOT NULL DEFAULT 0 CHECK (markdown IN (0, 1)),
  reason      TEXT NOT NULL DEFAULT 'auto'
              CHECK (reason IN ('auto', 'manual', 'before-restore')),
  created_at  INTEGER NOT NULL
);
CREATE INDEX snapshots_doc_created ON snapshots (doc_id, created_at DESC);

CREATE TABLE daily_stats (
  user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  day         TEXT NOT NULL CHECK (day GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
  words       INTEGER NOT NULL DEFAULT 0 CHECK (words >= 0),
  PRIMARY KEY (user_id, day)
);

CREATE TABLE settings (
  user_id            TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  theme              TEXT NOT NULL DEFAULT 'paper'
                     CHECK (theme IN ('paper', 'sepia', 'night', 'graphite')),
  font               TEXT NOT NULL DEFAULT 'serif'
                     CHECK (font IN ('serif', 'sans', 'mono')),
  font_size          INTEGER NOT NULL DEFAULT 19 CHECK (font_size BETWEEN 14 AND 30),
  line_height        REAL NOT NULL DEFAULT 1.75 CHECK (line_height BETWEEN 1.3 AND 2.4),
  paragraph_spacing  INTEGER NOT NULL DEFAULT 10 CHECK (paragraph_spacing BETWEEN 0 AND 32),
  page_width         INTEGER NOT NULL DEFAULT 720 CHECK (page_width BETWEEN 520 AND 960),
  typewriter         INTEGER NOT NULL DEFAULT 0 CHECK (typewriter IN (0, 1)),
  rulings            INTEGER NOT NULL DEFAULT 0 CHECK (rulings IN (0, 1)),
  daily_goal         INTEGER NOT NULL DEFAULT 500 CHECK (daily_goal BETWEEN 50 AND 10000),
  phrases            TEXT NOT NULL DEFAULT '[]',
  last_doc_id        TEXT REFERENCES docs(id) ON DELETE SET NULL,
  updated_at         INTEGER NOT NULL
);
