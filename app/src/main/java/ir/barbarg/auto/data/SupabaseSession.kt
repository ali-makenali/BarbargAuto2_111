package ir.barbarg.auto.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Per-install anonymous Supabase identity for private cloud backup.
 * The refresh token is encrypted with an Android Keystore AES-GCM key.
 * This identity is not proof of UTCMS driver identity; the app gates syncing
 * on successful UTCMS OTP and RLS scopes data to this installation's user.
 */
class SupabaseSession(private val context: Context) {
    private val prefs = context.getSharedPreferences("barbarg_supabase_session", Context.MODE_PRIVATE)
    private val base = "https://ufejrknhkwrtvfesxbbp.supabase.co"
    private val apiKey = "sb_publishable_2NKIDYuRSCD4KFyFgW7RdA_TIuNr1r9"

    data class Tokens(val access: String, val refresh: String)

    suspend fun sync(entity: String, action: String, record: JSONObject? = null, id: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val token = validAccessToken() ?: return@withContext false
                val body = JSONObject().put("entity", entity).put("action", action)
                if (record != null) body.put("record", record)
                if (id != null) body.put("id", id)
                request("$base/functions/v1/barbarg-sync", "POST", body, token)
                true
            } catch (_: Exception) { false }
        }

    suspend fun list(entity: String): org.json.JSONArray? = withContext(Dispatchers.IO) {
        try {
            val token = validAccessToken() ?: return@withContext null
            val response = request("$base/functions/v1/barbarg-sync", "POST",
                JSONObject().put("entity", entity).put("action", "list"), token)
            response.optJSONArray("data")
        } catch (_: Exception) { null }
    }

    private fun validAccessToken(): String? {
        val refresh = decrypt(prefs.getString("refresh", null)) ?: return createAnonymousSession()?.access
        val access = decrypt(prefs.getString("access", null))
        val expires = prefs.getLong("expires", 0L)
        if (!access.isNullOrBlank() && System.currentTimeMillis() < expires - 60_000L) return access
        val response = request("$base/auth/v1/token?grant_type=refresh_token", "POST",
            JSONObject().put("refresh_token", refresh), null)
        return saveTokens(response)?.access
    }

    private fun createAnonymousSession(): Tokens? {
        val response = request("$base/auth/v1/signup", "POST", JSONObject().put("data", JSONObject()), null)
        return saveTokens(response)
    }

    private fun saveTokens(json: JSONObject): Tokens? {
        val access = json.optString("access_token").takeIf { it.isNotBlank() } ?: return null
        val refresh = json.optString("refresh_token").takeIf { it.isNotBlank() } ?: return null
        val seconds = json.optLong("expires_in", 3600L)
        prefs.edit().putString("access", encrypt(access)).putString("refresh", encrypt(refresh))
            .putLong("expires", System.currentTimeMillis() + seconds * 1000L).apply()
        return Tokens(access, refresh)
    }

    private fun request(url: String, method: String, body: JSONObject, bearer: String?): JSONObject {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", apiKey)
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Supabase HTTP $code: $raw")
            return JSONObject(raw)
        } finally { conn.disconnect() }
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("barbarg_supabase_aes", null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder("barbarg_supabase_aes",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return gen.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
    }

    private fun decrypt(value: String?): String? {
        return try {
            if (value.isNullOrBlank()) null else {
                val all = android.util.Base64.decode(value, android.util.Base64.NO_WRAP)
                val iv = all.copyOfRange(0, 12)
                val data = all.copyOfRange(12, all.size)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
                String(cipher.doFinal(data), Charsets.UTF_8)
            }
        } catch (_: Exception) { null }
    }
}
