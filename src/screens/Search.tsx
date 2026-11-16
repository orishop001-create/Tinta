import { useMemo, useState, type ReactNode } from "react";
import { ArrowRight, Replace, Search as SearchIcon, BookOpen, SlidersHorizontal } from "lucide-react";
import { useStore } from "../lib/store";
import { excerpt, formatNumber, timeAgo } from "../lib/text";
import type { Doc } from "../lib/types";
import { PageHeader } from "../components/PageHeader";
import { EmptyState } from "../components/EmptyState";
import { ConfirmDialog } from "../components/Modal";
import { toast } from "../lib/toast";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

interface BuiltRegex {
  regex: RegExp | null;
  error: string | null;
}

function buildRegex(query: string, useRegex: boolean, caseSensitive: boolean): BuiltRegex {
  if (!query) return { regex: null, error: null };
  const source = useRegex ? query : escapeRegExp(query);
  try {
    const regex = new RegExp(source, caseSensitive ? "g" : "gi");
    const emptyCheck = new RegExp(source, caseSensitive ? "" : "i");
    if (emptyCheck.test("")) {
      return { regex: null, error: "Pola ini bisa mencocokkan teks kosong. Persempit polanya." };
    }
    return { regex, error: null };
  } catch {
    return { regex: null, error: "Pola regex tidak valid. Periksa tanda kurung dan garis miring." };
  }
}

function countMatches(text: string, regex: RegExp): number {
  if (!text) return 0;
  const clone = new RegExp(regex.source, regex.flags.includes("g") ? regex.flags : `${regex.flags}g`);
  let count = 0;
  let match: RegExpExecArray | null;
  while ((match = clone.exec(text)) !== null) {
    count += 1;
    if (match[0] === "") clone.lastIndex += 1;
    if (count > 5000) break;
  }
  return count;
}

function firstMatchSnippet(text: string, regex: RegExp): string {
  if (!text) return "";
  const clone = new RegExp(regex.source, regex.flags.includes("g") ? regex.flags : `${regex.flags}g`);
  const match = clone.exec(text);
  if (!match) return excerpt(text, 140);
  const start = Math.max(0, match.index - 64);
  const end = Math.min(text.length, match.index + match[0].length + 90);
  return `${start > 0 ? "…" : ""}${text.slice(start, end).replace(/\s+/g, " ").trim()}${
    end < text.length ? "…" : ""
  }`;
}

function Highlight({ text, regex }: { text: string; regex: RegExp | null }): ReactNode {
  if (!regex || !text) return text;
  const clone = new RegExp(regex.source, regex.flags.includes("g") ? regex.flags : `${regex.flags}g`);
  const parts: ReactNode[] = [];
  let last = 0;
  let index = 0;
  let match: RegExpExecArray | null;
  while ((match = clone.exec(text)) !== null && index < 40) {
    if (match.index > last) parts.push(text.slice(last, match.index));
    parts.push(<mark key={`${match.index}-${index}`}>{match[0]}</mark>);
    last = match.index + match[0].length;
    if (match[0] === "") clone.lastIndex += 1;
    index += 1;
  }
  parts.push(text.slice(last));
  return <>{parts}</>;
}

interface Result {
  doc: Doc;
  bookTitle: string;
  count: number;
  snippet: string;
  titleMatch: boolean;
}

