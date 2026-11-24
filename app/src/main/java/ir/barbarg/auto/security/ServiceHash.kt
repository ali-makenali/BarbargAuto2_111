package ir.barbarg.auto.security

import android.util.Base64
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Implements the hash layout currently described in the supplied project.
 * The exact byte layout must be confirmed against SIPAAD's official integration
 * specification before this is used against production services.
 */
object ServiceHash {
    data class Result(val salt: Int, val hashedValue: String)

    fun create(companyCode: String, password: CharArray): Result {
        require(companyCode.isNotBlank()) { "Company code must not be blank" }
        require(password.isNotEmpty() && password.any { !it.isWhitespace() }) {
            "Password must not be empty or whitespace"
        }

        val passwordBytes = encodeUtf16Le(password)
        val passwordDigest = try {
            MessageDigest.getInstance("SHA-512").digest(passwordBytes)
        } finally {
            passwordBytes.fill(0)
        }

        val passwordHash = Base64.encodeToString(passwordDigest, Base64.NO_WRAP)
        passwordDigest.fill(0)

        val salt = secureRandom.nextInt(Int.MAX_VALUE - 1) + 1
        val payload = (companyCode + passwordHash + salt.toString())
            .toByteArray(StandardCharsets.UTF_16LE)
        val digest = try {
            MessageDigest.getInstance("SHA-512").digest(payload)
        } finally {
            payload.fill(0)
        }

        return try {
            Result(salt, Base64.encodeToString(digest, Base64.NO_WRAP))
        } finally {
            digest.fill(0)
        }
    }

    private fun encodeUtf16Le(chars: CharArray): ByteArray {
        val encoded = StandardCharsets.UTF_16LE.encode(CharBuffer.wrap(chars))
        val bytes = ByteArray(encoded.remaining())
        encoded.get(bytes)
        if (encoded.hasArray()) encoded.array().fill(0)
        return bytes
    }

    private val secureRandom = SecureRandom()
}
