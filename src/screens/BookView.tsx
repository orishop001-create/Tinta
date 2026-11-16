import { useMemo, useState, type DragEvent } from "react";
import {
  ArrowDown,
  ArrowLeft,
  ArrowUp,
  ChevronDown,
  ChevronRight,
  Copy,
  FolderPlus,
  GripVertical,
  Layers,
  Move,
  Pencil,
  Plus,
  Trash2,
} from "lucide-react";
import { useStore } from "../lib/store";
import { navigate } from "../lib/router";
import { countWords, excerpt, formatNumber, timeAgo } from "../lib/text";
import type { Doc, Volume } from "../lib/types";
import { BookCover } from "../components/Cover";
import { BookDialog, type BookValues } from "../components/BookDialog";
import { Menu, MenuItem, MenuSeparator, MenuLabel } from "../components/Menu";
import { ConfirmDialog, Modal } from "../components/Modal";
import { MoveDocDialog } from "../components/MoveDocDialog";
import { EmptyState } from "../components/EmptyState";
import { toast } from "../lib/toast";

interface BookViewProps {
  bookId: string;
}

export function BookView({ bookId }: BookViewProps) {
  const { data, actions } = useStore();
  const book = data.books.find((item) => item.id === bookId);

  const [editingBook, setEditingBook] = useState(false);
  const [deletingBook, setDeletingBook] = useState(false);
  const [volumeEditor, setVolumeEditor] = useState<{ id: string | null; title: string } | null>(null);
  const [deletingVolume, setDeletingVolume] = useState<Volume | null>(null);
  const [moveDoc, setMoveDoc] = useState<Doc | null>(null);
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());
  const [draggingDoc, setDraggingDoc] = useState<string | null>(null);
  const [dropDoc, setDropDoc] = useState<string | null>(null);
  const [draggingVolume, setDraggingVolume] = useState<string | null>(null);

  const docs = useMemo(
    () =>
      data.docs
        .filter((doc) => doc.bookId === bookId && doc.deletedAt === null)
        .sort((a, b) => a.order - b.order || a.createdAt - b.createdAt),
    [data.docs, bookId],
  );

  const volumes = useMemo(
    () =>
      data.volumes
        .filter((volume) => volume.bookId === bookId)
        .sort((a, b) => a.order - b.order || a.createdAt - b.createdAt),
    [data.volumes, bookId],
  );

  const volumeIds = useMemo(() => new Set(volumes.map((volume) => volume.id)), [volumes]);
  const looseDocs = docs.filter((doc) => !doc.volumeId || !volumeIds.has(doc.volumeId));
  const totalWords = docs.reduce((total, doc) => total + countWords(doc.body), 0);

  if (!book) {
    return (
      <EmptyState
        icon={<Layers size={26} />}
        title="Buku tidak ditemukan"
        description="Buku ini mungkin sudah dihapus. Kembali ke pustaka untuk memilih buku lain."
        action={
          <a className="btn btn-primary" href="#/pustaka">
            <ArrowLeft size={16} />
            Ke pustaka
          </a>
        }
      />
    );
  }

  const docsIn = (volumeId: string | null): Doc[] =>
    (volumeId ? docs.filter((doc) => doc.volumeId === volumeId) : looseDocs).sort(
      (a, b) => a.order - b.order || a.createdAt - b.createdAt,
    );

  const openDoc = (doc: Doc) => {
    actions.updateSettings({ lastDocId: doc.id });
    navigate(`#/tulis/${doc.id}`);
  };

  const addDoc = (volumeId: string | null) => {
    const id = actions.createDoc(book.id, volumeId, "Tanpa judul");
    actions.updateSettings({ lastDocId: id });
    navigate(`#/tulis/${id}`);
  };

  const toggleVolume = (volumeId: string) => {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(volumeId)) next.delete(volumeId);
      else next.add(volumeId);
      return next;
    });
  };

  const saveVolume = (event: React.FormEvent) => {
    event.preventDefault();
    if (!volumeEditor) return;
    const title = volumeEditor.title.trim();
    if (!title) return;
    if (volumeEditor.id) {
      actions.renameVolume(volumeEditor.id, title);
      toast("Nama bagian diperbarui.", "success");
    } else {
      actions.createVolume(book.id, title);
      toast("Bagian baru ditambahkan.", "success");
    }
    setVolumeEditor(null);
  };

  const shiftDoc = (doc: Doc, direction: -1 | 1) => {
    const list = docsIn(doc.volumeId);
    const index = list.findIndex((item) => item.id === doc.id);
    const target = index + direction;
    if (index < 0 || target < 0 || target >= list.length) return;
    const ids = list.map((item) => item.id);
    [ids[index], ids[target]] = [ids[target], ids[index]];
    actions.placeDocs(ids, book.id, doc.volumeId);
  };

  const shiftVolume = (volume: Volume, direction: -1 | 1) => {
    const index = volumes.findIndex((item) => item.id === volume.id);
    const target = index + direction;
    if (index < 0 || target < 0 || target >= volumes.length) return;
    const ids = volumes.map((item) => item.id);
    [ids[index], ids[target]] = [ids[target], ids[index]];
    actions.reorderVolumes(book.id, ids);
  };

  const handleDocDrop = (target: Doc) => {
    setDropDoc(null);
    if (!draggingDoc || draggingDoc === target.id) {
      setDraggingDoc(null);
      return;
    }
    const source = docs.find((doc) => doc.id === draggingDoc);
    if (!source) {
      setDraggingDoc(null);
      return;
    }
    const targetVolume = target.volumeId;
    const list = docsIn(targetVolume).filter((doc) => doc.id !== source.id);
    const index = list.findIndex((doc) => doc.id === target.id);
    const ids = list.map((doc) => doc.id);
    ids.splice(index < 0 ? ids.length : index, 0, source.id);
    actions.placeDocs(ids, book.id, targetVolume);
    setDraggingDoc(null);
    toast("Urutan naskah diperbarui.", "success");
  };

  const handleSectionDrop = (volumeId: string | null) => {
    if (!draggingDoc) return;
    const source = docs.find((doc) => doc.id === draggingDoc);
    setDraggingDoc(null);
    setDropDoc(null);
    if (!source || source.volumeId === volumeId) return;
    actions.moveDoc(source.id, book.id, volumeId);
    toast("Naskah dipindahkan.", "success");
  };

  const handleVolumeDrop = (event: DragEvent, target: Volume) => {
    event.preventDefault();
    if (!draggingVolume || draggingVolume === target.id) {
      setDraggingVolume(null);
      return;
    }
    const ids = volumes.map((volume) => volume.id).filter((id) => id !== draggingVolume);
    const index = ids.indexOf(target.id);
    ids.splice(index < 0 ? ids.length : index, 0, draggingVolume);
    actions.reorderVolumes(book.id, ids);
    setDraggingVolume(null);
    toast("Urutan bagian diperbarui.", "success");
  };

  const renderDoc = (doc: Doc) => (
    <div
      key={doc.id}
      className={`doc-row${draggingDoc === doc.id ? " is-dragging" : ""}${
        dropDoc === doc.id ? " is-drop-target" : ""
      }`}
      draggable
      onDragStart={(event) => {
        event.dataTransfer.setData("text/plain", doc.id);
        event.dataTransfer.effectAllowed = "move";
        setDraggingDoc(doc.id);
      }}
      onDragEnd={() => {
        setDraggingDoc(null);
        setDropDoc(null);
      }}
      onDragOver={(event) => {
        if (!draggingDoc) return;
        event.preventDefault();
        event.stopPropagation();
        setDropDoc(doc.id);
      }}
      onDragLeave={() => setDropDoc((current) => (current === doc.id ? null : current))}
      onDrop={(event) => {
        event.preventDefault();
        event.stopPropagation();
        handleDocDrop(doc);
      }}
    >
      <span className="drag-handle" aria-hidden="true" title="Tarik untuk mengubah urutan">
        <GripVertical size={15} />
      </span>
      <button type="button" className="doc-main" onClick={() => openDoc(doc)}>
        <span className="doc-title">{doc.title || "Tanpa judul"}</span>
        <span className="doc-excerpt">{excerpt(doc.body, 96) || "Belum ada isi. Buka dan mulai menulis."}</span>
        <span className="doc-meta">
          {formatNumber(countWords(doc.body))} kata · diperbarui {timeAgo(doc.updatedAt)}
        </span>
      </button>
      <Menu label={`Aksi untuk ${doc.title || "naskah"}`} className="doc-menu">
        {(close) => (
          <>
            <MenuLabel>Naskah</MenuLabel>
            <MenuItem
              icon={<Pencil size={15} />}
              onClick={() => {
                close();
                openDoc(doc);
              }}
            >
              Buka dan sunting
            </MenuItem>
            <MenuItem
              icon={<Copy size={15} />}
              onClick={() => {
                close();
                const id = actions.duplicateDoc(doc.id);
                if (id) toast("Salinan dibuat.", "success");
              }}
            >
              Duplikat
            </MenuItem>
            <MenuItem
              icon={<Move size={15} />}
              onClick={() => {
                close();
                setMoveDoc(doc);
              }}
            >
              Pindahkan ke…
            </MenuItem>
            <MenuSeparator />
            <MenuItem
              icon={<ArrowUp size={15} />}
              onClick={() => {
                close();
                shiftDoc(doc, -1);
              }}
              disabled={docsIn(doc.volumeId)[0]?.id === doc.id}
            >
              Naikkan
            </MenuItem>
            <MenuItem
              icon={<ArrowDown size={15} />}
              onClick={() => {
                close();
                shiftDoc(doc, 1);
              }}
              disabled={docsIn(doc.volumeId).at(-1)?.id === doc.id}
            >
              Turunkan
            </MenuItem>
            <MenuSeparator />
            <MenuItem
              icon={<Trash2 size={15} />}
              danger
              onClick={() => {
                close();
                actions.deleteDoc(doc.id);
                toast("Naskah dipindahkan ke sampah. Masih bisa dipulihkan.", "info");
              }}
            >
              Pindahkan ke sampah
            </MenuItem>
          </>
        )}
      </Menu>
    </div>
  );

  return (
    <div className="book-screen">
      <a className="back-link" href="#/pustaka">
        <ArrowLeft size={15} />
        Pustaka
      </a>

      <header className="book-hero">
        <BookCover title={book.title} color={book.color} />
        <div className="book-hero-info">
          <h1>{book.title}</h1>
          <p className="book-hero-author">{book.author || "Tanpa penulis"}</p>
          <p className="book-hero-meta">
            {docs.length} naskah
            {volumes.length > 0 ? ` · ${volumes.length} bagian` : ""} · {formatNumber(totalWords)} kata
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
        </div>
        <div className="book-hero-actions">
          <button type="button" className="btn btn-primary" onClick={() => addDoc(null)}>
            <Plus size={16} />
            Bab baru
          </button>
          <button
            type="button"
            className="btn"
            onClick={() => setVolumeEditor({ id: null, title: "" })}
          >
            <FolderPlus size={16} />
            Bagian
          </button>
          <Menu label={`Aksi untuk buku ${book.title}`}>
            {(close) => (
              <>
                <MenuItem
                  icon={<Plus size={15} />}
                  onClick={() => {
                    close();
                    addDoc(null);
                  }}
                >
                  Bab baru
                </MenuItem>
                <MenuItem
                  icon={<FolderPlus size={15} />}
                  onClick={() => {
                    close();
                    setVolumeEditor({ id: null, title: "" });
                  }}
                >
                  Bagian baru
                </MenuItem>
                <MenuItem
                  icon={<Pencil size={15} />}
                  onClick={() => {
                    close();
                    setEditingBook(true);
                  }}
                >
                  Ubah detail buku
                </MenuItem>
                <MenuSeparator />
                <MenuItem
                  icon={<Trash2 size={15} />}
                  danger
                  onClick={() => {
                    close();
                    setDeletingBook(true);
                  }}
                >
                  Hapus buku
                </MenuItem>
              </>
            )}
          </Menu>
        </div>
      </header>

      {docs.length === 0 && volumes.length === 0 ? (
        <EmptyState
          icon={<Plus size={26} />}
          title="Buku ini masih kosong"
          description="Tambahkan bab pertama, atau buat bagian dulu kalau ini naskah panjang."
          action={
            <div className="button-row">
              <button type="button" className="btn btn-primary" onClick={() => addDoc(null)}>
                <Plus size={16} />
                Tulis bab pertama
              </button>
              <button
                type="button"
                className="btn"
                onClick={() => setVolumeEditor({ id: null, title: "" })}
              >
                <FolderPlus size={16} />
                Buat bagian
              </button>
            </div>
          }
        />
      ) : null}

      <div className="book-sections">
        {looseDocs.length > 0 || (volumes.length > 0 && docs.length > 0) ? (
          looseDocs.length > 0 ? (
            <section
              className="volume volume--loose"
              onDragOver={(event) => {
                if (!draggingDoc) return;
                event.preventDefault();
              }}
              onDrop={(event) => {
                event.preventDefault();
                handleSectionDrop(null);
              }}
            >
              <header className="volume-head">
                <span className="volume-icon" aria-hidden="true">
                  <Layers size={15} />
                </span>
                <h2 className="volume-title">Tanpa bagian</h2>
                <span className="volume-count">{looseDocs.length}</span>
                <div className="volume-actions">
                  <button
                    type="button"
                    className="icon-btn"
                    onClick={() => addDoc(null)}
                    aria-label="Tambah bab tanpa bagian"
                    title="Tambah bab"
                  >
                    <Plus size={16} />
                  </button>
                </div>
              </header>
              <div className="volume-body">{looseDocs.map(renderDoc)}</div>
            </section>
          ) : null
        ) : null}

        {volumes.map((volume) => {
          const isCollapsed = collapsed.has(volume.id);
          const items = docsIn(volume.id);
          return (
            <section
              className="volume"
              key={volume.id}
              onDragOver={(event) => {
                if (!draggingDoc) return;
                event.preventDefault();
              }}
              onDrop={(event) => {
                event.preventDefault();
                handleSectionDrop(volume.id);
              }}
            >
              <header
                className="volume-head"
                draggable
                onDragStart={(event) => {
                  event.dataTransfer.setData("text/plain", volume.id);
                  event.dataTransfer.effectAllowed = "move";
                  setDraggingVolume(volume.id);
                }}
                onDragEnd={() => setDraggingVolume(null)}
                onDragOver={(event) => {
                  if (draggingVolume) event.preventDefault();
                }}
                onDrop={(event) => handleVolumeDrop(event, volume)}
              >
                <span className="drag-handle volume-grip" aria-hidden="true" title="Tarik untuk mengurutkan bagian">
                  <GripVertical size={14} />
                </span>
                <button
                  type="button"
                  className="volume-toggle"
                  onClick={() => toggleVolume(volume.id)}
                  aria-expanded={!isCollapsed}
                >
                  {isCollapsed ? <ChevronRight size={16} /> : <ChevronDown size={16} />}
                  <h2 className="volume-title">{volume.title}</h2>
                  <span className="volume-count">{items.length}</span>
                </button>
                <div className="volume-actions">
                  <button
                    type="button"
                    className="icon-btn"
                    onClick={() => addDoc(volume.id)}
                    aria-label={`Tambah bab di ${volume.title}`}
                    title="Tambah bab"
                  >
                    <Plus size={16} />
                  </button>
                  <Menu label={`Aksi untuk bagian ${volume.title}`}>
                    {(close) => (
                      <>
                        <MenuItem
                          icon={<Plus size={15} />}
                          onClick={() => {
                            close();
                            addDoc(volume.id);
                          }}
                        >
                          Bab baru di bagian ini
                        </MenuItem>
                        <MenuItem
                          icon={<Pencil size={15} />}
                          onClick={() => {
                            close();
                            setVolumeEditor({ id: volume.id, title: volume.title });
                          }}
                        >
                          Ubah nama bagian
                        </MenuItem>
                        <MenuItem
                          icon={<ArrowUp size={15} />}
                          onClick={() => {
                            close();
                            shiftVolume(volume, -1);
                          }}
                          disabled={volumes[0]?.id === volume.id}
                        >
                          Naikkan bagian
                        </MenuItem>
                        <MenuItem
                          icon={<ArrowDown size={15} />}
                          onClick={() => {
                            close();
                            shiftVolume(volume, 1);
                          }}
                          disabled={volumes.at(-1)?.id === volume.id}
                        >
                          Turunkan bagian
                        </MenuItem>
                        <MenuSeparator />
                        <MenuItem
                          icon={<Trash2 size={15} />}
                          danger
                          onClick={() => {
                            close();
                            setDeletingVolume(volume);
                          }}
                        >
                          Hapus bagian
                        </MenuItem>
                      </>
                    )}
                  </Menu>
                </div>
              </header>
              {isCollapsed ? null : (
                <div className="volume-body">
                  {items.length > 0 ? (
                    items.map(renderDoc)
                  ) : (
                    <button type="button" className="volume-empty" onClick={() => addDoc(volume.id)}>
                      <Plus size={15} />
                      Bagian ini masih kosong. Tambahkan bab.
                    </button>
                  )}
                </div>
              )}
            </section>
          );
        })}
      </div>

      {docs.length > 0 ? (
        <p className="book-footnote">
          Tarik kartu bab atau bagian untuk mengubah urutan. Tautan judul, urutan, dan riwayat tetap
          tersimpan saat kamu memindahkan naskah.
        </p>
      ) : null}

      <BookDialog
        open={editingBook}
        book={book}
        onClose={() => setEditingBook(false)}
        onSubmit={(values: BookValues) => {
          actions.updateBook(book.id, values);
          toast("Detail buku diperbarui.", "success");
        }}
      />

      <Modal
        open={volumeEditor !== null}
        onClose={() => setVolumeEditor(null)}
        title={volumeEditor?.id ? "Ubah nama bagian" : "Bagian baru"}
        description="Bagian membantu mengelompokkan bab dalam naskah panjang. Bisa diurutkan sesuka hati."
        footer={
          <>
            <button type="button" className="btn" onClick={() => setVolumeEditor(null)}>
              Batal
            </button>
            <button type="submit" form="volume-form" className="btn btn-primary">
              {volumeEditor?.id ? "Simpan" : "Buat bagian"}
            </button>
          </>
        }
      >
        <form id="volume-form" className="form" onSubmit={saveVolume}>
          <label className="field">
            <span className="label">Nama bagian</span>
            <input
              className="input"
              value={volumeEditor?.title ?? ""}
              onChange={(event) =>
                setVolumeEditor((current) =>
                  current ? { ...current, title: event.target.value } : current,
                )
              }
              placeholder="Misalnya: Bagian I — Peron"
              maxLength={80}
              autoFocus
              required
            />
          </label>
        </form>
      </Modal>

      <ConfirmDialog
        open={deletingVolume !== null}
        title={`Hapus bagian “${deletingVolume?.title ?? ""}”?`}
        message="Bab di dalamnya tidak akan dihapus. Mereka akan dipindahkan ke “Tanpa bagian”."
        confirmLabel="Hapus bagian"
        onClose={() => setDeletingVolume(null)}
        onConfirm={() => {
          if (!deletingVolume) return;
          actions.deleteVolume(deletingVolume.id);
          setDeletingVolume(null);
          toast("Bagian dihapus, bab dipindahkan ke Tanpa bagian.", "info");
        }}
      />

      <ConfirmDialog
        open={deletingBook}
        title={`Hapus “${book.title}”?`}
        message={`Seluruh ${docs.length} naskah dan riwayatnya di buku ini akan dihapus permanen. Ekspor cadangan dulu kalau masih ragu.`}
        confirmLabel="Hapus buku"
        onClose={() => setDeletingBook(false)}
        onConfirm={() => {
          actions.deleteBook(book.id);
          toast("Buku dihapus.", "danger");
          navigate("#/pustaka");
        }}
      />

      <MoveDocDialog doc={moveDoc} open={moveDoc !== null} onClose={() => setMoveDoc(null)} />
    </div>
  );
}
