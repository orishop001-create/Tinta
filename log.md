# Log perubahan Tinta

Catatan setiap perubahan kode dan penambahan fitur. Entri terbaru di atas.

---

## 18 November 2026 — Pratinjau markdown per blok dan model data immutable

**Kelancaran menggulir.**

- `ui/components/Markdown.kt`: pratinjau dulu satu `Column(verticalScroll)` yang menggambar seluruh
  baris naskah sekaligus. Kini naskah dipecah menjadi blok diskret (heading, paragraf, butir,
  butir bernomor, kutipan, garis) lewat `previewBlocks()`, lalu digambar `LazyColumn` dengan kunci
  stabil per indeks, sehingga hanya blok yang terlihat yang diukur dan digambar. Paragraf yang
  berurutan digabung menjadi satu blok agar jumlah item tidak membengkak. Hasil parse disimpan
  lewat `remember(body)` agar tidak diulang saat recomposition.
- `data/Models.kt`: `Volume`, `Book`, `Chapter`, `Snapshot`, `DayStat`, `LoreEntry`, `MarginNote`,
  `Settings`, dan `AppData` ditandai `@Immutable`. Sebelumnya Compose menganggapnya tidak stabil
  karena memuat `List`, sehingga setiap item daftar (Setelan, pustaka, daftar bab) ikut dikomposisi
  ulang saat satu nilai berubah. Semua kelas ini memang hanya berisi `val` dan store selalu
  membuat salinan baru, jadi penandaan ini aman.

**Catatan.** R8 sudah aktif sejak commit sebelumnya. Baseline Profile AOT belum ditambahkan karena
butuh modul macrobenchmark dan pengukuran di perangkat, yang tidak tersedia di lingkungan ini.

---

## 18 November 2026 — Build release ber-R8 dan perbaikan bug hasil audit

**Build release.** `android/app/build.gradle.kts`: `isMinifyEnabled = true`, `isShrinkResources = true`,
`isDebuggable = false`. `proguard-rules.pro`: aturan keep untuk `$$serializer`/`Companion`
kotlinx.serialization dan `androidx.biometric`. `:app:assembleRelease` berhasil lewat
`:app:minifyReleaseWithR8`, APK 2,2 MB di `app/build/outputs/apk/release/`, ditandatangani kunci debug
karena `keystore.properties` tidak ada sehingga bisa langsung dipasang.

**Bug yang diperbaiki.**

- `ui/components/Ui.kt`: subtitle `TintaTopBar` memakai `maxLines = 1` tanpa `TextOverflow.Ellipsis`,
  sehingga teks panjang terpotong paksa di semua layar; kini berelipsis.
- `ui/screens/EditorScreen.kt`: placeholder "Tanpa judul" memakai `headlineSmall` (24sp) sementara
  teks judul asli 25sp Serif SemiBold, membuat judul berubah ukuran saat mulai diketik; disamakan.
- `ui/TintaApp.kt`: kegagalan menulis berkas ekspor ditelan `runCatching` tanpa pesan; kini muncul toast
  "Gagal menyimpan berkas." seperti alur ekspor di editor.
- `util/Spellcheck.kt`: `name.first()` pada nama lore kosong berpotensi `NoSuchElementException`;
  ditambah penjaga `isNotEmpty()`.

**Belum bisa diverifikasi:** kelancaran dan tampilan sebenarnya tidak terukur tanpa perangkat/emulator;
release hanya terbukti terkompilasi, ter-minify, dan lolos lint.

---

## 18 November 2026 — Ganti nama tokoh dari Story Bible & pemeriksa ejaan

**Fitur baru.**

- `android/app/src/main/java/id/tinta/app/data/TintaStore.kt`: `renameLore()` mengganti nama
  kartu Story Bible di seluruh bab buku itu dengan batas kata (`Ani` tidak mengubah `Anita`),
  memakai `replaceAll()` yang sudah menyimpan snapshot "sebelum-ganti", lalu memperbarui namanya.
- `android/app/src/main/java/id/tinta/app/ui/screens/LoreScreen.kt`: tombol "Ganti juga di seluruh
  naskah" di dialog kartu, dengan konfirmasi dan laporan jumlah kemunculan yang diganti.
- `android/app/src/main/java/id/tinta/app/util/Spellcheck.kt` (baru): pemeriksa ejaan offline tanpa
  kamus eksternal — kata baku (praktek, analisa, jaman, dll.), kata depan yang menempel (disini,
  diatas, dimana), spasi ganda, spasi sebelum tanda baca, tanda baca berulang, dan nama tokoh dari
  Story Bible (ejaan salah besar-kecil maupun jarak edit satu).
