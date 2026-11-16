import { useRef, useState, useEffect, type ChangeEvent } from "react";
import {
  Check,
  Database,
  Download,
  HardDriveDownload,
  Infinity as InfinityIcon,
  Keyboard,
  ShieldCheck,
  Trash2,
  Upload,
  X,
} from "lucide-react";
import { useStore } from "../lib/store";
import { PageHeader } from "../components/PageHeader";
import { ConfirmDialog } from "../components/Modal";
import { downloadFile, dateStamp } from "../lib/text";
import { exportLibrary } from "../lib/export";
import { navigate } from "../lib/router";
import { toast } from "../lib/toast";
import type { FontId, ThemeId } from "../lib/types";

const THEME_OPTIONS: { id: ThemeId; label: string; hint: string }[] = [
  { id: "paper", label: "Kertas", hint: "Krem hangat" },
  { id: "sepia", label: "Sepia", hint: "Cokelat tua" },
  { id: "night", label: "Malam", hint: "Hijau gelap" },
  { id: "graphite", label: "Grafit", hint: "Abu netral" },
];

const FONT_OPTIONS: { id: FontId; label: string; hint: string; sample: string }[] = [
  { id: "serif", label: "Serif", hint: "Newsreader", sample: "Aa" },
  { id: "sans", label: "Sans", hint: "Instrument Sans", sample: "Aa" },
  { id: "mono", label: "Mono", hint: "Mesin tik", sample: "Aa" },
];

