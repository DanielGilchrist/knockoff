package dev.danielgilchrist.knockoff.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PairedDeviceTest {
    private val address = BluetoothAddress("AA:AA:AA:AA:AA:01")

    @Test
    fun `a device is named by its alias`() {
        assertEquals(PairedDevice("Pixel Buds", address), PairedDevice.of("Pixel Buds", address))
    }

    @Test
    fun `a device without an alias is named by its address`() {
        assertEquals(PairedDevice("AA:AA:AA:AA:AA:01", address), PairedDevice.of(null, address))
        assertEquals(PairedDevice("AA:AA:AA:AA:AA:01", address), PairedDevice.of(" ", address))
    }
}
