package ir.barbarg.auto.data

/**
 * One driver-to-vehicle assignment imported from the six-column fleet sheet.
 * Password is decrypted only when a record is read into memory.
 */
data class FleetRecord(
    val driverNationalId: String,
    val driverFullName: String,
    val driverMobile: String,
    val driverPassword: String,
    val vehiclePlate: String,
    val vehicleCapacity: String
)