export function SettingsScreen() {
  const { data, actions } = useStore();
  const settings = data.settings;
  const fileRef = useRef<HTMLInputElement>(null);
  const [pendingImport, setPendingImport] = useState<{ name: string; raw: string } | null>(null);
  const [importError, setImportError] = useState<string | null>(null);
  const [resetOpen, setResetOpen] = useState(false);
  const [newPhrase, setNewPhrase] = useState("");
  const [goalDraft, setGoalDraft] = useState(String(settings.dailyGoal));

  useEffect(() => {
    setGoalDraft(String(settings.dailyGoal));
  }, [settings.dailyGoal]);

  const exportBackup = () => {
    const payload = {
      app: "tinta",
      exportedAt: new Date().toISOString(),
      ...data,
    };
    downloadFile(
      `tinta-cadangan-${dateStamp()}.json`,
      JSON.stringify(payload, null, 2),
      "application/json",
    );
    toast("Cadangan lengkap diekspor.", "success");
  };

  const onFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    setImportError(null);
    try {
      const raw = await file.text();
      setPendingImport({ name: file.name, raw });
    } catch {
      setImportError("Berkas tidak bisa dibaca.");
    }
  };

  const addPhrase = (event: React.FormEvent) => {
    event.preventDefault();
    const value = newPhrase.trim();
    if (!value) return;
    actions.updateSettings({ phrases: [...settings.phrases, value].slice(0, 40) });
    setNewPhrase("");
    toast("Frasa ditambahkan.", "success");
  };

  return (
    <div className="settings-screen">
      <PageHeader
        eyebrow="Kendali penuh"
        title="Setelan"
        subtitle="Atur tampilan menulis, target harian, dan cadangan. Tidak ada opsi yang dikunci di balik pembayaran."
      />

      <section className="unlock-card">
        <div className="unlock-top">
          <span className="unlock-icon" aria-hidden="true">
            <InfinityIcon size={26} />
          </span>
          <div>
            <p className="eyebrow">Tinta versi web</p>
            <h2>Gratis selamanya. Bukan uji coba.</h2>
          </div>
          <span className="unlock-pill">100% terbuka</span>
        </div>
        <p className="unlock-copy">
          Semua yang ada di sini bisa dipakai tanpa akun, langganan, atau kunci Pro. Ekspor,
          riwayat, Markdown, statistik, pencarian lintas buku, sampai gambar panjang — semuanya
          aktif sejak awal.
        </p>
        <ul className="unlock-list">
          <li><Check size={14} /> Tanpa batas jumlah buku dan naskah</li>
          <li><Check size={14} /> Tanpa batas ekspor atau riwayat</li>
          <li><Check size={14} /> Tanpa iklan dan tanpa pelacakan</li>
          <li><Check size={14} /> Tetap bekerja offline setelah dimuat</li>
        </ul>
      </section>

      <div className="settings-grid">
        <section className="settings-section">
          <header className="settings-head">
            <h2>Tampilan</h2>
            <p className="hint">Empat suasana, satu naskah yang sama.</p>
          </header>
          <div className="theme-options" role="group" aria-label="Pilih tema">
            {THEME_OPTIONS.map((theme) => (
              <button
                key={theme.id}
                type="button"
                className={`theme-option${settings.theme === theme.id ? " is-active" : ""}`}
                onClick={() => actions.updateSettings({ theme: theme.id })}
                aria-pressed={settings.theme === theme.id}
              >
                <span className={`theme-swatch theme-swatch--${theme.id}`} aria-hidden="true">
                  <span className="theme-swatch-line" />
                  <span className="theme-swatch-line short" />
                  <span className="theme-swatch-accent" />
                  <span className="theme-swatch-line" />
                </span>
                <span className="theme-option-label">{theme.label}</span>
                <span className="theme-option-hint">{theme.hint}</span>
              </button>
            ))}
          </div>

          <div className="setting-block">
            <div className="setting-copy">
              <span className="setting-title">Jenis huruf</span>
              <span className="setting-desc">Huruf isi naskah. Semua pilihan sudah terpasang.</span>
            </div>
            <div className="font-options" role="group" aria-label="Jenis huruf">
              {FONT_OPTIONS.map((font) => (
                <button
                  key={font.id}
                  type="button"
                  className={`font-option font-option--${font.id}${
                    settings.font === font.id ? " is-active" : ""
                  }`}
                  onClick={() => actions.updateSettings({ font: font.id })}
                  aria-pressed={settings.font === font.id}
                >
                  <span className="font-sample">{font.sample}</span>
                  <span>{font.label}</span>
                  <small>{font.hint}</small>
                </button>
              ))}
            </div>
          </div>
        </section>

        <section className="settings-section">
          <header className="settings-head">
            <h2>Tipografi</h2>
            <p className="hint">Perubahan langsung terasa di editor. Semua bisa diatur bebas.</p>
          </header>
          <div className="setting-row">
            <div className="setting-copy">
              <span className="setting-title">Ukuran huruf</span>
              <span className="setting-desc">Besar kecil teks naskah.</span>
            </div>
            <div className="range-control">
              <input
                type="range"
                min={14}
                max={26}
                step={1}
                value={settings.fontSize}
                onChange={(event) => actions.updateSettings({ fontSize: Number(event.target.value) })}
                aria-label="Ukuran huruf naskah"
              />
              <output>{settings.fontSize}px</output>
            </div>
          </div>
          <div className="setting-row">
            <div className="setting-copy">
              <span className="setting-title">Jarak antarbaris</span>
              <span className="setting-desc">Ruang napas di setiap baris.</span>
            </div>
            <div className="range-control">
              <input
                type="range"
                min={1.3}
                max={2.3}
                step={0.05}
                value={settings.lineHeight}
                onChange={(event) => actions.updateSettings({ lineHeight: Number(event.target.value) })}
                aria-label="Jarak antarbaris"
              />
              <output>{settings.lineHeight.toFixed(2).replace(".", ",")}</output>
            </div>
          </div>
          <div className="setting-row">
            <div className="setting-copy">
              <span className="setting-title">Jarak antarparagraf</span>
              <span className="setting-desc">Untuk pemisah yang terasa, bukan mencolok.</span>
            </div>
            <div className="range-control">
              <input
                type="range"
                min={0}
                max={28}
                step={2}
                value={settings.paragraphSpacing}
                onChange={(event) =>
                  actions.updateSettings({ paragraphSpacing: Number(event.target.value) })
                }
                aria-label="Jarak antarparagraf"
              />
              <output>{settings.paragraphSpacing}px</output>
            </div>
          </div>
          <div className="setting-row">
            <div className="setting-copy">
              <span className="setting-title">Lebar halaman</span>
              <span className="setting-desc">Antara baris panjang dan kolom yang nyaman.</span>
            </div>
            <div className="range-control">
              <input
                type="range"
                min={540}
                max={920}
                step={20}
                value={settings.pageWidth}
                onChange={(event) => actions.updateSettings({ pageWidth: Number(event.target.value) })}
                aria-label="Lebar halaman"
              />
              <output>{settings.pageWidth}px</output>
            </div>
          </div>
        </section>

        <section className="settings-section">
          <header className="settings-head">
            <h2>Menulis</h2>
            <p className="hint">Kebiasaan kecil yang menjaga fokus.</p>
          </header>
          <div className="setting-row setting-row--toggle">
            <div className="setting-copy">
              <span className="setting-title">Mode ketik</span>
              <span className="setting-desc">Baris yang sedang ditulis tetap di tengah layar.</span>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={settings.typewriter}
              aria-label="Mode ketik"
              className={`switch${settings.typewriter ? " is-on" : ""}`}
              onClick={() => actions.updateSettings({ typewriter: !settings.typewriter })}
            >
              <span className="switch-thumb" />
            </button>
          </div>
          <div className="setting-row setting-row--toggle">
            <div className="setting-copy">
              <span className="setting-title">Garis bantu naskah</span>
              <span className="setting-desc">Garis tipis seperti kertas folio di belakang teks.</span>
            </div>
            <button
              type="button"
              role="switch"
              aria-checked={settings.rulings}
              aria-label="Garis bantu naskah"
              className={`switch${settings.rulings ? " is-on" : ""}`}
              onClick={() => actions.updateSettings({ rulings: !settings.rulings })}
            >
              <span className="switch-thumb" />
            </button>
          </div>
          <div className="setting-row">
            <div className="setting-copy">
              <span className="setting-title">Target harian</span>
              <span className="setting-desc">Dipakai untuk progres dan warna peta panas.</span>
            </div>
            <div className="goal-input">
              <input
                type="number"
                min={50}
                max={10000}
                step={50}
                value={goalDraft}
                onChange={(event) => setGoalDraft(event.target.value)}
                onBlur={() => {
                  const next = Number(goalDraft);
                  const safe = Number.isFinite(next)
                    ? Math.min(10000, Math.max(50, Math.round(next)))
                    : settings.dailyGoal;
                  actions.updateSettings({ dailyGoal: safe });
                  setGoalDraft(String(safe));
                }}
                aria-label="Target kata harian"
              />
              <span>kata</span>
            </div>
          </div>

          <div className="setting-block">
            <div className="setting-copy">
              <span className="setting-title">Bilah sisipan</span>
              <span className="setting-desc">
                Teks yang sering dipakai: nama tokoh, tanda baca, atau pembuka kalimat.
              </span>
            </div>
            <div className="phrase-settings">
              {settings.phrases.map((phrase, index) => (
                <span className="phrase-chip phrase-chip--editable" key={`${phrase}-${index}`}>
                  {phrase}
                  <button
                    type="button"
                    onClick={() =>
                      actions.updateSettings({
                        phrases: settings.phrases.filter((_, itemIndex) => itemIndex !== index),
                      })
                    }
                    aria-label={`Hapus frasa ${phrase}`}
                  >
                    <X size={12} />
                  </button>
                </span>
              ))}
              <form className="phrase-form" onSubmit={addPhrase}>
                <label className="sr-only" htmlFor="settings-phrase">
                  Frasa baru
                </label>
                <input
                  id="settings-phrase"
                  className="input input--sm"
                  value={newPhrase}
                  onChange={(event) => setNewPhrase(event.target.value)}
                  placeholder="Frasa baru…"
                  maxLength={80}
                />
                <button type="submit" className="btn btn-sm">
                  Tambah
                </button>
              </form>
            </div>
          </div>
        </section>

        <section className="settings-section">
          <header className="settings-head">
            <h2>Data &amp; cadangan</h2>
            <p className="hint">
              Tulisan tinggal di peramban ini. Ekspor cadangan secara berkala, terutama sebelum
              membersihkan data situs.
            </p>
          </header>
          <div className="data-actions">
            <button type="button" className="data-action" onClick={exportBackup}>
              <span className="data-icon"><HardDriveDownload size={18} /></span>
              <span>
                <strong>Ekspor cadangan (.json)</strong>
                <small>Semua buku, naskah, riwayat, statistik, dan setelan.</small>
              </span>
            </button>
            <button type="button" className="data-action" onClick={() => fileRef.current?.click()}>
              <span className="data-icon"><Upload size={18} /></span>
              <span>
                <strong>Impor cadangan</strong>
                <small>Menggantikan seluruh data sekarang dengan isi berkas.</small>
              </span>
            </button>
            <button
              type="button"
              className="data-action"
              onClick={() => {
                exportLibrary(data.books, data.volumes, data.docs);
                toast("Seluruh pustaka diekspor sebagai Markdown.", "success");
              }}
            >
              <span className="data-icon"><Download size={18} /></span>
              <span>
                <strong>Ekspor pustaka (.md)</strong>
                <small>Semua naskah dalam satu berkas, lengkap dengan judul buku.</small>
              </span>
            </button>
            <input
              ref={fileRef}
              type="file"
              accept="application/json,.json"
              className="sr-only"
              onChange={onFile}
            />
          </div>
          {importError ? <p className="form-error">{importError}</p> : null}
          <div className="danger-zone">
            <div className="setting-copy">
              <span className="setting-title">Hapus semua data</span>
              <span className="setting-desc">
                Menghapus seluruh buku dan naskah dari peramban ini. Setelan tampilan tetap.
              </span>
            </div>
            <button type="button" className="btn btn-danger-ghost" onClick={() => setResetOpen(true)}>
              <Trash2 size={15} />
              Hapus semua
            </button>
          </div>
        </section>

        <section className="settings-section settings-section--about">
          <header className="settings-head">
            <h2>Tentang Tinta</h2>
            <p className="hint">Versi 1.0.0 · dibuat untuk menulis, bukan untuk berlangganan.</p>
          </header>
          <div className="about-grid">
            <div className="about-item">
              <ShieldCheck size={18} />
              <div>
                <strong>Local-first</strong>
                <p>Data disimpan di penyimpanan peramban. Tidak ada server, akun, atau sinkronisasi diam-diam.</p>
              </div>
            </div>
            <div className="about-item">
              <Database size={18} />
              <div>
                <strong>Tanpa kunci</strong>
                <p>Tidak ada tombol “beli”, “coba Pro”, atau batas ekspor. Semua fitur aktif dari awal.</p>
              </div>
            </div>
            <div className="about-item">
              <Keyboard size={18} />
              <div>
                <strong>Pintasan</strong>
                <p>Ctrl/⌘B tebal, Ctrl/⌘I miring, Ctrl/⌘S simpan versi, Tab menjorok, Esc keluar mode fokus.</p>
              </div>
            </div>
          </div>
          <p className="about-note">
            Dibangun ulang secara clean-room dari fitur publik aplikasi menulis. Tidak memuat kode,
            aset, logo, atau teks milik aplikasi lain. Nama, warna, ikon, dan seluruh tulisan di sini
            dibuat untuk Tinta.
          </p>
        </section>
      </div>

      <ConfirmDialog
        open={pendingImport !== null}
        title="Impor cadangan?"
        message={`Berkas “${pendingImport?.name ?? ""}” akan menggantikan seluruh data Tinta yang sekarang. Ekspor cadangan dulu kalau belum yakin.`}
        confirmLabel="Impor dan ganti"
        onClose={() => setPendingImport(null)}
        onConfirm={() => {
          if (!pendingImport) return;
          const result = actions.importData(pendingImport.raw);
          if (!result.ok) {
            setImportError(result.error);
            toast(result.error, "danger");
            setPendingImport(null);
            return;
          }
          setPendingImport(null);
          toast("Cadangan dipulihkan.", "success");
          navigate("#/pustaka");
        }}
      />

      <ConfirmDialog
        open={resetOpen}
        title="Hapus semua data?"
        message="Semua buku, naskah, riwayat, dan statistik di peramban ini akan hilang. Tindakan ini tidak bisa dibatalkan. Ekspor cadangan lebih dulu kalau masih ada yang ingin disimpan."
        confirmLabel="Hapus semuanya"
        onClose={() => setResetOpen(false)}
        onConfirm={() => {
          actions.resetData();
          toast("Semua data dihapus.", "danger");
          navigate("#/pustaka");
        }}
      />
    </div>
  );
}
