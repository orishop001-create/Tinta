import { useMemo, useState } from "react";
import { FileText, RotateCcw, Trash2 } from "lucide-react";
import { useStore } from "../lib/store";
import { countWords, formatDateTime, formatNumber, timeAgo } from "../lib/text";
import type { Doc } from "../lib/types";
import { PageHeader } from "../components/PageHeader";
import { EmptyState } from "../components/EmptyState";
import { ConfirmDialog } from "../components/Modal";
import { toast } from "../lib/toast";

export function TrashScreen() {
  const { data, actions } = useStore();
  const [purgeTarget, setPurgeTarget] = useState<Doc | null>(null);
  const [emptyOpen, setEmptyOpen] = useState(false);

  const trashed = useMemo(
    () =>
      data.docs
        .filter((doc) => doc.deletedAt !== null)
        .sort((a, b) => (b.deletedAt ?? 0) - (a.deletedAt ?? 0)),
    [data.docs],
  );

  return (
    <div className="trash-screen">
      <PageHeader
        eyebrow="Keamanan"
        title="Sampah"
        subtitle="Naskah yang kamu hapus tidak langsung lenyap. Pulihkan kapan saja, atau bersihkan saat sudah yakin."
        actions={
          trashed.length > 0 ? (
            <button type="button" className="btn btn-danger-ghost" onClick={() => setEmptyOpen(true)}>
              <Trash2 size={16} />
              Kosongkan sampah
            </button>
          ) : undefined
        }
      />

      {trashed.length > 0 ? (
        <div className="trash-note">
          <FileText size={15} aria-hidden="true" />
          <span>
            {trashed.length} naskah di sampah. Tidak ada penghapusan otomatis — selama tidak
            dikosongkan, semuanya tetap di sini.
          </span>
        </div>
      ) : null}

      {trashed.length === 0 ? (
        <EmptyState
          icon={<Trash2 size={26} />}
          title="Sampah kosong"
          description="Kalau kamu menghapus naskah, ia akan muncul di sini dulu. Tidak ada yang hilang tanpa kesempatan kedua."
          action={
            <a className="btn btn-primary" href="#/pustaka">
              Ke pustaka
            </a>
          }
        />
      ) : (
        <ul className="trash-list">
          {trashed.map((doc) => {
            const book = data.books.find((item) => item.id === doc.bookId);
            return (
              <li className="trash-row" key={doc.id}>
                <span className="trash-icon" aria-hidden="true">
                  <FileText size={18} />
                </span>
                <div className="trash-main">
                  <h2>{doc.title || "Tanpa judul"}</h2>
                  <p>
                    {book?.title ?? "Tanpa buku"} · {formatNumber(countWords(doc.body))} kata ·{" "}
                    dihapus {doc.deletedAt ? timeAgo(doc.deletedAt) : "baru saja"}
                  </p>
                  <p className="hint">{doc.deletedAt ? formatDateTime(doc.deletedAt) : ""}</p>
                </div>
                <div className="trash-actions">
                  <button
                    type="button"
                    className="btn btn-sm"
                    onClick={() => {
                      actions.restoreDoc(doc.id);
                      toast("Naskah dikembalikan ke bukunya.", "success");
                    }}
                  >
                    <RotateCcw size={14} />
                    Pulihkan
                  </button>
                  <button
                    type="button"
                    className="btn btn-sm btn-danger-ghost"
                    onClick={() => setPurgeTarget(doc)}
                  >
                    Hapus permanen
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      <ConfirmDialog
        open={purgeTarget !== null}
        title={`Hapus permanen “${purgeTarget?.title ?? ""}”?`}
        message="Naskah dan seluruh riwayat versinya akan dihapus dari peramban ini. Tindakan ini tidak bisa dibatalkan."
        confirmLabel="Hapus permanen"
        onClose={() => setPurgeTarget(null)}
        onConfirm={() => {
          if (!purgeTarget) return;
          actions.purgeDoc(purgeTarget.id);
          setPurgeTarget(null);
          toast("Naskah dihapus permanen.", "danger");
        }}
      />

      <ConfirmDialog
        open={emptyOpen}
        title="Kosongkan seluruh sampah?"
        message={`${trashed.length} naskah beserta riwayatnya akan dihapus permanen. Naskah yang masih di buku tidak terpengaruh.`}
        confirmLabel="Kosongkan sampah"
        onClose={() => setEmptyOpen(false)}
        onConfirm={() => {
          actions.emptyTrash();
          toast("Sampah dikosongkan.", "danger");
        }}
      />
    </div>
  );
}
