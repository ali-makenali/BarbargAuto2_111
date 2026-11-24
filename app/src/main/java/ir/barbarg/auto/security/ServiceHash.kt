package ir.barbarg.auto.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

object ServiceHash {
    data class Result(val salt: Int, val hashedValue: String)

    fun create(companyCode: String, password: CharArray): Result {
        require(companyCode.isNotBlank()) { "Company code must not be blank" }
        require(password.isNotEmpty()) { "Password must not be empty" }

        val passwordText = String(password)
        val passwordDigest = MessageDigest.getInstance("SHA-512")
            .digest(passwordText.toByteArray(StandardCharsets.UTF_16LE))
        val passwordHash = Base64.encodeToString(passwordDigest, Base64.NO_WRAP)

        val salt = SecureRandom().nextInt(Int.MAX_VALUE)
        val payload = (companyCode + passwordHash + salt.toString())
            .toByteArray(StandardCharsets.UTF_16LE)
        val digest = MessageDigest.getInstance("SHA-512").digest(payload)

        return Result(salt, Base64.encodeToString(digest, Base64.NO_WRAP))
    }
}