export function SearchScreen() {
  const { data, actions } = useStore();
  const [query, setQuery] = useState("");
  const [replacement, setReplacement] = useState("");
  const [caseSensitive, setCaseSensitive] = useState(false);
  const [useRegex, setUseRegex] = useState(false);
  const [bookFilter, setBookFilter] = useState("semua");
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { regex, error } = useMemo(
    () => buildRegex(query, useRegex, caseSensitive),
    [query, useRegex, caseSensitive],
  );

  const results = useMemo<Result[]>(() => {
    if (!regex || error) return [];
    const found: Result[] = [];
    for (const doc of data.docs) {
      if (doc.deletedAt !== null) continue;
      if (bookFilter !== "semua" && doc.bookId !== bookFilter) continue;
      const titleCount = countMatches(doc.title, regex);
      const bodyCount = countMatches(doc.body, regex);
      if (titleCount + bodyCount === 0) continue;
      found.push({
        doc,
        bookTitle: data.books.find((book) => book.id === doc.bookId)?.title ?? "Tanpa buku",
        count: titleCount + bodyCount,
        snippet: firstMatchSnippet(doc.body || doc.title, regex),
        titleMatch: titleCount > 0,
      });
    }
    return found.sort((a, b) => b.doc.updatedAt - a.doc.updatedAt);
  }, [data.docs, data.books, regex, error, bookFilter]);

  const totalMatches = results.reduce((total, result) => total + result.count, 0);

  const runReplace = () => {
    if (!regex) return;
    let changedDocs = 0;
    let replacements = 0;
    for (const result of results) {
      const source = result.doc;
      const titleRegex = new RegExp(regex.source, regex.flags);
      const bodyRegex = new RegExp(regex.source, regex.flags);
      const nextTitle = source.title.replace(titleRegex, () => replacement);
      const nextBody = source.body.replace(bodyRegex, () => replacement);
      if (nextTitle === source.title && nextBody === source.body) continue;
      actions.snapshot(source.id, "auto");
      actions.updateDoc(source.id, { title: nextTitle, body: nextBody });
      changedDocs += 1;
      replacements += result.count;
    }
    toast(
      `${formatNumber(replacements)} penggantian di ${changedDocs} naskah. Versi sebelumnya ada di Riwayat.`,
      "success",
    );
    setConfirmOpen(false);
  };

  return (
    <div className="search-screen">
      <PageHeader
        eyebrow="Lintas buku"
        title="Cari & ganti"
        subtitle="Telusuri judul dan isi semua naskah sekaligus. Ganti satu per satu atau semuanya, tanpa biaya tambahan."
      />

      <div className="search-panel">
        <div className="search-main-row">
          <div className="search-field search-field--lg">
            <SearchIcon size={18} aria-hidden="true" />
            <input
              type="search"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder={useRegex ? "Pola regex, misalnya: bab \\d+" : "Kata atau kalimat yang dicari…"}
              aria-label="Kata yang dicari"
              autoFocus
            />
          </div>
        </div>

        <div className="search-options">
          <label className="check">
            <input
              type="checkbox"
              checked={caseSensitive}
              onChange={(event) => setCaseSensitive(event.target.checked)}
            />
            <span>Bedakan huruf besar/kecil</span>
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={useRegex}
              onChange={(event) => setUseRegex(event.target.checked)}
            />
            <span>Pakai regex</span>
          </label>
          <label className="search-scope">
            <SlidersHorizontal size={14} aria-hidden="true" />
            <span className="sr-only">Batasi ke buku</span>
            <select
              className="select select--sm"
              value={bookFilter}
              onChange={(event) => setBookFilter(event.target.value)}
            >
              <option value="semua">Semua buku</option>
              {data.books.map((book) => (
                <option key={book.id} value={book.id}>
                  {book.title}
                </option>
              ))}
            </select>
          </label>
        </div>

        {error ? <p className="form-error">{error}</p> : null}

        <div className="replace-row">
          <div className="search-field">
            <Replace size={16} aria-hidden="true" />
            <input
              type="text"
              value={replacement}
              onChange={(event) => setReplacement(event.target.value)}
              placeholder="Ganti dengan… (boleh kosong)"
              aria-label="Teks pengganti"
            />
          </div>
          <button
            type="button"
            className="btn"
            disabled={!regex || !query || results.length === 0}
            onClick={() => setConfirmOpen(true)}
          >
            Ganti semua
          </button>
        </div>
        <p className="hint">
          Penggantian berlaku untuk {formatNumber(results.length)} naskah dan {formatNumber(totalMatches)}{" "}
          kecocokan. Versi lama otomatis masuk Riwayat sebelum diubah.
        </p>
      </div>

      {query && !error ? (
        <div className="search-summary">
          <span>
            <strong>{formatNumber(totalMatches)}</strong> kecocokan di{" "}
            <strong>{formatNumber(results.length)}</strong> naskah
          </span>
          {bookFilter !== "semua" ? (
            <span className="search-scope-chip">
              <BookOpen size={13} />
              {data.books.find((book) => book.id === bookFilter)?.title}
            </span>
          ) : null}
        </div>
      ) : null}

      {!query ? (
        <EmptyState
          icon={<SearchIcon size={26} />}
          title="Cari di seluruh tulisanmu"
          description="Judul, isi, sampai catatan kecil di bab yang sudah lama tidak dibuka. Coba kata yang kamu ingat, atau aktifkan regex untuk pencarian lanjutan."
        />
      ) : null}

      {query && !error && results.length === 0 ? (
        <EmptyState
          icon={<SearchIcon size={26} />}
          title="Tidak ada yang cocok"
          description="Coba kata yang lebih pendek, matikan pembedaan huruf besar/kecil, atau periksa pola regex."
        />
      ) : null}

      {results.length > 0 ? (
        <div className="search-results">
          {results.map((result) => (
            <a
              className="search-result"
              key={result.doc.id}
              href={`#/tulis/${result.doc.id}`}
              onClick={() => actions.updateSettings({ lastDocId: result.doc.id })}
            >
              <span className="search-result-top">
                <span className="search-result-book">{result.bookTitle}</span>
                <span className="search-result-count">{result.count} kecocokan</span>
              </span>
              <span className="search-result-title">
                <Highlight text={result.doc.title || "Tanpa judul"} regex={result.titleMatch ? regex : null} />
              </span>
              <span className="search-result-snippet">
                <Highlight text={result.snippet} regex={regex} />
              </span>
              <span className="search-result-meta">Diperbarui {timeAgo(result.doc.updatedAt)}</span>
              <ArrowRight size={16} className="search-result-arrow" aria-hidden="true" />
            </a>
          ))}
        </div>
      ) : null}

      <ConfirmDialog
        open={confirmOpen}
        title={`Ganti ${formatNumber(totalMatches)} kecocokan?`}
        message={`Teks “${query}” di ${results.length} naskah akan diganti dengan “${
          replacement || "(kosong)"
        }”. Versi sebelum perubahan disimpan otomatis ke Riwayat masing-masing naskah.`}
        confirmLabel="Ganti semua"
        danger={false}
        onClose={() => setConfirmOpen(false)}
        onConfirm={runReplace}
      />
    </div>
  );
}
