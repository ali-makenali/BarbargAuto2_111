package ir.barbarg.auto.data

import android.content.Context
import org.json.JSONObject

/** Stores only non-sensitive draft fields; the UI filters identifiers and contact details. */
class FreightDraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("freight_draft", Context.MODE_PRIVATE)

    fun load(): Map<String, String> {
        val raw = prefs.getString(KEY_DRAFT, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (!json.isNull(key)) put(key, json.optString(key))
                }
            }
        } catch (_: Exception) { emptyMap() }
    }

    fun save(values: Map<String, String>) {
        val json = JSONObject()
        values.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_DRAFT, json.toString()).apply()
    }

    fun clear() { prefs.edit().remove(KEY_DRAFT).apply() }

    private companion object { const val KEY_DRAFT = "draft_json" }
}
