# Tinta — Android native

Aplikasi menulis **Kotlin + Jetpack Compose** yang berjalan sepenuhnya di perangkat:
tidak ada WebView, akun, iklan, analytics, atau izin jaringan. Semua fitur yang ada terbuka
tanpa paywall.

## Build

Prasyarat: JDK 17, Android SDK Platform 35, Build-Tools 35.0.0.

```bash
cd android
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Atau buka folder `android/` di Android Studio. `gradle.properties` membatasi memori Gradle
(1,5 GB) supaya bisa dibangun di mesin kecil.

APK yang dihasilkan adalah **debug build**, ditandatangani dengan debug keystore. Untuk
rilis publik, buat keystore sendiri dan konfigurasi `signingConfigs`; jangan pakai debug key.

## CI / unduh APK

Workflow [`.github/workflows/android.yml`](../.github/workflows/android.yml) berjalan pada
push ke `main` (atau manual lewat **Run workflow**): build APK debug, lint, lalu unggah
`tinta-android-debug` (APK + checksum) dan `tinta-source` (arsip sumber). Unduh dari tab
**Actions → Android build → run terbaru → Artifacts**. Artefak disimpan 30 hari; binary
build tidak disimpan di git.

## Fitur

- Pustaka buku dengan sampul gradien generatif, tag, dan kartu "Lanjutkan".
- Buku → bagian → bab; buat, ubah, duplikat, pindahkan, dan hapus ke sampah.
- Editor fokus: simpan otomatis (debounce 550 ms), judul, jumlah kata/karakter, estimasi
  waktu baca, empat tema, tiga tumpukan font, ukuran dan jarak baris.
- Markdown per naskah: pratinjau aman (heading, tebal/miring, kode, kutipan, daftar, garis),
  bilah format, dan sisipan frasa cepat.
- Riwayat: snapshot otomatis saat buka, tiap dua menit, manual, dan sebelum pemulihan;
  diff baris; pulihkan dengan versi pengaman.
- Cari lintas buku: filter buku, beda huruf besar/kecil, regex, dan ganti semua dengan
  konfirmasi + snapshot.
- Statistik: target harian, runtutan, total kata, peta panas 12 minggu, naskah terakhir.
- Ekspor: bagikan teks, simpan Markdown, cadangkan/pulihkan JSON penuh; sampah dengan
  pemulihan.
- Halaman **Tinta Pro** (dari Setelan): katalog semua fitur dalam kisi empat kolom dengan
  sorotan dan bilah aksi tetap. Tanpa pembelian, langganan, TTS, atau AI.
- Penyimpanan lokal JSON atomik di `filesDir/tinta.json`; cadangan otomatis saat aplikasi
  dihentikan.

## Struktur

```
app/src/main/java/id/tinta/app/
  MainActivity.kt          activity + Compose root
  data/                    model, seed, TintaStore (JSON + StateFlow)
  ui/TintaApp.kt           Navigation Compose routes
  ui/screens/              Pustaka, Buku, Editor, Riwayat, Cari, Statistik, Setelan, Sampah, Pro
  ui/components/           top bar, kontrol, sampul, Markdown
  ui/theme/                empat palet + tipografi
  util/                    kata/tanggal, diff baris
```

Tidak ada dependensi database: satu berkas JSON cukup untuk pustaka pribadi dan membuat
cadangan mudah diperiksa. Untuk pustaka yang sangat besar, migrasi ke Room/IndexedDB
setara (SQLite) adalah langkah berikutnya.

## Batas versi 1.0

- Belum dijalankan di emulator/perangkat pada lingkungan build ini; yang diverifikasi baru
  kompilasi, lint, penandatanganan debug, dan metadata APK. Lakukan smoke test di perangkat
  sebelum menganggapnya siap rilis.
- Belum ada PDF/gambar/HTML/DOCX/EPUB, TTS, sinkronisasi cloud, atau bagikan multiperangkat.
- Belum ada tombol undo/redo khusus; teks memakai undo bawaan platform. Riwayat versi tetap
  tersedia.
- Urutan bab/bagian belum bisa diseret; gunakan duplikat/pindah bagian.
- Markdown adalah subset aman, bukan CommonMark penuh. Tidak ada upload gambar.
- JSON tidak dienkripsi dan tidak ada kunci aplikasi. Itu tidak akan memberi keamanan nyata
  pada perangkat yang tidak aman, jadi fitur semu itu tidak ditawarkan.
- Saat dua salinan aplikasi menulis bersamaan (misalnya dua profil), berkas terakhir menang;
  tidak ada merge.

Rencana berikutnya: Room/SQLite + migrasi, undo/redo, PDF cetak, EPUB/DOCX, dan pengujian
instrumentasi di perangkat.
