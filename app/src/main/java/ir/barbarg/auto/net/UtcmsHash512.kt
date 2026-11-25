package ir.barbarg.auto.net

import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Reproduces the two-stage SHA-512 procedure shown in the supplied UTCMS C# sample.
 *
 * C# Encoding.Unicode is UTF-16 little-endian. The salt is passed explicitly to
 * [calculate] so callers and tests can verify the exact value included in the hash.
 */
data class UtcmsHashCredentials(
    val salt: Int,
    val hashedValue: String
)

object UtcmsHash512 {
    private val utf16Le: Charset = Charset.forName("UTF-16LE")
    private val secureRandom = SecureRandom()

    /**
     * Generates a non-negative salt. The C# sample uses time-seeded Random,
     * which is predictable; this implementation uses SecureRandom instead.
     */
    fun generateSalt(): Int = secureRandom.nextInt(Int.MAX_VALUE)

    /**
     * Algorithm from the supplied sample:
     * Base64(SHA512(UTF16LE(companyCode + Base64(SHA512(UTF16LE(password))) + salt)))
     *
     * Salt is rendered as a decimal integer, matching normal C# integer
     * concatenation for the documented sample's invariant-culture digits.
     */
    fun calculate(companyCode: String, password: String, salt: Int): UtcmsHashCredentials {
        require(companyCode.isNotEmpty()) { "CompanyCode must not be empty" }
        require(password.isNotEmpty()) { "Password must not be empty" }
        require(salt >= 0) { "Salt must be non-negative" }

        val passwordBytes = password.toByteArray(utf16Le)
        val passwordHashBytes = sha512(passwordBytes)
        val passwordHashValue = encodeBase64(passwordHashBytes)

        val combined = companyCode + passwordHashValue + salt.toString()
        val inputBytes = combined.toByteArray(utf16Le)
        val hashBytes = sha512(inputBytes)
        val hashedValue = encodeBase64(hashBytes)

        return UtcmsHashCredentials(salt = salt, hashedValue = hashedValue)
    }


    /** Small RFC 4648 Base64 encoder to keep compatibility with minSdk 23. */
    private fun encodeBase64(input: ByteArray): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val output = StringBuilder(((input.size + 2) / 3) * 4)
        var index = 0
        while (index < input.size) {
            val first = input[index++].toInt() and 0xff
            val hasSecond = index < input.size
            val second = if (hasSecond) input[index++].toInt() and 0xff else 0
            val hasThird = index < input.size
            val third = if (hasThird) input[index++].toInt() and 0xff else 0

            output.append(alphabet[first ushr 2])
            output.append(alphabet[((first and 0x03) shl 4) or (second ushr 4)])
            output.append(if (hasSecond) alphabet[((second and 0x0f) shl 2) or (third ushr 6)] else '=')
            output.append(if (hasThird) alphabet[third and 0x3f] else '=')
        }
        return output.toString()
    }

    private fun sha512(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-512").digest(input)
}
