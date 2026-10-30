package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.domain.dayoff.CalendarId
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.Theme
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import dev.danielgilchrist.knockoff.settings.SettingsStore
import dev.danielgilchrist.knockoff.ui.SaveStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.ZoneId

class PreferencesEditorTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `the theme and default zone are saved`() =
        runTest {
            val store = SettingsStore(settingsFile())
            val editor = editor(store)
            val tokyo = ZoneChoice.Fixed(ZoneId.of("Asia/Tokyo"))

            val expected =
                SettingsLoad.Loaded(
                    Settings(Settings.DEFAULT.rules.withDefaultZone(tokyo), Theme.Dark, HolidayCalendar.None),
                )

            editor.setTheme(Theme.Dark)
            store.loads.first { it is SettingsLoad.Loaded && it.settings.theme == Theme.Dark }
            editor.setDefaultZone(tokyo)

            store.loads.first { it == expected }
        }

    @Test
    fun `the holiday calendar is saved`() =
        runTest {
            val store = SettingsStore(settingsFile())
            val chosen = HolidayCalendar.Chosen(CalendarId(3), emptySet())

            editor(store).setHolidays(chosen)

            store.loads.first { it is SettingsLoad.Loaded && it.settings.holidayCalendar == chosen }
        }

    @Test
    fun `a later save clears the failure`() =
        runTest {
            settingsFile().writeText("not json")
            val editor = editor(SettingsStore(settingsFile()))
            editor.setTheme(Theme.Light)
            editor.saveStatus.first { it is SaveStatus.Failed }
            settingsFile().writeText(Settings.DEFAULT.toJson())

            editor.setTheme(Theme.Light)

            editor.saveStatus.first { it == SaveStatus.Idle }
        }

    private fun TestScope.editor(store: SettingsStore): PreferencesEditor = PreferencesEditor(store, backgroundScope)

    private fun settingsFile(): File = File(directory, "settings.json")
}
