import { useEffect, useMemo, useRef, useState } from "react";
import {
  ArrowLeft,
  Bold,
  CalendarDays,
  Check,
  Code2,
  Copy,
  Download,
  Eye,
  FileText,
  Hash,
  Heading2,
  Image as ImageIcon,
  Italic,
  Link2,
  List,
  ListOrdered,
  Maximize2,
  Minimize2,
  Minus,
  Move,
  Plus,
  Quote,
  Redo2,
  Settings as SettingsIcon,
  Sparkles,
  Target,
  Trash2,
  Type,
  Undo2,
  Wand2,
} from "lucide-react";
import { useStore } from "../lib/store";
import { navigate } from "../lib/router";
import {
  countChars,
  countWords,
  dayKey,
  formatDate,
  formatNumber,
} from "../lib/text";
import { renderMarkdown } from "../lib/markdown";
import { exportDoc, exportDocImage, openPrintView } from "../lib/export";
import { toast } from "../lib/toast";
import { Menu, MenuItem, MenuLabel, MenuSeparator } from "../components/Menu";
import { ConfirmDialog, Modal } from "../components/Modal";
import { MoveDocDialog } from "../components/MoveDocDialog";
import { EmptyState } from "../components/EmptyState";

interface EditorProps {
  docId: string;
}

function typeset(source: string): string {
  let text = source.replace(/\r\n/g, "\n");
  text = text
    .split("\n")
    .map((line) => line.replace(/[ \t]+$/g, ""))
    .join("\n");
  text = text.replace(/[ \t]{2,}/g, " ");
  text = text.replace(/\n{3,}/g, "\n\n");
  text = text.replace(/"([^"\n]{1,400})"/g, "“$1”");
  text = text.replace(/(^|\s)'([^'\n]{1,200})'(?=\s|[.,!?;:]|$)/g, "$1‘$2’");
  return text.trim();
}

