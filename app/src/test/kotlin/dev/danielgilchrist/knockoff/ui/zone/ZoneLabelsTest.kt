package dev.danielgilchrist.knockoff.ui.zone

import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.ui.SettingsOrigin
import dev.danielgilchrist.knockoff.ui.SettingsState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class ZoneLabelsTest {
    private val utc = ZoneChoice.Fixed(ZoneId.of("Etc/UTC"))

    @Test
    fun `zones are labelled by how they are chosen`() {
        val device = DefaultZone.Known(ZoneChoice.Device)

        val london = ZoneId.of("Europe/London")

        assertEquals("Same as your phone (Europe/London)", RuleZone.Default.label(device, london))
        assertEquals("Default (Etc/UTC (UTC+00:00))", RuleZone.Default.label(DefaultZone.Known(utc), london))
        assertEquals("Default", RuleZone.Default.label(DefaultZone.Unknown, london))
        assertEquals("Same as your phone", RuleZone.Chosen(ZoneChoice.Device).label(device, london))
        assertEquals("Etc/UTC (UTC+00:00)", RuleZone.Chosen(utc).label(device, london))
        assertEquals("Same as your phone", ZoneChoice.Device.label())
    }

    @Test
    fun `the default zone is known once settings are ready`() {
        assertEquals(DefaultZone.Unknown, DefaultZone.of(SettingsState.Loading))
        assertEquals(DefaultZone.Unknown, DefaultZone.of(SettingsState.Failed("x")))
        assertEquals(
            DefaultZone.Known(Settings.DEFAULT.rules.defaultZone),
            DefaultZone.of(SettingsState.Ready(Settings.DEFAULT, SettingsOrigin.Saved)),
        )
    }

    @Test
    fun `a rule offers app default and device first, settings only device`() {
        assertEquals(listOf(RuleZone.Default, RuleZone.Chosen(ZoneChoice.Device)), RULE_ZONES.map { it.value })
        assertEquals(ZoneChoice.Device, DEVICE_ZONE.value)
    }
}
