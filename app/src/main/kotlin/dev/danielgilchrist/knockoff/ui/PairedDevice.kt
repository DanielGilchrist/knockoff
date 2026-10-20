package dev.danielgilchrist.knockoff.ui

data class PairedDevice(
    val name: String,
    val address: BluetoothAddress,
) {
    companion object {
        fun of(
            alias: String?,
            address: BluetoothAddress,
        ): PairedDevice = PairedDevice(alias?.takeIf { it.isNotBlank() } ?: address.value, address)
    }
}