- `android/app/src/main/java/id/tinta/app/ui/components/SpellcheckSheet.kt` (baru): daftar temuan
  dengan cuplikan kalimat; menekan satu temuan memindahkan kursor ke kata itu.
- `android/app/src/main/java/id/tinta/app/ui/screens/EditorScreen.kt`: menu ⋮ "Periksa ejaan".
- `Models.kt`: `Settings.spellcheck` dan `Settings.dictionary` disiapkan.

Belum ada di versi ini: garis bawah langsung di dalam editor dan layar pengaturan kamus pribadi.

---

## 18 November 2026 — Perbaikan tersendat saat menggulir layar Setelan

**Perbaikan bug.** Layar Setelan sebenarnya sudah memakai `LazyColumn`; penyebab tersendat ada di
bentuk item dan di slider.

- `android/app/src/main/java/id/tinta/app/ui/screens/SettingsScreen.kt`:
  - Kartu "Menulis" dan "Data" adalah satu item `LazyColumn` setinggi beberapa layar, sehingga
    seluruh isinya tetap disusun dan digambar setiap frame saat digulir cepat. Kartu itu dipecah
    menjadi item-item kecil (kebiasaan menulis, font kustom, frasa cepat, cadangan, folder, sampah)
    agar Compose melewati bagian yang tidak terlihat.
  - `SettingSlider` kini menyimpan nilai sementara di `remember` dan baru memanggil `onChange` saat
    `onValueChangeFinished`. Sebelumnya setiap piksel geseran slider menulis ulang seluruh state
    aplikasi dan menjadwalkan penyimpanan JSON, yang membuat layar tersendat.

Catatan: laporan menyebut penyebabnya `Column(verticalScroll)` dan bayangan bertumpuk; keduanya
tidak ada di kode ini. Sisa perbedaan kecepatan berasal dari build debug tanpa R8, yang tidak diubah
pada perbaikan ini.

---

## 18 November 2026 — Tata letak otomatis saat ekspor (auto arrange)

**Fitur baru.** Ekspor buku dan ekspor bab kini menata naskah sendiri sebelum berkas dibuat:
margin buku A4, bab selalu mulai di halaman baru, teks rata kanan-kiri, indentasi baris pertama,
nomor halaman, header berjalan, dan daftar isi yang nomor halamannya dihitung dari hasil pemotongan
halaman sungguhan.

**Berkas dan perubahan.**

- `android/app/src/main/java/id/tinta/app/util/Typesetting.kt` (baru): mesin tata letak murni Kotlin
  (`TextMetrics`, `PrintFormat`, `Typesetter`, `TypesetPage`, `TypesetSection`). Menangani halaman
  judul + hak cipta, daftar isi, pemotongan baris, perataan kanan-kiri, indentasi, pencegahan judul
  dan baris yatim di dasar halaman, serta penomoran halaman. Tidak memakai Android sehingga bisa
  diuji di JVM.
- `android/app/src/main/java/id/tinta/app/util/Export.kt`: PDF buku dan bab memakai `Typesetter`;
  EPUB mendapat `style.css` (serif, jarak baris 1,55, rata kanan-kiri, indentasi, daftar isi);
  DOCX mendapat `styles.xml` (A4, margin 2,54 cm, Times New Roman 11 pt, rata kanan-kiri, indentasi,
  Heading1 dengan page-break, footer nomor halaman) dan `footer1.xml`. Ditambah
  `chapterPdf()`, `chapterEpub()`, `chapterDocx()`.
- `android/app/src/main/java/id/tinta/app/ui/components/ExportDialog.kt`: parameter `heading`
  supaya dialog yang sama dipakai untuk buku dan bab; teks pilihan format menyebut tata letak otomatis.
- `android/app/src/main/java/id/tinta/app/ui/screens/EditorScreen.kt`: menu ⋮ "Ekspor bab" membuka
  dialog ekspor (Markdown, PDF, EPUB, DOCX) untuk bab yang sedang dibuka.
- `android/app/src/test/java/id/tinta/app/util/TypesettingTest.kt` (baru): 6 unit test untuk
  penomoran halaman, bab di halaman baru, nomor daftar isi, lebar baris, judul yang tidak tertinggal
  di dasar halaman, dan pembersihan penanda Markdown.
