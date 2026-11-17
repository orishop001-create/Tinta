package id.tinta.app.util

import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Berkas cadangan terenkripsi: kata sandi diturunkan dengan PBKDF2-HMAC-SHA256,
 * isi diacak dengan AES-GCM. Semua dikerjakan di perangkat; tidak ada kunci yang
 * pernah dikirim keluar atau disimpan di aplikasi.
 */
object Crypto {
    private val magic = "TINTA1".toByteArray(Charsets.US_ASCII)
    private const val iterations = 150_000
    private const val keyBits = 256
    private const val saltBytes = 16
    private const val ivBytes = 12
    private const val tagBits = 128

    fun isEncrypted(data: ByteArray): Boolean =
        data.size > magic.size && data.copyOfRange(0, magic.size).contentEquals(magic)

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(saltBytes).also(random::nextBytes)
        val iv = ByteArray(ivBytes).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(password, salt), GCMParameterSpec(tagBits, iv))
        val body = cipher.doFinal(plain)
        val out = ByteArrayOutputStream(magic.size + saltBytes + ivBytes + body.size + 8)
        out.write(magic)
        out.write(salt)
        out.write(iv)
        out.write(byteArrayOf(
            (iterations ushr 24).toByte(),
            (iterations ushr 16).toByte(),
            (iterations ushr 8).toByte(),
            iterations.toByte(),
        ))
        out.write(body)
        return out.toByteArray()
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        require(data.size > magic.size + saltBytes + ivBytes + 4) { "Berkas terenkripsi tidak lengkap." }
        require(isEncrypted(data)) { "Bukan berkas cadangan terenkripsi Tinta." }
        var offset = magic.size
        val salt = data.copyOfRange(offset, offset + saltBytes)
        offset += saltBytes
        val iv = data.copyOfRange(offset, offset + ivBytes)
        offset += ivBytes
        val rounds = ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
        offset += 4
        require(rounds in 1_000..1_000_000) { "Berkas terenkripsi tidak dikenali." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt, rounds), GCMParameterSpec(tagBits, iv))
        return cipher.doFinal(data, offset, data.size - offset)
    }

    private fun derive(password: CharArray, salt: ByteArray, rounds: Int = iterations): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, rounds, keyBits)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(encoded, "AES")
    }
}
