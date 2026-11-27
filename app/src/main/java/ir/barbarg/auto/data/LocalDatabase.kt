package ir.barbarg.auto.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-local durable store. Records are separated by domain and keyed by a
 * stable business key (for example vehicle plate or driver national code).
 * There is deliberately no 20-record ceiling.
 */
class LocalDatabase(context: Context) : SQLiteOpenHelper(context, "barbarg_local.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE local_records (
                record_type TEXT NOT NULL,
                record_key TEXT NOT NULL,
                payload TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY(record_type, record_key)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_local_records_type ON local_records(record_type)")
        db.execSQL("""
            CREATE TABLE auth_profiles (
                national_code TEXT PRIMARY KEY,
                account_type TEXT NOT NULL,
                mobile TEXT NOT NULL,
                display_name TEXT NOT NULL DEFAULT '',
                verified_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE bol_history (
                trace_code TEXT PRIMARY KEY,
                request_json TEXT NOT NULL,
                response_json TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema migrations must be added here; never drop user data.
    }

    @Synchronized
    fun put(type: String, key: String, payload: JSONObject): Boolean {
        require(type in setOf("vehicle", "driver", "cargo", "route", "bol"))
        require(key.isNotBlank())
        val now = System.currentTimeMillis()
        val db = writableDatabase
        val existing = db.rawQuery(
            "SELECT created_at FROM local_records WHERE record_type=? AND record_key=?",
            arrayOf(type, key)
        ).use { if (it.moveToFirst()) it.getLong(0) else now }
        val values = ContentValues().apply {
            put("record_type", type)
            put("record_key", key)
            put("payload", payload.toString())
            put("created_at", existing)
            put("updated_at", now)
        }
        return db.insertWithOnConflict("local_records", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L
    }

    @Synchronized
    fun all(type: String): JSONArray {
        require(type in setOf("vehicle", "driver", "cargo", "route", "bol"))
        val result = JSONArray()
        readableDatabase.rawQuery(
            "SELECT record_key,payload,created_at,updated_at FROM local_records WHERE record_type=? ORDER BY updated_at DESC",
            arrayOf(type)
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.put(JSONObject()
                    .put("key", cursor.getString(0))
                    .put("data", JSONObject(cursor.getString(1)))
                    .put("createdAt", cursor.getLong(2))
                    .put("updatedAt", cursor.getLong(3)))
            }
        }
        return result
    }

    @Synchronized
    fun delete(type: String, key: String): Boolean {
        require(type in setOf("vehicle", "driver", "cargo", "route", "bol"))
        return writableDatabase.delete(
            "local_records", "record_type=? AND record_key=?", arrayOf(type, key)
        ) > 0
    }

    @Synchronized
    fun saveVerifiedProfile(nationalCode: String, accountType: String, mobile: String, name: String = "") {
        require(accountType == "driver" || accountType == "company")
        val values = ContentValues().apply {
            put("national_code", nationalCode)
            put("account_type", accountType)
            put("mobile", mobile)
            put("display_name", name)
            put("verified_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("auth_profiles", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun isProfileVerified(nationalCode: String, mobile: String): Boolean {
        return readableDatabase.rawQuery(
            "SELECT 1 FROM auth_profiles WHERE national_code=? AND mobile=? LIMIT 1",
            arrayOf(nationalCode, mobile)
        ).use { it.moveToFirst() }
    }

    @Synchronized
    fun saveBolFailure(request: JSONObject, response: JSONObject) {
        val now = System.currentTimeMillis()
        val localId = "FAILED-$now"
        val values = ContentValues().apply {
            put("trace_code", localId)
            put("request_json", request.toString())
            put("response_json", response.toString())
            put("status", "FAILED")
            put("created_at", now)
            put("updated_at", now)
        }
        writableDatabase.insertWithOnConflict("bol_history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Synchronized
    fun bolHistory(): JSONArray {
        val result = JSONArray()
        readableDatabase.rawQuery(
            "SELECT trace_code,status,response_json,created_at FROM bol_history ORDER BY created_at DESC LIMIT 100",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.put(JSONObject()
                    .put("traceCode", cursor.getString(0))
                    .put("status", cursor.getString(1))
                    .put("response", JSONObject(cursor.getString(2)))
                    .put("createdAt", cursor.getLong(3)))
            }
        }
        return result
    }

    @Synchronized
    fun latestIssuedTrace(): String {
        return readableDatabase.rawQuery(
            "SELECT trace_code FROM bol_history WHERE status='ISSUED' ORDER BY created_at DESC LIMIT 1",
            null
        ).use { if (it.moveToFirst()) it.getString(0) else "" }
    }

    @Synchronized
    fun saveBol(traceCode: String, request: JSONObject, response: JSONObject, status: String) {
        require(traceCode.isNotBlank())
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("trace_code", traceCode)
            put("request_json", request.toString())
            put("response_json", response.toString())
            put("status", status)
            put("created_at", now)
            put("updated_at", now)
        }
        writableDatabase.insertWithOnConflict("bol_history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
