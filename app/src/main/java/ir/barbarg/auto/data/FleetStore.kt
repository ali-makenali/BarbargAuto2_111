package ir.barbarg.auto.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Persistent local fleet database. Driver passwords are encrypted with an
 * Android Keystore AES/GCM key and are never written to SQLite as clear text.
 */
class FleetStore(context: Context) {
    private val dbHelper = FleetDb(context.applicationContext)
    private val crypto = FleetPasswordCrypto()

    fun all(): List<FleetRecord> {
        val result = mutableListOf<FleetRecord>()
        dbHelper.readableDatabase.query(
            TABLE, COLUMNS, null, null, null, null, "driver_name COLLATE NOCASE"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += FleetRecord(
                    driverNationalId = cursor.getString(0),
                    driverFullName = cursor.getString(1),
                    driverMobile = cursor.getString(2),
                    driverPassword = crypto.decrypt(cursor.getString(3)),
                    vehiclePlate = cursor.getString(4),
                    vehicleCapacity = cursor.getString(5)
                )
            }
        }
        return result
    }

    /** Returns false when required data is invalid or a national ID/plate is already assigned. */
    fun insert(record: FleetRecord): Boolean {
        if (!isValid(record)) return false
        val db = dbHelper.writableDatabase
        val duplicate = db.rawQuery(
            "SELECT 1 FROM $TABLE WHERE driver_national_id = ? OR vehicle_plate = ? LIMIT 1",
            arrayOf(record.driverNationalId, record.vehiclePlate)
        ).use { it.moveToFirst() }
        if (duplicate) return false
        return db.insert(TABLE, null, values(record)) != -1L
    }

    /** Update by the previous driver national ID; plate and ID uniqueness are enforced. */
    fun update(previousDriverNationalId: String, record: FleetRecord): Boolean {
        if (previousDriverNationalId.isBlank() || !isValid(record)) return false
        val db = dbHelper.writableDatabase
        val duplicate = db.rawQuery(
            "SELECT 1 FROM $TABLE WHERE (driver_national_id = ? OR vehicle_plate = ?) AND driver_national_id != ? LIMIT 1",
            arrayOf(record.driverNationalId, record.vehiclePlate, previousDriverNationalId)
        ).use { it.moveToFirst() }
        if (duplicate) return false
        return db.update(
            TABLE, values(record), "driver_national_id = ?",
            arrayOf(previousDriverNationalId)
        ) > 0
    }

    fun delete(driverNationalId: String): Boolean =
        dbHelper.writableDatabase.delete(
            TABLE, "driver_national_id = ?", arrayOf(driverNationalId)
        ) > 0

    fun search(query: String): List<FleetRecord> {
        val term = query.trim()
        if (term.isEmpty()) return all()
        val result = mutableListOf<FleetRecord>()
        dbHelper.readableDatabase.query(
            TABLE, COLUMNS,
            "driver_national_id LIKE ? OR vehicle_plate LIKE ? OR driver_name LIKE ?",
            arrayOf("%$term%", "%$term%", "%$term%"),
            null, null, "driver_name COLLATE NOCASE"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result += FleetRecord(
                    cursor.getString(0), cursor.getString(1), cursor.getString(2),
                    crypto.decrypt(cursor.getString(3)), cursor.getString(4), cursor.getString(5)
                )
            }
        }
        return result
    }

    private fun values(record: FleetRecord) = android.content.ContentValues().apply {
        put("driver_national_id", record.driverNationalId.trim())
        put("driver_name", record.driverFullName.trim())
        put("driver_mobile", record.driverMobile.trim())
        put("password_ciphertext", crypto.encrypt(record.driverPassword))
        put("vehicle_plate", record.vehiclePlate.trim())
        put("vehicle_capacity", record.vehicleCapacity.trim())
    }

    private fun isValid(record: FleetRecord): Boolean =
        record.driverNationalId.matches(Regex("\\d{10}")) &&
        record.driverFullName.isNotBlank() &&
        record.driverMobile.matches(Regex("09\\d{9}")) &&
        record.driverPassword.isNotBlank() &&
        record.vehiclePlate.isNotBlank() &&
        record.vehicleCapacity.isNotBlank()

    private class FleetDb(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE $TABLE (
                    driver_national_id TEXT PRIMARY KEY NOT NULL,
                    driver_name TEXT NOT NULL,
                    driver_mobile TEXT NOT NULL,
                    password_ciphertext TEXT NOT NULL,
                    vehicle_plate TEXT NOT NULL UNIQUE,
                    vehicle_capacity TEXT NOT NULL
                )""".trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // No destructive migration: future schema changes must preserve the fleet table.
        }
    }

    private class FleetPasswordCrypto {
        fun encrypt(value: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
        }

        fun decrypt(value: String): String {
            val combined = Base64.decode(value, Base64.NO_WRAP)
            require(combined.size > GCM_IV_BYTES) { "Encrypted password data is invalid" }
            val iv = combined.copyOfRange(0, GCM_IV_BYTES)
            val encrypted = combined.copyOfRange(GCM_IV_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            return String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }

        private fun getOrCreateKey(): SecretKey {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            return generator.generateKey()
        }
    }

    private companion object {
        const val DB_NAME = "barbarg_fleet.db"
        const val DB_VERSION = 1
        const val TABLE = "fleet"
        const val KEY_ALIAS = "barbarg_fleet_password_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        val COLUMNS = arrayOf(
            "driver_national_id", "driver_name", "driver_mobile",
            "password_ciphertext", "vehicle_plate", "vehicle_capacity"
        )
    }
}
