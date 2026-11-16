import { useMemo, useState } from "react";
import {
  ArrowRight,
  BookOpen,
  Library as LibraryIcon,
  Pencil,
  Plus,
  Search,
  Trash2,
  X,
} from "lucide-react";
import { useStore } from "../lib/store";
import { navigate } from "../lib/router";
import { countWords, formatNumber, timeAgo } from "../lib/text";
import { PageHeader } from "../components/PageHeader";
import { BookCover } from "../components/Cover";
import { BookDialog, type BookValues } from "../components/BookDialog";
import { Menu, MenuItem, MenuSeparator } from "../components/Menu";
import { ConfirmDialog } from "../components/Modal";
import { EmptyState } from "../components/EmptyState";
import { toast } from "../lib/toast";
import type { Book } from "../lib/types";

export function LibraryScreen() {
  const { data, actions } = useStore();
  const [query, setQuery] = useState("");
  const [activeTag, setActiveTag] = useState("semua");
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<Book | null>(null);
  const [deleting, setDeleting] = useState<Book | null>(null);

  const liveDocs = useMemo(() => data.docs.filter((doc) => doc.deletedAt === null), [data.docs]);

  const docStats = useMemo(() => {
    const map = new Map<string, { count: number; words: number }>();
    for (const doc of liveDocs) {
      const entry = map.get(doc.bookId) ?? { count: 0, words: 0 };
      entry.count += 1;
      entry.words += countWords(doc.body);
      map.set(doc.bookId, entry);
    }
    return map;
  }, [liveDocs]);

  const totalWords = useMemo(
    () => liveDocs.reduce((total, doc) => total + countWords(doc.body), 0),
    [liveDocs],
  );

  const tags = useMemo(
    () => Array.from(new Set(data.books.flatMap((book) => book.tags))).sort((a, b) => a.localeCompare(b)),
    [data.books],
  );

  const books = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return [...data.books]
      .filter((book) => {
        const matchesTag = activeTag === "semua" || book.tags.includes(activeTag);
        const matchesQuery =
          !needle ||
          book.title.toLowerCase().includes(needle) ||
          book.author.toLowerCase().includes(needle) ||
          book.tags.some((tag) => tag.toLowerCase().includes(needle));
        return matchesTag && matchesQuery;
      })
      .sort((a, b) => b.updatedAt - a.updatedAt);
  }, [data.books, query, activeTag]);

  const recent = data.docs.find(
    (doc) => doc.id === data.settings.lastDocId && doc.deletedAt === null,
  );
  const recentBook = recent ? data.books.find((book) => book.id === recent.bookId) : null;

  const submitBook = (values: BookValues) => {
    if (editing) {
      actions.updateBook(editing.id, values);
      toast("Detail buku diperbarui.", "success");
      return;
    }
    const id = actions.createBook(values);
    toast("Buku baru siap.", "success");
    navigate(`#/buku/${id}`);
  };

  const hasBooks = data.books.length > 0;

  return (
    <div className="library-screen">
      <PageHeader
        eyebrow="Ruang kerja"
        title="Pustaka"
        subtitle={
          hasBooks
            ? `${data.books.length} buku · ${liveDocs.length} naskah · ${formatNumber(totalWords)} kata`
            : "Semua tulisanmu, di satu tempat yang tidak mengunci apa pun."
        }
        actions={
          <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>
            <Plus size={17} />
            Buku baru
          </button>
        }
      />

      {recent && recentBook ? (
        <a className="resume-card" href={`#/tulis/${recent.id}`}>
          <span className="resume-icon" aria-hidden="true">
            <BookOpen size={20} />
          </span>
          <span className="resume-text">
            <span className="resume-kicker">Lanjutkan menulis</span>
            <span className="resume-title">{recent.title || "Tanpa judul"}</span>
            <span className="resume-meta">
              {recentBook.title} · {formatNumber(countWords(recent.body))} kata · {timeAgo(recent.updatedAt)}
            </span>
          </span>
          <ArrowRight size={19} className="resume-arrow" aria-hidden="true" />
        </a>
      ) : null}

      {hasBooks ? (
        <div className="library-toolbar">
          <div className="search-field">
            <Search size={17} aria-hidden="true" />
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Cari judul, penulis, atau label…"
              aria-label="Cari buku"
            />
            {query ? (
              <button
                type="button"
                className="search-clear"
                onClick={() => setQuery("")}
                aria-label="Bersihkan pencarian"
              >
                <X size={14} />
              </button>
            ) : null}
          </div>
          <div className="tag-filter" role="group" aria-label="Saring berdasarkan label">
            <button
              type="button"
              className={`chip${activeTag === "semua" ? " is-active" : ""}`}
              onClick={() => setActiveTag("semua")}
              aria-pressed={activeTag === "semua"}
            >
              Semua
            </button>
            {tags.map((tag) => (
              <button
                key={tag}
                type="button"
                className={`chip${activeTag === tag ? " is-active" : ""}`}
                onClick={() => setActiveTag(tag)}
                aria-pressed={activeTag === tag}
              >
                {tag}
              </button>
            ))}
          </div>
        </div>
      ) : null}

      {books.length > 0 ? (
        <div className="book-grid">
          {books.map((book) => {
            const stats = docStats.get(book.id) ?? { count: 0, words: 0 };
            return (
              <article className="book-card" key={book.id}>
                <a className="book-card-link" href={`#/buku/${book.id}`}>
                  <BookCover title={book.title} color={book.color} />
                  <div className="book-card-body">
                    <h2 className="book-card-title">{book.title}</h2>
                    <p className="book-card-author">{book.author || "Tanpa penulis"}</p>
                    <p className="book-card-meta">
                      {stats.count} naskah · {formatNumber(stats.words)} kata
                    </p>
                    {book.tags.length > 0 ? (
                      <div className="book-card-tags">
                        {book.tags.map((tag) => (
                          <span className="mini-tag" key={tag}>
                            {tag}
                          </span>
                        ))}
                      </div>
                    ) : null}
                    <p className="book-card-updated">Diperbarui {timeAgo(book.updatedAt)}</p>
                  </div>
                </a>
                <div className="book-card-menu">
                  <Menu label={`Aksi untuk buku ${book.title}`}>
                    {(close) => (
                      <>
                        <MenuItem
                          icon={<BookOpen size={15} />}
                          onClick={() => {
                            close();
                            navigate(`#/buku/${book.id}`);
                          }}
                        >
                          Buka buku
                        </MenuItem>
                        <MenuItem
                          icon={<Pencil size={15} />}
                          onClick={() => {
                            close();
                            setEditing(book);
                          }}
                        >
                          Ubah detail
                        </MenuItem>
                        <MenuSeparator />
                        <MenuItem
                          icon={<Trash2 size={15} />}
                          danger
                          onClick={() => {
                            close();
                            setDeleting(book);
                          }}
                        >
                          Hapus buku
                        </MenuItem>
                      </>
                    )}
                  </Menu>
                </div>
              </article>
            );
          })}
        </div>
      ) : null}

      {hasBooks && books.length === 0 ? (
        <EmptyState
          icon={<Search size={26} />}
          title="Tidak ada buku yang cocok"
          description="Coba kata kunci lain atau tampilkan semua label."
          action={
            <button
              type="button"
              className="btn"
              onClick={() => {
                setQuery("");
                setActiveTag("semua");
              }}
            >
              Tampilkan semua
            </button>
          }
        />
      ) : null}

      {!hasBooks ? (
        <EmptyState
          icon={<LibraryIcon size={28} />}
          title="Rakmu masih kosong"
          description="Mulai dari satu buku. Di dalamnya bisa ada bagian dan bab sebanyak yang kamu mau."
          action={
            <button type="button" className="btn btn-primary" onClick={() => setCreating(true)}>
              <Plus size={17} />
              Buat buku pertama
            </button>
          }
        />
      ) : null}

      <BookDialog
        open={creating || editing !== null}
        book={editing}
        onClose={() => {
          setCreating(false);
          setEditing(null);
        }}
        onSubmit={submitBook}
      />

      <ConfirmDialog
        open={deleting !== null}
        title={`Hapus “${deleting?.title ?? ""}”?`}
        message="Seluruh bagian dan naskah di dalam buku ini ikut dihapus permanen, termasuk riwayatnya. Kalau hanya ingin menyembunyikan sementara, pindahkan naskahnya ke sampah satu per satu."
        confirmLabel="Hapus buku"
        onClose={() => setDeleting(null)}
        onConfirm={() => {
          if (!deleting) return;
          actions.deleteBook(deleting.id);
          toast("Buku dihapus.", "danger");
          setDeleting(null);
        }}
      />
    </div>
  );
}
