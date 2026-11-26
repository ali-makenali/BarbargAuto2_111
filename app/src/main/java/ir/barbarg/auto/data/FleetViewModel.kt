package ir.barbarg.auto.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class FleetViewModel(application: Application) : AndroidViewModel(application) {
    private val store = FleetStore(application)
    private val _fleets = MutableLiveData(store.all())
    val fleets: LiveData<List<FleetRecord>> = _fleets

    fun add(record: FleetRecord): Boolean {
        if (!store.insert(record)) return false
        refresh()
        return true
    }

    fun update(previousDriverNationalId: String, record: FleetRecord): Boolean {
        if (!store.update(previousDriverNationalId, record)) return false
        refresh()
        return true
    }

    fun remove(driverNationalId: String) {
        store.delete(driverNationalId)
        refresh()
    }

    fun search(query: String): List<FleetRecord> = store.search(query)

    private fun refresh() {
        _fleets.value = store.all()
    }
}
