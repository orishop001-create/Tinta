import type { AppData, Book, Doc, Volume } from "./types";
import { countWords, dayKey } from "./text";
import { DEFAULT_SETTINGS, SCHEMA_VERSION } from "./defaults";

const DAY = 86_400_000;

export function seedData(): AppData {
  const now = Date.now();
  const at = (daysAgo: number) => now - daysAgo * DAY;

  const books: Book[] = [
    {
      id: "b-catatan",
      title: "Catatan Kecil",
      author: "Kamu",
      color: "clay",
      tags: ["jurnal"],
      createdAt: at(9),
      updatedAt: at(1),
    },
    {
      id: "b-hujan",
      title: "Hujan di Kota Tua",
      author: "Kamu",
      color: "ink",
      tags: ["fiksi", "draf"],
      createdAt: at(6),
      updatedAt: at(1),
    },
  ];

  const volumes: Volume[] = [
    { id: "v-satu", bookId: "b-hujan", title: "Bagian I — Peron", order: 0, createdAt: at(6) },
    { id: "v-dua", bookId: "b-hujan", title: "Bagian II — Surat", order: 1, createdAt: at(3) },
  ];

  const docs: Doc[] = [
    {
      id: "d-selamat",
      bookId: "b-catatan",
      volumeId: null,
      title: "Mulai dari sini",
      markdown: true,
      order: 0,
      createdAt: at(9),
      updatedAt: at(1),
      deletedAt: null,
      body: `Selamat datang di tinta.

Ini ruang menulis yang menganggap tulisanmu lebih penting daripada aplikasinya. Tidak ada tombol simpan, karena menyimpan seharusnya tidak perlu kamu ingat.

**Tiga hal yang perlu kamu tahu:**

1. Semua yang kamu tulis tersimpan otomatis di peramban ini.
2. Setiap perubahan punya riwayat. Buka menu ⋯ → **Riwayat** untuk melihat dan memulihkan versi lama.
3. Semua fitur terbuka. Tidak ada akun, iklan, atau versi Pro.

Coba sekarang: hapus teks ini, lalu mulai dengan kalimatmu sendiri. Atau biarkan catatan ini sebagai pengingat bahwa halaman kosong tidak harus menakutkan.

**Tips kecil**

- Tekan ⌘/Ctrl + B untuk tebal, ⌘/Ctrl + I untuk miring.
- Nyalakan Markdown lewat tombol di kanan atas kalau kamu suka menulis dengan sintaks.
- Buka **Statistik** untuk melihat hari-hari yang sudah kamu isi.
- Ekspor kapan saja ke TXT, Markdown, HTML, gambar, atau PDF. Gratis.

Selamat menulis.`,
    },
    {
      id: "d-ide",
      bookId: "b-catatan",
      volumeId: null,
      title: "Ide yang belum selesai",
      markdown: false,
      order: 1,
      createdAt: at(4),
      updatedAt: at(4),
      deletedAt: null,
      body: `Beberapa benih yang mungkin tumbuh jadi sesuatu:

- Cerita tentang kota yang lupa cara hujan.
- Esai pendek soal menulis setiap hari, bahkan saat tidak ingin.
- Wawancara fiktif dengan diriku sepuluh tahun lalu.

Pilih satu, beri waktu dua puluh menit, lalu lihat apakah masih menarik besok.`,
    },
    {
      id: "d-bab1",
      bookId: "b-hujan",
      volumeId: "v-satu",
      title: "Bab 1 — Kereta Terlambat",
      markdown: false,
      order: 0,
      createdAt: at(6),
      updatedAt: at(2),
      deletedAt: null,
      body: `Kereta terakhir selalu datang terlambat, dan malam itu tidak berbeda.

Aku berdiri di ujung peron, menghitung tetes air yang jatuh dari atap seng. Suara hujan menutupi pengumuman yang tidak pernah jelas. Di seberang, lampu stasiun memantul di genangan, memanjang seperti jalan yang tidak jadi pergi.

Seorang lelaki tua duduk di bangku kayu, membawa kotak kayu kecil di pangkuannya. Ia tidak menatap rel. Ia menatap jam.

"Masih lama," kataku, sekadar mengisi udara.

Ia mengangguk. "Saya menunggu kereta yang sudah lewat."

Aku tidak bertanya. Di kota ini, orang sering menunggu hal-hal yang sudah pergi. Kenangan punya jadwalnya sendiri, dan kerap lebih tepat daripada kereta.

Ketika kereta akhirnya tiba, pintunya membuka dengan suara mengeluh. Lelaki itu tetap duduk. Aku naik, lalu menoleh dari dalam. Ia masih di sana, kotak kayu di pangkuan, seperti sedang menjaga sesuatu yang tidak boleh basah.

Peron perlahan menghilang di balik jendela. Hujan menulis garis-garis miring di kaca, dan untuk pertama kalinya dalam beberapa minggu, aku merasa tidak perlu ke mana-mana.`,
    },
    {
      id: "d-bab2",
      bookId: "b-hujan",
      volumeId: "v-satu",
      title: "Bab 2 — Payung Pinjaman",
      markdown: false,
      order: 1,
      createdAt: at(5),
      updatedAt: at(1),
      deletedAt: null,
      body: `Payung itu bukan milikku.

Aku menemukannya di kursi kereta, masih basah, gagangnya dihiasi ukiran kecil berbentuk daun. Tidak ada nama. Tidak ada catatan. Hanya payung hitam yang baunya seperti halaman buku lama.

Aku membawanya pulang, berniat menyerahkannya ke pos kehilangan besok pagi. Tetapi besok pagi datang dengan hujan yang lebih rajin, dan payung itu sudah berdiri di dekat pintu, seolah menunggu.

Di dalam lipatannya, terselip selembar kertas kecil. Tulisan tangan yang miring ke kanan: "Kalau kamu menemukan ini, berarti aku benar. Hujan memang memilih orangnya."

Tidak ada tanda tangan. Tidak ada alamat.

Aku membaca kalimat itu tiga kali, lalu menyeduh kopi, lalu menulis balasan yang tidak akan pernah dikirim. Beberapa hal lebih nyaman tinggal sebagai kemungkinan.`,
    },
    {
      id: "d-bab3",
      bookId: "b-hujan",
      volumeId: "v-dua",
      title: "Bab 3 — Surat Tanpa Alamat",
      markdown: false,
      order: 0,
      createdAt: at(2),
      updatedAt: at(1),
      deletedAt: null,
      body: `Kepada yang menemukan payungku,

Aku tidak tahu siapa kamu, dan mungkin itu bagian terbaiknya. Aku menulis surat ini di meja yang sama tempat aku kehilangan keberanian untuk mengirim surat-surat sebelumnya.

Kalau kamu sedang membaca ini, jangan mengembalikan payungnya. Pakai saja. Kota ini punya terlalu banyak hujan untuk orang yang berjalan tanpa perlindungan.

Mungkin suatu hari kita akan bertemu di peron yang sama, sama-sama basah, sama-sama pura-pura tidak mengenali payung itu. Sampai saat itu, anggap surat ini sebagai terima kasih.

— Yang menunggu kereta yang sudah lewat`,
    },
  ];

  const stats: Record<string, number> = {};
  for (const doc of docs) {
    const key = dayKey(doc.createdAt);
    stats[key] = (stats[key] ?? 0) + countWords(doc.body);
  }

  return {
    schema: SCHEMA_VERSION,
    books,
    volumes,
    docs,
    snapshots: [],
    stats,
    settings: { ...DEFAULT_SETTINGS, phrases: [...DEFAULT_SETTINGS.phrases] },
  };
}
