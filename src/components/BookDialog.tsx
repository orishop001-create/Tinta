import { useEffect, useState } from "react";
import type { Book } from "../lib/types";
import { COVERS, coverClass } from "./Cover";
import { Modal } from "./Modal";

export interface BookValues {
  title: string;
  author: string;
  tags: string[];
  color: string;
}

interface BookDialogProps {
  open: boolean;
  book: Book | null;
  onClose: () => void;
  onSubmit: (values: BookValues) => void;
}

export function BookDialog({ open, book, onClose, onSubmit }: BookDialogProps) {
  const [title, setTitle] = useState("");
  const [author, setAuthor] = useState("");
  const [tags, setTags] = useState("");
  const [color, setColor] = useState("clay");

  useEffect(() => {
    if (!open) return;
    setTitle(book?.title ?? "");
    setAuthor(book?.author ?? "");
    setTags(book?.tags.join(", ") ?? "");
    setColor(book?.color ?? COVERS[0].id);
  }, [open, book]);

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
    if (!title.trim()) return;
    onSubmit({
      title: title.trim(),
      author: author.trim(),
      tags: tags
        .split(",")
        .map((tag) => tag.trim())
        .filter(Boolean)
        .slice(0, 6),
      color,
    });
    onClose();
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={book ? "Ubah buku" : "Buku baru"}
      description={book ? "Perbarui identitas buku tanpa menyentuh isinya." : "Beri nama bukunya dulu. Semua bisa diubah nanti."}
      footer={
        <>
          <button type="button" className="btn" onClick={onClose}>
            Batal
          </button>
          <button type="submit" form="book-form" className="btn btn-primary">
            {book ? "Simpan perubahan" : "Buat buku"}
          </button>
        </>
      }
    >
      <form id="book-form" className="form" onSubmit={submit}>
        <label className="field">
          <span className="label">Judul buku</span>
          <input
            className="input"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="Misalnya: Catatan Harian"
            maxLength={80}
            autoFocus
            required
          />
        </label>
        <label className="field">
          <span className="label">Penulis</span>
          <input
            className="input"
            value={author}
            onChange={(event) => setAuthor(event.target.value)}
            placeholder="Nama pena atau namamu"
            maxLength={60}
          />
        </label>
        <label className="field">
          <span className="label">Label</span>
          <input
            className="input"
            value={tags}
            onChange={(event) => setTags(event.target.value)}
            placeholder="fiksi, jurnal, draf"
            maxLength={120}
          />
          <span className="hint">Pisahkan dengan koma. Maksimal enam label.</span>
        </label>
        <fieldset className="field">
          <legend className="label">Warna sampul</legend>
          <div className="cover-options">
            {COVERS.map((cover) => (
              <button
                key={cover.id}
                type="button"
                className={`cover-option ${coverClass(cover.id)}${color === cover.id ? " is-active" : ""}`}
                onClick={() => setColor(cover.id)}
                aria-pressed={color === cover.id}
                aria-label={`Sampul ${cover.label}`}
                title={cover.label}
              >
                <span className="cover-option-sheen" />
              </button>
            ))}
          </div>
        </fieldset>
      </form>
    </Modal>
  );
}
