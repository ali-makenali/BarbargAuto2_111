package ir.barbarg.auto.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts the UTCMS integration credential with an Android Keystore AES-GCM key. */
class ServiceCredentialStore(context: Context) {
    data class Credentials(val companyCode: String, val password: CharArray)

    private val prefs = context.getSharedPreferences("utcms_service_credentials", Context.MODE_PRIVATE)

    fun hasCredentials(): Boolean =
        !prefs.getString("ciphertext", null).isNullOrBlank() &&
        !prefs.getString("iv", null).isNullOrBlank()

    fun save(companyCode: String, password: CharArray) {
        require(companyCode.isNotBlank() && password.isNotEmpty())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val clear = (companyCode + "\u0000" + String(password)).toByteArray(Charsets.UTF_8)
        val encrypted = try { cipher.doFinal(clear) } finally { clear.fill(0) }
        prefs.edit()
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
        encrypted.fill(0)
    }

    fun load(): Credentials? {
        val ivText = prefs.getString("iv", null) ?: return null
        val cipherText = prefs.getString("ciphertext", null) ?: return null
        val iv = Base64.decode(ivText, Base64.NO_WRAP)
        val encrypted = Base64.decode(cipherText, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        val clear = cipher.doFinal(encrypted)
        return try {
            val text = String(clear, Charsets.UTF_8)
            val split = text.indexOf('\u0000')
            if (split <= 0 || split == text.lastIndex) null
            else Credentials(text.substring(0, split), text.substring(split + 1).toCharArray())
        } finally {
            clear.fill(0)
            encrypted.fill(0)
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) { }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object { private const val KEY_ALIAS = "barbarg_utcms_service_key_v1" }
}
