package dev.danielgilchrist.knockoff.ui.zone

import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class ZoneLabelsTest {
    private val utc = ZoneChoice.Fixed(ZoneId.of("Etc/UTC"))

    @Test
    fun `zones are labelled by how they are chosen`() {
        val device = ZoneChoice.Device

        val london = ZoneId.of("Europe/London")

        assertEquals("Same as your phone (Europe/London)", RuleZone.Default.label(device, london))
        assertEquals("Default (Etc/UTC (UTC+00:00))", RuleZone.Default.label(utc, london))
        assertEquals("Same as your phone", RuleZone.Chosen(ZoneChoice.Device).label(device, london))
        assertEquals("Etc/UTC (UTC+00:00)", RuleZone.Chosen(utc).label(device, london))
        assertEquals("Same as your phone", ZoneChoice.Device.label())
    }

    @Test
    fun `a rule offers app default and device first, settings only device`() {
        assertEquals(listOf(RuleZone.Default, RuleZone.Chosen(ZoneChoice.Device)), RULE_ZONES.map { it.value })
        assertEquals(ZoneChoice.Device, DEVICE_ZONE.value)
    }
}
