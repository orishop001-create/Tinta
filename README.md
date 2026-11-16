# Tinta

Aplikasi menulis **Android native** dengan Kotlin dan Jetpack Compose. Buku, bagian, bab,
editor tanpa gangguan dengan simpan otomatis, riwayat versi, pencarian lintas buku,
statistik, Markdown, tema, dan ekspor lokal — semuanya terbuka. Tanpa akun, iklan,
analitik, izin jaringan, atau versi Pro.

Dibangun ulang secara **clean-room** dari fitur publik aplikasi menulis. Nama, palet, ikon,
sampul, dan seluruh teks dibuat untuk Tinta. Tidak ada kode, aset, logo, font, atau teks
milik aplikasi lain yang dipakai.

## Unduh APK dari GitHub Actions

Setiap push ke `main` yang menyentuh proyek Android membangun APK debug, menjalankan lint,
dan mengunggah hasilnya sebagai artefak Actions (disimpan 30 hari):

1. Buka [Actions → Android build](https://github.com/ErlanggaDev-Studios/Pure-writer/actions/workflows/android.yml).
2. Pilih run terbaru yang hijau.
3. Di bagian **Artifacts**, unduh `tinta-android-debug` (zip berisi APK + `SHA256SUMS.txt`)
   atau `tinta-source` (arsip sumber).

Tidak ada APK biner di git. APK ditandatangani dengan debug key; untuk distribusi publik,
buat keystore rilis sendiri.

## Build native

Prasyarat: JDK 17, Android SDK Platform 35, Build-Tools 35.0.0.

```bash
cd android
./gradlew :app:assembleDebug     # APK di app/build/outputs/apk/debug/
./gradlew :app:lintDebug
```

Atau buka `android/` di Android Studio. Detail arsitektur, CI, dan batas versi ada di
[`android/README.md`](android/README.md).

## Fitur native

- Pustaka dengan sampul generatif, tag, dan kartu lanjutkan.
- Buku → bagian → bab; buat, ubah, duplikat, pindah bagian, sampah + pulihkan.
- Editor: simpan otomatis, hitungan kata/karakter, estimasi baca, empat tema, tiga font,
  ukuran dan jarak baris, mode fokus.
- Markdown per naskah dengan pratinjau aman, bilah format, dan frasa cepat.
- Riwayat otomatis/manual, diff baris, pemulihan dengan versi pengaman.
- Cari & ganti lintas buku: filter buku, beda huruf, regex, konfirmasi ganti massal.
- Statistik: target harian, runtutan, total kata, peta panas 12 minggu.
- Bagikan teks, simpan Markdown, cadangkan/pulihkan JSON penuh.
- Halaman Fitur Tinta: katalog semua fitur dalam kisi empat kolom dengan sorotan dan
  bilah aksi tetap. Semuanya gratis; tanpa pembelian, langganan, TTS, atau AI.
- Penyimpanan lokal atomik di `filesDir/tinta.json`; tanpa database, tanpa jaringan.

## Batas yang diketahui

- APK sudah **dibangun dan lolos lint**, tetapi belum dijalankan di emulator/perangkat pada
  lingkungan ini. Lakukan smoke test perangkat sebelum menganggapnya siap rilis.
- Belum ada PDF/gambar/HTML/DOCX/EPUB, TTS, atau sinkronisasi cloud. Tidak ada kunci aplikasi
  palsu; JSON lokal tidak dienkripsi.
- Belum ada tombol undo/redo khusus; teks memakai undo platform dan riwayat versi.
- Urutan bab/bagian belum bisa diseret.
- Markdown adalah subset aman, bukan CommonMark penuh.

## Prototipe web

Rebuild web sebelumnya tetap disimpan sebagai prototipe di `src/` dan dapat dijalankan
dengan `npm run dev`. Prototipe itu bukan lagi artefak utama. Dokumen recon, desain, QA,
brand, dan paritas web ada di [`replica/`](replica/); catatan build native ada di
[`replica/native.md`](replica/native.md).