export function EditorScreen({ docId }: EditorProps) {
  const { data, actions } = useStore();
  const doc = data.docs.find((item) => item.id === docId && item.deletedAt === null);
  const book = data.books.find((item) => item.id === doc?.bookId);
  const volume = data.volumes.find((item) => item.id === doc?.volumeId);
  const settings = data.settings;

  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const historyRef = useRef<{ undo: string[]; redo: string[] }>({ undo: [], redo: [] });
  const lastInputRef = useRef(0);
  const [mode, setMode] = useState<"write" | "preview">("write");
  const [focusMode, setFocusMode] = useState(false);
  const [saveState, setSaveState] = useState<"saved" | "saving">("saved");
  const [exportOpen, setExportOpen] = useState(false);
  const [exportingImage, setExportingImage] = useState(false);
  const [typesetOpen, setTypesetOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [moveOpen, setMoveOpen] = useState(false);
  const [quickAddOpen, setQuickAddOpen] = useState(false);
  const [phrase, setPhrase] = useState("");

  const words = doc ? countWords(doc.body) : 0;
  const chars = doc ? countChars(doc.body) : 0;
  const wordsToday = data.stats[dayKey()] ?? 0;
  const goal = Math.max(1, settings.dailyGoal);
  const goalPercent = Math.min(100, Math.round((wordsToday / goal) * 100));

  const hasDoc = Boolean(doc);

  useEffect(() => {
    if (!hasDoc) return;
    actions.updateSettings({ lastDocId: docId });
    actions.snapshot(docId, "auto");
    const interval = window.setInterval(() => actions.snapshot(docId, "auto"), 120_000);
    return () => {
      window.clearInterval(interval);
      actions.snapshot(docId, "auto");
    };
  }, [docId, hasDoc, actions]);

  useEffect(() => {
    if (!doc) return;
    setSaveState("saving");
    const timer = window.setTimeout(() => setSaveState("saved"), 550);
    return () => window.clearTimeout(timer);
  }, [doc?.body, doc?.title]);

  useEffect(() => {
    const element = textareaRef.current;
    if (!element) return;
    element.style.height = "auto";
    element.style.height = `${element.scrollHeight}px`;
  }, [doc?.body, mode, settings.font, settings.fontSize, settings.lineHeight, settings.pageWidth]);

  useEffect(() => {
    if (!focusMode) return;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setFocusMode(false);
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [focusMode]);

  useEffect(() => {
    setMode("write");
    setFocusMode(false);
    setQuickAddOpen(false);
    historyRef.current = { undo: [], redo: [] };
    lastInputRef.current = 0;
  }, [docId]);

  const dailyGoalLabel = useMemo(
    () => `${formatNumber(wordsToday)} dari ${formatNumber(goal)} kata hari ini`,
    [wordsToday, goal],
  );

  if (!doc) {
    return (
      <div className="editor">
        <EmptyState
          icon={<FileText size={26} />}
          title="Naskah tidak ditemukan"
          description="Naskah ini mungkin sudah dihapus, atau sedang berada di sampah. Kembali ke pustaka untuk memilih tulisan lain."
          action={
            <a className="btn btn-primary" href="#/pustaka">
              <ArrowLeft size={16} />
              Kembali ke pustaka
            </a>
          }
        />
      </div>
    );
  }

  const recordHistory = (previous: string, next: string) => {
    const now = Date.now();
    const gap = now - lastInputRef.current;
    if (gap > 700 || Math.abs(next.length - previous.length) > 2) {
      historyRef.current.undo.push(previous);
      if (historyRef.current.undo.length > 120) historyRef.current.undo.shift();
      historyRef.current.redo = [];
    }
    lastInputRef.current = now;
  };

  const updateBody = (body: string) => {
    recordHistory(doc.body, body);
    actions.updateDoc(doc.id, { body });
  };

  const undo = () => {
    const previous = historyRef.current.undo.pop();
    if (previous === undefined) return;
    historyRef.current.redo.push(doc.body);
    actions.updateDoc(doc.id, { body: previous });
    lastInputRef.current = 0;
    requestAnimationFrame(() => {
      const element = textareaRef.current;
      if (!element) return;
      element.focus();
      element.setSelectionRange(previous.length, previous.length);
    });
  };

  const redo = () => {
    const next = historyRef.current.redo.pop();
    if (next === undefined) return;
    historyRef.current.undo.push(doc.body);
    actions.updateDoc(doc.id, { body: next });
    lastInputRef.current = 0;
    requestAnimationFrame(() => {
      const element = textareaRef.current;
      if (!element) return;
      element.focus();
      element.setSelectionRange(next.length, next.length);
    });
  };

  const canUndo = historyRef.current.undo.length > 0;
  const canRedo = historyRef.current.redo.length > 0;

  const leave = () => {
    actions.snapshot(doc.id, "auto");
    navigate(`#/buku/${doc.bookId}`);
  };

  const openHistory = () => {
    actions.snapshot(doc.id, "auto");
    navigate(`#/riwayat/${doc.id}`);
  };

  const replaceRange = (
    start: number,
    end: number,
    text: string,
    selectStart?: number,
    selectEnd?: number,
  ) => {
    const next = doc.body.slice(0, start) + text + doc.body.slice(end);
    recordHistory(doc.body, next);
    actions.updateDoc(doc.id, { body: next });
    requestAnimationFrame(() => {
      const element = textareaRef.current;
      if (!element) return;
      element.focus();
      const caretStart = selectStart ?? start + text.length;
      element.setSelectionRange(caretStart, selectEnd ?? caretStart);
    });
  };

  const wrapSelection = (before: string, after = before, placeholder = "teks") => {
    const element = textareaRef.current;
    if (!element) return;
    const start = element.selectionStart;
    const end = element.selectionEnd;
    const selected = doc.body.slice(start, end) || placeholder;
    replaceRange(start, end, `${before}${selected}${after}`, start + before.length, start + before.length + selected.length);
  };

  const prefixLines = (prefix: string) => {
    const element = textareaRef.current;
    if (!element) return;
    const start = element.selectionStart;
    const end = Math.max(element.selectionEnd, start);
    const lineStart = doc.body.lastIndexOf("\n", start - 1) + 1;
    const block = doc.body.slice(lineStart, end);
    const prefixed = block
      .split("\n")
      .map((line) => `${prefix}${line}`)
      .join("\n");
    replaceRange(lineStart, end, prefixed, lineStart, lineStart + prefixed.length);
  };

  const insertText = (text: string) => {
    const element = textareaRef.current;
    if (!element) return;
    replaceRange(element.selectionStart, element.selectionEnd, text);
  };

  const centerCaret = () => {
    if (!settings.typewriter) return;
    const element = textareaRef.current;
    if (!element) return;
    const before = element.value.slice(0, element.selectionStart);
    const line = before.split("\n").length - 1;
    const lineHeight = parseFloat(getComputedStyle(element).lineHeight) || 32;
    const rect = element.getBoundingClientRect();
    const lineTop = rect.top + window.scrollY + line * lineHeight;
    window.scrollTo({ top: Math.max(0, lineTop - window.innerHeight / 2), behavior: "auto" });
  };

  const onKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    const element = event.currentTarget;
    const mod = event.metaKey || event.ctrlKey;
    const start = element.selectionStart;
    const end = element.selectionEnd;

    if (mod && event.key.toLowerCase() === "b") {
      event.preventDefault();
      wrapSelection("**", "**");
      return;
    }
    if (mod && event.key.toLowerCase() === "i") {
      event.preventDefault();
      wrapSelection("*", "*");
      return;
    }
    if (mod && event.key.toLowerCase() === "s") {
      event.preventDefault();
      actions.snapshot(doc.id, "manual");
      toast("Tersimpan di perangkat dan ditambahkan ke riwayat.", "success");
      return;
    }
    if (mod && event.key.toLowerCase() === "z") {
      event.preventDefault();
      if (event.shiftKey) redo();
      else undo();
      return;
    }
    if (mod && event.key.toLowerCase() === "y") {
      event.preventDefault();
      redo();
      return;
    }
    if (event.key === "Escape" && focusMode) {
      setFocusMode(false);
      return;
    }
    if (mod || event.altKey) return;

    const pairs: Record<string, string> = { '"': '"', "(": ")", "[": "]", "{": "}", "`": "`" };
    if (pairs[event.key] && start === end) {
      const next = doc.body[start] ?? "";
      if (!next || /\s/.test(next)) {
        event.preventDefault();
        replaceRange(start, end, pairs[event.key], start + 1);
        return;
      }
    }

    if (event.key === "Backspace" && start === end) {
      const previous = doc.body[start - 1];
      const next = doc.body[start];
      const opener = Object.entries(pairs).find(([, closer]) => closer === next)?.[0];
      if (opener && previous === opener) {
        event.preventDefault();
        replaceRange(start - 1, start + 1, "", start - 1);
        return;
      }
    }

    if (event.key === "Tab") {
      event.preventDefault();
      replaceRange(start, end, "  ", start + 2);
      return;
    }

    if (event.key === "Enter" && !event.shiftKey) {
      const before = doc.body.slice(0, start);
      const lineStart = before.lastIndexOf("\n") + 1;
      const line = before.slice(lineStart);
      const listMatch = line.match(/^(\s*)([-*+]|\d+[.)])\s+(.*)$/);
      const quoteMatch = line.match(/^(\s*>\s?)(.*)$/);

      if (listMatch && listMatch[3].trim() === "") {
        event.preventDefault();
        replaceRange(lineStart, start, "", lineStart);
        return;
      }
      if (listMatch) {
        event.preventDefault();
        const marker = /^\d/.test(listMatch[2])
          ? `${Number.parseInt(listMatch[2], 10) + 1}.`
          : listMatch[2];
        replaceRange(start, end, `\n${listMatch[1]}${marker} `, start + 2 + listMatch[1].length + marker.length);
        return;
      }
      if (quoteMatch) {
        event.preventDefault();
        replaceRange(start, end, `\n${quoteMatch[1]}`, start + 1 + quoteMatch[1].length);
      }
    }
  };

  const toggleMarkdown = () => {
    actions.updateDoc(doc.id, { markdown: !doc.markdown });
    if (doc.markdown) setMode("write");
  };

  const runTypeset = () => {
    actions.snapshot(doc.id, "manual");
    const next = typeset(doc.body);
    if (next === doc.body) {
      toast("Naskah sudah rapi. Tidak ada yang diubah.", "info");
      return;
    }
    actions.updateDoc(doc.id, { body: next });
    toast("Naskah dirapikan. Versi sebelumnya ada di Riwayat.", "success");
  };

  const runImageExport = async () => {
    setExportingImage(true);
    try {
      await exportDocImage({
        doc,
        bookTitle: book?.title ?? "Tinta",
        theme: settings.theme,
      });
      toast("Gambar panjang berhasil diekspor.", "success");
      setExportOpen(false);
    } catch (error) {
      toast(error instanceof Error ? error.message : "Gambar gagal dibuat.", "danger");
    } finally {
      setExportingImage(false);
    }
  };

  const remove = () => {
    actions.deleteDoc(doc.id);
    navigate(`#/buku/${doc.bookId}`);
    toast("Naskah dipindahkan ke sampah. Masih bisa dipulihkan.", "info");
  };

  return (
    <div className={`editor${focusMode ? " is-focus" : ""}${settings.rulings ? " has-rulings" : ""}`}>
      <header className="editor-top">
        <button type="button" className="icon-btn" onClick={leave} aria-label="Kembali ke buku">
          <ArrowLeft size={19} />
        </button>
        <div className="editor-crumbs">
          <a href={`#/buku/${doc.bookId}`}>{book?.title ?? "Pustaka"}</a>
          {volume ? (
            <>
              <span aria-hidden="true">/</span>
              <span className="crumb-current">{volume.title}</span>
            </>
          ) : null}
        </div>
        <div className="editor-top-actions">
          <span className={`save-status save-status--${saveState}`} aria-live="polite">
            {saveState === "saved" ? <Check size={13} /> : <span className="save-dot" />}
            <span className="save-label">{saveState === "saved" ? "Tersimpan" : "Menyimpan…"}</span>
          </span>
          <button
            type="button"
            className={`icon-btn${doc.markdown ? " is-on" : ""}`}
            onClick={toggleMarkdown}
            aria-pressed={doc.markdown}
            title={doc.markdown ? "Matikan Markdown" : "Aktifkan Markdown"}
            aria-label={doc.markdown ? "Matikan Markdown" : "Aktifkan Markdown"}
          >
            <Hash size={18} />
          </button>
          <button
            type="button"
            className="icon-btn"
            onClick={() => setMode(mode === "write" ? "preview" : "write")}
            disabled={!doc.markdown}
            aria-pressed={mode === "preview"}
            title={doc.markdown ? "Pratinjau Markdown" : "Aktifkan Markdown untuk pratinjau"}
            aria-label="Pratinjau Markdown"
          >
            <Eye size={18} />
          </button>
          <Menu label="Menu naskah">
            {(close) => (
              <>
                <MenuLabel>Naskah</MenuLabel>
                <MenuItem
                  icon={<Sparkles size={15} />}
                  onClick={() => {
                    close();
                    actions.snapshot(doc.id, "manual");
                    toast("Versi sekarang disimpan ke Riwayat.", "success");
                  }}
                >
                  Simpan ke riwayat
                </MenuItem>
                <MenuItem
                  icon={<Undo2 size={15} />}
                  onClick={() => {
                    close();
                    openHistory();
                  }}
                >
                  Lihat riwayat versi
                </MenuItem>
                <MenuItem
                  icon={<Copy size={15} />}
                  onClick={() => {
                    close();
                    const id = actions.duplicateDoc(doc.id);
                    if (id) {
                      actions.updateSettings({ lastDocId: id });
                      navigate(`#/tulis/${id}`);
                      toast("Salinan naskah dibuat.", "success");
                    }
                  }}
                >
                  Duplikat naskah
                </MenuItem>
                <MenuItem
                  icon={<Move size={15} />}
                  onClick={() => {
                    close();
                    setMoveOpen(true);
                  }}
                >
                  Pindahkan ke…
                </MenuItem>
                <MenuSeparator />
                <MenuLabel>Tampilan</MenuLabel>
                <MenuItem
                  icon={<Type size={15} />}
                  onClick={() => {
                    close();
                    actions.updateSettings({ typewriter: !settings.typewriter });
                  }}
                >
                  {settings.typewriter ? "Matikan mode ketik" : "Mode ketik (typewriter)"}
                </MenuItem>
                <MenuItem
                  icon={<Redo2 size={15} />}
                  onClick={() => {
                    close();
                    actions.updateSettings({ rulings: !settings.rulings });
                  }}
                >
                  {settings.rulings ? "Sembunyikan garis bantu" : "Tampilkan garis bantu"}
                </MenuItem>
                <MenuItem
                  icon={<Maximize2 size={15} />}
                  onClick={() => {
                    close();
                    setFocusMode(true);
                  }}
                >
                  Mode fokus
                </MenuItem>
                <MenuSeparator />
                <MenuItem
                  icon={<Wand2 size={15} />}
                  onClick={() => {
                    close();
                    setTypesetOpen(true);
                  }}
                >
                  Rapikan naskah…
                </MenuItem>
                <MenuItem
                  icon={<CalendarDays size={15} />}
                  onClick={() => {
                    close();
                    insertText(formatDate(Date.now()));
                  }}
                >
                  Sisipkan tanggal
                </MenuItem>
                <MenuItem
                  icon={<SettingsIcon size={15} />}
                  onClick={() => {
                    close();
                    actions.snapshot(doc.id, "auto");
                    navigate("#/pengaturan");
                  }}
                >
                  Buka setelan menulis
                </MenuItem>
                <MenuSeparator />
                <MenuItem
                  icon={<Trash2 size={15} />}
                  danger
                  onClick={() => {
                    close();
                    setDeleteOpen(true);
                  }}
                >
                  Pindahkan ke sampah
                </MenuItem>
              </>
            )}
          </Menu>
          <button
            type="button"
            className="icon-btn"
            onClick={() => setFocusMode((value) => !value)}
            aria-pressed={focusMode}
            aria-label={focusMode ? "Keluar mode fokus" : "Masuk mode fokus"}
            title={focusMode ? "Keluar mode fokus (Esc)" : "Mode fokus"}
          >
            {focusMode ? <Minimize2 size={18} /> : <Maximize2 size={18} />}
          </button>
          <button type="button" className="btn btn-primary btn-sm export-btn" onClick={() => setExportOpen(true)}>
            <Download size={15} />
            <span className="hide-sm">Ekspor</span>
          </button>
        </div>
      </header>

      {mode === "write" ? (
        <div className="formatbar" role="toolbar" aria-label="Format teks">
          <button
            type="button"
            className="icon-btn"
            onClick={undo}
            disabled={!canUndo}
            aria-label="Urungkan"
            title="Urungkan (Ctrl/⌘Z)"
          >
            <Undo2 size={16} />
          </button>
          <button
            type="button"
            className="icon-btn"
            onClick={redo}
            disabled={!canRedo}
            aria-label="Ulangi"
            title="Ulangi (Ctrl/⌘Shift+Z)"
          >
            <Redo2 size={16} />
          </button>
          <span className="formatbar-sep" aria-hidden="true" />
          {doc.markdown ? (
            <>
              <button type="button" className="icon-btn" onClick={() => wrapSelection("**", "**")} aria-label="Tebal" title="Tebal (Ctrl+B)">
                <Bold size={16} />
              </button>
              <button type="button" className="icon-btn" onClick={() => wrapSelection("*", "*")} aria-label="Miring" title="Miring (Ctrl+I)">
                <Italic size={16} />
              </button>
              <button type="button" className="icon-btn" onClick={() => prefixLines("## ")} aria-label="Subjudul" title="Subjudul">
                <Heading2 size={16} />
              </button>
              <span className="formatbar-sep" aria-hidden="true" />
              <button type="button" className="icon-btn" onClick={() => prefixLines("- ")} aria-label="Daftar butir" title="Daftar butir">
                <List size={16} />
              </button>
              <button type="button" className="icon-btn" onClick={() => prefixLines("1. ")} aria-label="Daftar bernomor" title="Daftar bernomor">
                <ListOrdered size={16} />
              </button>
              <button type="button" className="icon-btn" onClick={() => prefixLines("> ")} aria-label="Kutipan" title="Kutipan">
                <Quote size={16} />
              </button>
              <span className="formatbar-sep" aria-hidden="true" />
              <button type="button" className="icon-btn" onClick={() => wrapSelection("`", "`", "kode")} aria-label="Kode sebaris" title="Kode sebaris">
                <Code2 size={16} />
              </button>
              <button type="button" className="icon-btn" onClick={() => wrapSelection("[", "](https://)", "tautan")} aria-label="Tautan" title="Tautan">
                <Link2 size={16} />
              </button>
              <button type="button" className="icon-btn" onClick={() => insertText("\n\n---\n\n")} aria-label="Garis pemisah" title="Garis pemisah">
                <Minus size={16} />
              </button>
              <button type="button" className="icon-btn" onClick={() => insertText(formatDate(Date.now()))} aria-label="Sisipkan tanggal" title="Sisipkan tanggal">
                <CalendarDays size={16} />
              </button>
              <span className="formatbar-hint">Markdown aktif — pratinjau tersedia di kanan atas.</span>
            </>
          ) : (
            <div className="formatbar-off">
              <span>Markdown nonaktif. Tulisan tetap tersimpan sebagai teks biasa.</span>
              <button type="button" className="btn btn-sm" onClick={toggleMarkdown}>
                <Hash size={14} />
                Aktifkan Markdown
              </button>
            </div>
          )}
        </div>
      ) : null}

      <div className="editor-shell">
        <div className={`paper${settings.rulings ? " has-rulings" : ""}`}>
          {mode === "write" ? (
            <>
              <input
                className="paper-title"
                value={doc.title}
                onChange={(event) => actions.updateDoc(doc.id, { title: event.target.value })}
                placeholder="Judul naskah"
                aria-label="Judul naskah"
                maxLength={120}
                spellCheck
              />
              <div className="paper-divider" aria-hidden="true" />
              <textarea
                ref={textareaRef}
                className="paper-text"
                value={doc.body}
                onChange={(event) => {
                  updateBody(event.target.value);
                  if (settings.typewriter) requestAnimationFrame(centerCaret);
                }}
                onKeyDown={onKeyDown}
                onSelect={centerCaret}
                onClick={centerCaret}
                placeholder="Mulai menulis. Tidak ada tombol simpan, karena semuanya tersimpan sendiri."
                aria-label="Isi naskah"
                spellCheck
              />
            </>
          ) : (
            <article className="preview markdown-body" aria-label="Pratinjau Markdown">
              <h1 className="preview-title">{doc.title || "Tanpa judul"}</h1>
              <div dangerouslySetInnerHTML={{ __html: renderMarkdown(doc.body) }} />
            </article>
          )}
        </div>
      </div>

      <div className="word-pill" aria-live="off">
        <strong>{formatNumber(words)}</strong> kata
        <span aria-hidden="true">·</span>
        <span>{formatNumber(chars)} karakter</span>
      </div>

      <div className="quickbar">
        {quickAddOpen ? (
          <form
            className="quick-add"
            onSubmit={(event) => {
              event.preventDefault();
              const value = phrase.trim();
              if (!value) return;
              actions.updateSettings({ phrases: [...settings.phrases, value].slice(0, 40) });
              setPhrase("");
              setQuickAddOpen(false);
              toast("Frasa ditambahkan ke bilah sisipan. Kelola di Setelan.", "success");
            }}
          >
            <label className="sr-only" htmlFor="quick-phrase">
              Frasa baru
            </label>
            <input
              id="quick-phrase"
              className="input"
              value={phrase}
              onChange={(event) => setPhrase(event.target.value)}
              placeholder="Teks yang sering kamu pakai…"
              maxLength={80}
              autoFocus
            />
            <button type="submit" className="btn btn-primary btn-sm">
              Tambah
            </button>
            <button type="button" className="btn btn-sm" onClick={() => setQuickAddOpen(false)}>
              Batal
            </button>
          </form>
        ) : null}
        <div className="quickbar-inner">
          <span className="quickbar-label">
            <Sparkles size={14} aria-hidden="true" />
            Sisipan
          </span>
          <div className="phrase-scroll">
            {settings.phrases.slice(0, 18).map((item, index) => (
              <button
                key={`${item}-${index}`}
                type="button"
                className="phrase-chip"
                onClick={() => insertText(item)}
                title={`Sisipkan “${item}”`}
              >
                {item}
              </button>
            ))}
            <button
              type="button"
              className="phrase-chip phrase-add"
              onClick={() => setQuickAddOpen((value) => !value)}
              aria-expanded={quickAddOpen}
            >
              <Plus size={13} />
              Frasa
            </button>
          </div>
          <span className="quick-goal" title={dailyGoalLabel}>
            <Target size={13} aria-hidden="true" />
            {formatNumber(wordsToday)}/{formatNumber(goal)}
            <span className="quick-goal-track" aria-hidden="true">
              <span style={{ width: `${goalPercent}%` }} />
            </span>
          </span>
        </div>
      </div>

      <Modal
        open={exportOpen}
        onClose={() => setExportOpen(false)}
        title="Ekspor naskah"
        description="Semua format terbuka. Tidak ada batas ekspor, tidak ada kunci Pro."
      >
        <div className="export-list">
          <button
            type="button"
            className="export-option"
            onClick={() => {
              exportDoc(doc, book?.title ?? "Tinta", "txt");
              setExportOpen(false);
              toast("Teks diekspor.", "success");
            }}
          >
            <span className="export-icon"><FileText size={18} /></span>
            <span>
              <strong>Teks polos (.txt)</strong>
              <small>Untuk dibuka di aplikasi apa pun.</small>
            </span>
          </button>
          <button
            type="button"
            className="export-option"
            onClick={() => {
              exportDoc(doc, book?.title ?? "Tinta", "md");
              setExportOpen(false);
              toast("Markdown diekspor.", "success");
            }}
          >
            <span className="export-icon"><Hash size={18} /></span>
            <span>
              <strong>Markdown (.md)</strong>
              <small>Format asli, siap dipakai di editor lain.</small>
            </span>
          </button>
          <button
            type="button"
            className="export-option"
            onClick={() => {
              exportDoc(doc, book?.title ?? "Tinta", "html");
              setExportOpen(false);
              toast("HTML diekspor.", "success");
            }}
          >
            <span className="export-icon"><Code2 size={18} /></span>
            <span>
              <strong>Halaman HTML (.html)</strong>
              <small>Satu berkas rapi yang bisa dibuka di peramban.</small>
            </span>
          </button>
          <button type="button" className="export-option" onClick={runImageExport} disabled={exportingImage}>
            <span className="export-icon"><ImageIcon size={18} /></span>
            <span>
              <strong>{exportingImage ? "Menyiapkan gambar…" : "Gambar panjang (.png)"}</strong>
              <small>Gambar vertikal dengan judul dan hitungan kata.</small>
            </span>
          </button>
          <button
            type="button"
            className="export-option"
            onClick={() => {
              openPrintView(doc, book?.title ?? "Tinta");
              setExportOpen(false);
              toast("Pratinjau cetak dibuka. Pilih “Simpan sebagai PDF”.", "info");
            }}
          >
            <span className="export-icon"><Download size={18} /></span>
            <span>
              <strong>PDF lewat cetak</strong>
              <small>Buka tampilan cetak, lalu simpan sebagai PDF.</small>
            </span>
          </button>
        </div>
        <p className="export-note">
          {formatNumber(words)} kata · {formatNumber(chars)} karakter. Ekspor memakai data yang sudah
          tersimpan otomatis.
        </p>
      </Modal>

      <ConfirmDialog
        open={typesetOpen}
        title="Rapikan naskah?"
        message="Spasi di ujung baris akan dihapus, spasi ganda dirapatkan, baris kosong berlebih dikurangi, dan tanda kutip lurus diubah menjadi kutipan melengkung. Versi sekarang disimpan ke Riwayat lebih dulu."
        confirmLabel="Rapikan"
        danger={false}
        onClose={() => setTypesetOpen(false)}
        onConfirm={runTypeset}
      />

      <ConfirmDialog
        open={deleteOpen}
        title="Pindahkan ke sampah?"
        message={`“${doc.title || "Tanpa judul"}” akan hilang dari buku ini, tetapi masih bisa dipulihkan dari Sampah. Riwayat versinya tetap disimpan.`}
        confirmLabel="Pindahkan"
        danger={false}
        onClose={() => setDeleteOpen(false)}
        onConfirm={remove}
      />

      <MoveDocDialog doc={doc} open={moveOpen} onClose={() => setMoveOpen(false)} />
    </div>
  );
}
