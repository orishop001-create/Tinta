# Log perubahan Tinta

Catatan setiap perubahan kode dan penambahan fitur. Entri terbaru di atas.

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
