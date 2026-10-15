package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShownScreenTest {
    private val ready = SettingsState.Ready(Settings.DEFAULT, SettingsOrigin.Saved)

    @Test
    fun `settings show once they're loaded`() {
        assertEquals(ShownScreen.Preferences(Settings.DEFAULT), ShownScreen.of(Screen.Preferences, ready))
        assertEquals(ShownScreen.Home, ShownScreen.of(Screen.Home, ready))
    }

    @Test
    fun `home shows while settings are loading or failed`() {
        assertEquals(ShownScreen.Home, ShownScreen.of(Screen.Preferences, SettingsState.Loading))
        assertEquals(ShownScreen.Home, ShownScreen.of(Screen.Preferences, SettingsState.Failed("broken")))
    }

    @Test
    fun `each shown screen names the screen to remember`() {
        assertEquals(Screen.Home, ShownScreen.Home.screen)
        assertEquals(Screen.Preferences, ShownScreen.Preferences(Settings.DEFAULT).screen)
    }
}
