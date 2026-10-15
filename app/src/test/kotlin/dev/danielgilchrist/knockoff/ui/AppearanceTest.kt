package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.Theme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppearanceTest {
    @Test
    fun `nothing is themed until settings load`() {
        assertEquals(Appearance.Pending, Appearance.of(SettingsState.Loading))
    }

    @Test
    fun `loaded settings choose the theme`() {
        val dark = SettingsState.Ready(Settings(Settings.DEFAULT.rules, Theme.Dark), SettingsOrigin.Saved)

        assertEquals(Appearance.Themed(Theme.Dark), Appearance.of(dark))
    }

    @Test
    fun `settings that fail to load follow the system`() {
        assertEquals(Appearance.Themed(Theme.System), Appearance.of(SettingsState.Failed("broken")))
    }
}
