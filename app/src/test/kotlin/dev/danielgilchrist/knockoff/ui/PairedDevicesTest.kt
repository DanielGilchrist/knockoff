package dev.danielgilchrist.knockoff.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PairedDevicesTest {
    private fun device(
        name: String,
        address: String,
    ) = PairedDevice(name, BluetoothAddress(address))

    @Test
    fun `paired devices are listed by name ignoring case`() {
        val buds = device("Pixel Buds", "AA:AA:AA:AA:AA:01")
        val car = device("car", "AA:AA:AA:AA:AA:02")
        val watch = device("Watch", "AA:AA:AA:AA:AA:03")

        assertEquals(PairedDevices.Listed(listOf(car, buds, watch)), PairedDevices.listed(listOf(watch, buds, car)))
    }

    @Test
    fun `a refused permission can be asked again until Android blocks it`() {
        assertEquals(PairedDevices.NotAllowed, PairedDevices.refused(PermissionAsk.Askable))
        assertEquals(PairedDevices.Blocked, PairedDevices.refused(PermissionAsk.Blocked))
    }
}
