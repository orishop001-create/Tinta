import { useEffect, useMemo, useState } from "react";
import { ArrowLeft, Clock, FileText, RotateCcw, Sparkles } from "lucide-react";
import { useStore } from "../lib/store";
import { navigate } from "../lib/router";
import { countWords, formatDateTime, formatNumber } from "../lib/text";
import { diffLines, diffStats } from "../lib/diff";
import { renderMarkdown } from "../lib/markdown";
import type { SnapshotReason } from "../lib/types";
import { ConfirmDialog } from "../components/Modal";
import { EmptyState } from "../components/EmptyState";
import { toast } from "../lib/toast";

const REASON_LABEL: Record<SnapshotReason, string> = {
  auto: "Otomatis",
  manual: "Disimpan manual",
  "before-restore": "Cadangan sebelum pulihkan",
};

interface HistoryProps {
  docId: string;
}

export function HistoryScreen({ docId }: HistoryProps) {
  const { data, actions } = useStore();
  const doc = data.docs.find((item) => item.id === docId);
  const book = data.books.find((item) => item.id === doc?.bookId);

  const snapshots = useMemo(
    () =>
      data.snapshots
        .filter((snapshot) => snapshot.docId === docId)
        .sort((a, b) => b.createdAt - a.createdAt),
    [data.snapshots, docId],
  );

  const [selectedId, setSelectedId] = useState<string | null>(snapshots[0]?.id ?? null);
  const [tab, setTab] = useState<"diff" | "preview">("diff");
  const [restoreOpen, setRestoreOpen] = useState(false);

  useEffect(() => {
    if (!snapshots.some((snapshot) => snapshot.id === selectedId)) {
      setSelectedId(snapshots[0]?.id ?? null);
    }
  }, [snapshots, selectedId]);

  const selected = snapshots.find((snapshot) => snapshot.id === selectedId) ?? null;
  const diff = useMemo(
    () => (selected && doc ? diffLines(selected.body, doc.body) : []),
    [selected, doc],
  );
  const stats = useMemo(() => diffStats(diff), [diff]);
  const changed = Boolean(
    selected && doc && (selected.body !== doc.body || selected.title !== doc.title),
  );
  const visibleRows = diff.slice(0, 500);

  if (!doc) {
    return (
      <div className="history-screen">
        <EmptyState
          icon={<FileText size={26} />}
          title="Naskah tidak ditemukan"
          description="Riwayat hanya tersedia untuk naskah yang masih ada di pustaka."
          action={
            <a className="btn btn-primary" href="#/pustaka">
              <ArrowLeft size={16} />
              Ke pustaka
            </a>
          }
        />
      </div>
    );
  }

  return (
    <div className="history-screen">
      <header className="editor-top">
        <button
          type="button"
          className="icon-btn"
          onClick={() => navigate(`#/tulis/${doc.id}`)}
          aria-label="Kembali ke editor"
        >
          <ArrowLeft size={19} />
        </button>
        <div className="editor-crumbs">
          <span className="crumb-current">Riwayat</span>
          <span aria-hidden="true">/</span>
          <span>{doc.title || "Tanpa judul"}</span>
        </div>
        <div className="editor-top-actions">
          <button
            type="button"
            className="btn btn-sm"
            onClick={() => {
              actions.snapshot(doc.id, "manual");
              toast("Versi sekarang disimpan.", "success");
            }}
          >
            <Sparkles size={14} />
            <span className="hide-sm">Simpan versi</span>
          </button>
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => navigate(`#/tulis/${doc.id}`)}
          >
            Kembali menulis
          </button>
        </div>
      </header>

      <main className="history-main">
        <div className="history-heading">
          <p className="eyebrow">{book?.title ?? "Pustaka"}</p>
          <h1>Riwayat versi</h1>
          <p className="page-sub">
            Versi otomatis dibuat saat membuka naskah, setiap dua menit saat menulis, dan sebelum
            memulihkan versi lama. Maksimal 40 versi terbaru per naskah.
          </p>
        </div>

        <div className="history-layout">
          <aside className="history-list" aria-label="Daftar versi">
            <div className="history-list-head">
              <span>
                <Clock size={14} aria-hidden="true" /> {snapshots.length} versi
              </span>
            </div>
            {snapshots.length === 0 ? (
              <div className="history-list-empty">
                <p>Belum ada riwayat.</p>
                <p className="hint">Tulis beberapa kalimat, lalu simpan versi manual.</p>
              </div>
            ) : (
              <ul className="history-items">
                {snapshots.map((snapshot) => (
                  <li key={snapshot.id}>
                    <button
                      type="button"
                      className={`history-item${snapshot.id === selectedId ? " is-active" : ""}`}
                      onClick={() => setSelectedId(snapshot.id)}
                      aria-pressed={snapshot.id === selectedId}
                    >
                      <span className="history-item-time">{formatDateTime(snapshot.createdAt)}</span>
                      <span className="history-item-title">{snapshot.title || "Tanpa judul"}</span>
                      <span className="history-item-meta">
                        {formatNumber(countWords(snapshot.body))} kata · {REASON_LABEL[snapshot.reason]}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </aside>

          <section className="history-detail" aria-live="polite">
            {selected ? (
              <>
                <header className="history-detail-head">
                  <div>
                    <h2>{selected.title || "Tanpa judul"}</h2>
                    <p className="hint">
                      {formatDateTime(selected.createdAt)} · {formatNumber(countWords(selected.body))} kata
                    </p>
                  </div>
                  <div className="segmented" role="group" aria-label="Tampilan versi">
                    <button
                      type="button"
                      className={tab === "diff" ? "is-active" : ""}
                      onClick={() => setTab("diff")}
                      aria-pressed={tab === "diff"}
                    >
                      Bandingkan
                    </button>
                    <button
                      type="button"
                      className={tab === "preview" ? "is-active" : ""}
                      onClick={() => setTab("preview")}
                      aria-pressed={tab === "preview"}
                    >
                      Pratinjau
                    </button>
                  </div>
                </header>

                {tab === "diff" ? (
                  <div className="diff-wrap">
                    <p className="diff-summary">
                      <span className="diff-added">+{stats.added}</span>
                      <span className="diff-removed">−{stats.removed}</span>
                      baris dibandingkan dengan versi sekarang.
                    </p>
                    {changed ? (
                      <div className="diff-table" role="table" aria-label="Perbedaan versi">
                        {visibleRows.map((row, index) => (
                          <div
                            key={`${row.kind}-${index}`}
                            className={`diff-row diff-row--${row.kind}`}
                            role="row"
                          >
                            <span className="diff-sign" aria-hidden="true">
                              {row.kind === "add" ? "+" : row.kind === "remove" ? "−" : " "}
                            </span>
                            <span className="diff-text">{row.text || " "}</span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <p className="diff-empty">Versi ini identik dengan isi naskah sekarang.</p>
                    )}
                    {diff.length > visibleRows.length ? (
                      <p className="hint">Menampilkan 500 baris pertama dari {diff.length} baris.</p>
                    ) : null}
                  </div>
                ) : (
                  <div className="history-preview markdown-body">
                    {selected.markdown ? (
                      <div dangerouslySetInnerHTML={{ __html: renderMarkdown(selected.body) }} />
                    ) : (
                      <pre className="history-plain">{selected.body || "(naskah kosong)"}</pre>
                    )}
                  </div>
                )}

                <footer className="history-detail-foot">
                  <p className="hint">
                    Memulihkan versi ini akan menyimpan isi sekarang sebagai cadangan lebih dulu.
                  </p>
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={!changed}
                    onClick={() => setRestoreOpen(true)}
                  >
                    <RotateCcw size={15} />
                    Pulihkan versi ini
                  </button>
                </footer>
              </>
            ) : (
              <EmptyState
                compact
                icon={<Clock size={24} />}
                title="Belum ada yang bisa dipulihkan"
                description="Riwayat dibuat otomatis saat kamu menulis. Versi manual bisa dibuat kapan saja lewat tombol Simpan versi."
              />
            )}
          </section>
        </div>
      </main>

      <ConfirmDialog
        open={restoreOpen}
        title="Pulihkan versi ini?"
        message={
          selected
            ? `Isi naskah sekarang akan disimpan sebagai cadangan, lalu diganti dengan versi ${formatDateTime(
                selected.createdAt,
              )}. Kamu masih bisa kembali lagi lewat Riwayat.`
            : ""
        }
        confirmLabel="Pulihkan"
        danger={false}
        onClose={() => setRestoreOpen(false)}
        onConfirm={() => {
          if (!selected) return;
          actions.restoreSnapshot(selected.id);
          toast("Versi lama dipulihkan. Isi sebelumnya aman di riwayat.", "success");
          navigate(`#/tulis/${doc.id}`);
        }}
      />
    </div>
  );
}
