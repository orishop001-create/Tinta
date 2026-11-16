package id.tinta.app.data

import id.tinta.app.util.Text
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Seed {
    private const val DAY = 86_400_000L

    fun create(now: Long = System.currentTimeMillis()): AppData {
        fun at(daysAgo: Long) = now - daysAgo * DAY
        val catatan = "b-catatan"
        val hujan = "b-hujan"
        val books = listOf(
            Book(
                id = catatan,
                title = "Catatan Kecil",
                tags = listOf("jurnal"),
                cover = 0,
                createdAt = at(9),
                updatedAt = at(1),
            ),
            Book(
                id = hujan,
                title = "Hujan di Kota Tua",
                tags = listOf("fiksi", "draf"),
                cover = 1,
                volumes = listOf(
                    Volume("v-satu", "Bagian I — Peron"),
                    Volume("v-dua", "Bagian II — Surat"),
                ),
                createdAt = at(6),
                updatedAt = at(1),
            ),
        )

        val chapters = listOf(
            Chapter(
                id = "d-selamat",
                bookId = catatan,
                title = "Mulai dari sini",
                body = """Selamat datang di Tinta.

Ini ruang menulis yang menganggap tulisanmu lebih penting daripada aplikasinya. Tidak ada tombol simpan, karena menyimpan seharusnya tidak perlu kamu ingat.

**Tiga hal yang perlu kamu tahu:**

1. Semua yang kamu tulis tersimpan otomatis di perangkat ini.
2. Setiap perubahan punya riwayat. Buka menu ⋯ → **Riwayat** untuk melihat dan memulihkan versi lama.
3. Semua fitur terbuka. Tidak ada akun, iklan, atau versi Pro.

Coba sekarang: hapus teks ini, lalu mulai dengan kalimatmu sendiri. Atau biarkan catatan ini sebagai pengingat bahwa halaman kosong tidak harus menakutkan.

**Tips kecil**

- Tekan lama teks untuk memilih, lalu gunakan bilah format di bawah.
- Nyalakan Markdown lewat menu ⋯ kalau kamu suka menulis dengan sintaks.
- Buka **Statistik** untuk melihat hari-hari yang sudah kamu isi.
- Bagikan atau ekspor Markdown kapan saja. Gratis.

Selamat menulis.""",
                markdown = true,
                order = 0,
                createdAt = at(9),
                updatedAt = at(1),
            ),
            Chapter(
                id = "d-ide",
                bookId = catatan,
                title = "Ide yang belum selesai",
                body = """Beberapa benih yang mungkin tumbuh jadi sesuatu:

- Cerita tentang kota yang lupa cara hujan.
- Esai pendek soal menulis setiap hari, bahkan saat tidak ingin.
- Wawancara fiktif dengan diriku sepuluh tahun lalu.

Pilih satu, beri waktu dua puluh menit, lalu lihat apakah masih menarik besok.""",
                markdown = false,
                order = 1,
                createdAt = at(4),
                updatedAt = at(4),
            ),
            Chapter(
                id = "d-bab1",
                bookId = hujan,
                volumeId = "v-satu",
                title = "Bab 1 — Kereta Terlambat",
                body = """Kereta terakhir selalu datang terlambat, dan malam itu tidak berbeda.

Aku berdiri di ujung peron, menghitung tetes air yang jatuh dari atap seng. Suara hujan menutupi pengumuman yang tidak pernah jelas. Di seberang, lampu stasiun memantul di genangan, memanjang seperti jalan yang tidak jadi pergi.

Seorang lelaki tua duduk di bangku kayu, membawa kotak kayu kecil di pangkuannya. Ia tidak menatap rel. Ia menatap jam.

"Masih lama," kataku, sekadar mengisi udara.

Ia mengangguk. "Saya menunggu kereta yang sudah lewat."

Aku tidak bertanya. Di kota ini, orang sering menunggu hal-hal yang sudah pergi. Kenangan punya jadwalnya sendiri, dan kerap lebih tepat daripada kereta.

Ketika kereta akhirnya tiba, pintunya membuka dengan suara mengeluh. Lelaki itu tetap duduk. Aku naik, lalu menoleh dari dalam. Ia masih di sana, kotak kayu di pangkuan, seperti sedang menjaga sesuatu yang tidak boleh basah.

Peron perlahan menghilang di balik jendela. Hujan menulis garis-garis miring di kaca, dan untuk pertama kalinya dalam beberapa minggu, aku merasa tidak perlu ke mana-mana.""",
                markdown = false,
                order = 0,
                createdAt = at(6),
                updatedAt = at(2),
            ),
            Chapter(
                id = "d-bab2",
                bookId = hujan,
                volumeId = "v-satu",
                title = "Bab 2 — Payung Pinjaman",
                body = """Payung itu bukan milikku.

Aku menemukannya di kursi kereta, masih basah, gagangnya dihiasi ukiran kecil berbentuk daun. Tidak ada nama. Tidak ada catatan. Hanya payung hitam yang baunya seperti halaman buku lama.

Aku membawanya pulang, berniat menyerahkannya ke pos kehilangan besok pagi. Tetapi besok pagi datang dengan hujan yang lebih rajin, dan payung itu sudah berdiri di dekat pintu, seolah menunggu.

Di dalam lipatannya, terselip selembar kertas kecil. Tulisan tangan yang miring ke kanan: "Kalau kamu menemukan ini, berarti aku benar. Hujan memang memilih orangnya."

Tidak ada tanda tangan. Tidak ada alamat.

Aku membaca kalimat itu tiga kali, lalu menyeduh kopi, lalu menulis balasan yang tidak akan pernah dikirim. Beberapa hal lebih nyaman tinggal sebagai kemungkinan.""",
                markdown = false,
                order = 1,
                createdAt = at(5),
                updatedAt = at(1),
            ),
            Chapter(
                id = "d-bab3",
                bookId = hujan,
                volumeId = "v-dua",
                title = "Bab 3 — Surat Tanpa Alamat",
                body = """Kepada yang menemukan payungku,

Aku tidak tahu siapa kamu, dan mungkin itu bagian terbaiknya. Aku menulis surat ini di meja yang sama tempat aku kehilangan keberanian untuk mengirim surat-surat sebelumnya.

Kalau kamu sedang membaca ini, jangan mengembalikan payungnya. Pakai saja. Kota ini punya terlalu banyak hujan untuk orang yang berjalan tanpa perlindungan.

Mungkin suatu hari kita akan bertemu di peron yang sama, sama-sama basah, sama-sama pura-pura tidak mengenali payung itu. Sampai saat itu, anggap surat ini sebagai terima kasih.

— Yang menunggu kereta yang sudah lewat""",
                markdown = false,
                order = 0,
                createdAt = at(2),
                updatedAt = at(1),
            ),
        )

        val grouped = chapters.groupBy {
            Instant.ofEpochMilli(it.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }
        val daily = grouped.map { (date, docs) ->
            DayStat(date.format(DateTimeFormatter.ISO_LOCAL_DATE), docs.sumOf { Text.wordCount(it.body) })
        }.sortedBy { it.date }

        return AppData(books = books, chapters = chapters, daily = daily)
    }
}
