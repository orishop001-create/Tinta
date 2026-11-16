import { useEffect, useState } from "react";
import type { Doc } from "../lib/types";
import { useStore } from "../lib/store";
import { toast } from "../lib/toast";
import { Modal } from "./Modal";

interface MoveDocDialogProps {
  doc: Doc | null;
  open: boolean;
  onClose: () => void;
}

export function MoveDocDialog({ doc, open, onClose }: MoveDocDialogProps) {
  const { data, actions } = useStore();
  const [bookId, setBookId] = useState("");
  const [volumeId, setVolumeId] = useState("");

  useEffect(() => {
    if (!open || !doc) return;
    setBookId(doc.bookId);
    setVolumeId(doc.volumeId ?? "");
  }, [open, doc]);

  if (!doc) return null;

  const volumes = data.volumes
    .filter((volume) => volume.bookId === bookId)
    .sort((a, b) => a.order - b.order);
  const currentBook = data.books.find((book) => book.id === bookId);

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Pindahkan naskah"
      description={`“${doc.title || "Tanpa judul"}” bisa pindah buku atau bagian tanpa kehilangan riwayatnya.`}
      footer={
        <>
          <button type="button" className="btn" onClick={onClose}>
            Batal
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              actions.moveDoc(doc.id, bookId, volumeId || null);
              onClose();
              toast(`Dipindahkan ke ${currentBook?.title ?? "buku lain"}.`, "success");
            }}
          >
            Pindahkan
          </button>
        </>
      }
    >
      <div className="form">
        <label className="field">
          <span className="label">Buku tujuan</span>
          <select
            className="select"
            value={bookId}
            onChange={(event) => {
              setBookId(event.target.value);
              setVolumeId("");
            }}
          >
            {data.books.map((book) => (
              <option key={book.id} value={book.id}>
                {book.title}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span className="label">Bagian</span>
          <select
            className="select"
            value={volumeId}
            onChange={(event) => setVolumeId(event.target.value)}
          >
            <option value="">Tanpa bagian</option>
            {volumes.map((volume) => (
              <option key={volume.id} value={volume.id}>
                {volume.title}
              </option>
            ))}
          </select>
        </label>
        <p className="hint">Naskah akan diletakkan di urutan terakhir bagian tujuan.</p>
      </div>
    </Modal>
  );
}
