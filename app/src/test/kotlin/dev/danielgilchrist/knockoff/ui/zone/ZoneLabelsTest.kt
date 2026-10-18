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
        assertEquals("App default", RuleZone.Default.label())
        assertEquals("Device time zone", RuleZone.Chosen(ZoneChoice.Device).label())
        assertEquals("Etc/UTC (UTC+00:00)", RuleZone.Chosen(utc).label())
        assertEquals("Device time zone", ZoneChoice.Device.label())
    }

    @Test
    fun `a rule offers app default and device first, settings only device`() {
        assertEquals(listOf(RuleZone.Default, RuleZone.Chosen(ZoneChoice.Device)), RULE_ZONES.map { it.value })
        assertEquals(ZoneChoice.Device, DEVICE_ZONE.value)
    }
}
