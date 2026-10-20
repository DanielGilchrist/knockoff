package dev.danielgilchrist.knockoff.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceLinkTest {
    @Test
    fun `a link is named by the device's name`() {
        assertEquals(
            DeviceLink.Linked("Jabra Evolve 75"),
            DeviceLink.linked("Jabra Evolve 75", BluetoothAddress("30:50:75:0e:29:8d")),
        )
    }

    @Test
    fun `an unnamed link falls back to the address`() {
        assertEquals(
            DeviceLink.Linked("30:50:75:0E:29:8D"),
            DeviceLink.linked(null, BluetoothAddress("30:50:75:0e:29:8d")),
        )
        assertEquals(
            DeviceLink.Linked("30:50:75:0E:29:8D"),
            DeviceLink.linked("", BluetoothAddress("30:50:75:0e:29:8d")),
        )
    }

    @Test
    fun `a link with neither a name nor an address is still described`() {
        assertEquals(DeviceLink.Linked("a device"), DeviceLink.linked(null, null))
    }
}
