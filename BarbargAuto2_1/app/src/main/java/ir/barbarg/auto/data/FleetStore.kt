package ir.barbarg.auto.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

class FleetStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> {
        val raw = prefs.getString(KEY_FLEETS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(fleets: List<String>) {
        val array = JSONArray()
        fleets.forEach(array::put)
        prefs.edit { putString(KEY_FLEETS, array.toString()) }
    }

    private companion object {
        const val PREFS_NAME = "barbarg_fleets"
        const val KEY_FLEETS = "fleets"
    }
}
