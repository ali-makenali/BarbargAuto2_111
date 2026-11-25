package ir.barbarg.auto.net

import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

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
        val passwordHashValue = Base64.getEncoder().encodeToString(passwordHashBytes)

        val combined = companyCode + passwordHashValue + salt.toString()
        val inputBytes = combined.toByteArray(utf16Le)
        val hashBytes = sha512(inputBytes)
        val hashedValue = Base64.getEncoder().encodeToString(hashBytes)

        return UtcmsHashCredentials(salt = salt, hashedValue = hashedValue)
    }

    private fun sha512(input: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-512").digest(input)
}
