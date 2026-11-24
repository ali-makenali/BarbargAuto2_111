package ir.barbarg.auto.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FleetViewModel(application: Application) : AndroidViewModel(application) {
    private val store = FleetStore(application)
    private val _fleets = MutableLiveData(store.load())
    val fleets: LiveData<List<String>> = _fleets

    fun addFleet(rawName: String): Boolean {
        val name = rawName.trim()
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) return false
        val current = _fleets.value.orEmpty()
        if (current.any { it.equals(name, ignoreCase = true) }) return false
        val updated = current + name
        store.save(updated)
        _fleets.value = updated
        return true
    }

    fun removeFleet(name: String) {
        val updated = _fleets.value.orEmpty().filterNot { it == name }
        store.save(updated)
        _fleets.value = updated
    }

    private companion object {
        const val MAX_NAME_LENGTH = 60
    }
}
