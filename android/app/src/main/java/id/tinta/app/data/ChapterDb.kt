package id.tinta.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import id.tinta.app.util.Text

/**
 * Badan naskah disimpan per bab di SQLite. Metadata buku, bab, riwayat, dan setelan
 * tetap di tinta.json, sehingga setiap jeda ketik hanya menulis satu baris naskah,
 * bukan seluruh isi buku.
 */
class ChapterDb(context: Context) : SQLiteOpenHelper(context, "tinta-naskah.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE naskah (id TEXT PRIMARY KEY, body TEXT NOT NULL, words INTEGER NOT NULL DEFAULT 0)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun bodies(): Map<String, String> {
        val out = HashMap<String, String>()
        runCatching {
            readableDatabase.rawQuery("SELECT id, body FROM naskah", null).use { cursor ->
                while (cursor.moveToNext()) {
                    out[cursor.getString(0)] = cursor.getString(1) ?: ""
                }
            }
        }
        return out
    }

    fun put(id: String, body: String) {
        runCatching {
            writableDatabase.execSQL(
                "INSERT OR REPLACE INTO naskah (id, body, words) VALUES (?, ?, ?)",
                arrayOf<Any>(id, body, Text.wordCount(body)),
            )
        }
    }

    fun putAll(chapters: List<Chapter>) {
        runCatching {
            val db = writableDatabase
            db.beginTransaction()
            try {
                chapters.forEach { chapter ->
                    db.execSQL(
                        "INSERT OR REPLACE INTO naskah (id, body, words) VALUES (?, ?, ?)",
                        arrayOf<Any>(chapter.id, chapter.body, Text.wordCount(chapter.body)),
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun delete(ids: Collection<String>) {
        if (ids.isEmpty()) return
        runCatching {
            val marks = ids.joinToString(",") { "?" }
            writableDatabase.execSQL("DELETE FROM naskah WHERE id IN ($marks)", ids.toTypedArray())
        }
    }

    fun clear() {
        runCatching { writableDatabase.execSQL("DELETE FROM naskah") }
    }
}
