package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 hashing for the vault PIN.
 *
 * Stored format: `pbkdf2:<iterations>:<saltHex>:<hashHex>`.
 *
 * Legacy values (plain-text PINs written by older app versions) are still verified so existing
 * users are not locked out, and are upgraded to a hash after the first successful unlock.
 * The old factory default "1234" is deliberately treated as "no PIN has been set".
 */
object PinHasher {

    private const val PREFIX = "pbkdf2"
    private const val SEPARATOR = ":"
    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val LEGACY_DEFAULT_PIN = "1234"
    private const val PIN_LENGTH = 4
    private val HEX = "0123456789abcdef".toCharArray()

    private val WEAK_PINS = setOf("1234", "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999", "4321")

    fun isHashed(stored: String): Boolean = stored.startsWith(PREFIX + SEPARATOR)

    /** True when [stored] represents a PIN the user actually chose. */
    fun hasPin(stored: String): Boolean =
        isHashed(stored) || (stored.isNotEmpty() && stored != LEGACY_DEFAULT_PIN)

    /** True for a plain-text PIN written by an older version (needs upgrading to a hash). */
    fun isLegacyPlain(stored: String): Boolean = hasPin(stored) && !isHashed(stored)

    fun isValidFormat(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    fun isWeak(pin: String): Boolean = pin in WEAK_PINS

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = derive(pin, salt, ITERATIONS)
        return listOf(PREFIX, ITERATIONS.toString(), toHex(salt), toHex(derived)).joinToString(SEPARATOR)
    }

    fun verify(pin: String, stored: String): Boolean {
        if (!hasPin(stored)) return false
        if (!isHashed(stored)) {
            return MessageDigest.isEqual(pin.toByteArray(Charsets.UTF_8), stored.toByteArray(Charsets.UTF_8))
        }
        val parts = stored.split(SEPARATOR)
        if (parts.size != 4) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations < 1) return false
        val salt = fromHex(parts[2]) ?: return false
        val expected = fromHex(parts[3]) ?: return false
        val actual = derive(pin, salt, iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            out[i * 2] = HEX[v ushr 4]
            out[i * 2 + 1] = HEX[v and 0x0F]
        }
        return String(out)
    }

    private fun fromHex(text: String): ByteArray? {
        if (text.isEmpty() || text.length % 2 != 0) return null
        val out = ByteArray(text.length / 2)
        for (i in out.indices) {
            val hi = Character.digit(text[i * 2], 16)
            val lo = Character.digit(text[i * 2 + 1], 16)
            if (hi < 0 || lo < 0) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
