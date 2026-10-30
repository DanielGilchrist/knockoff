package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.holiday.HolidaysState
import dev.danielgilchrist.knockoff.ui.preferences.CalendarChoices

internal data class ClosedState(
    val setup: Setup,
    val paired: PairedDevices,
    val settings: SettingsState,
    val saveStatus: SaveStatus,
    val preferencesStatus: SaveStatus,
    val newChats: NewChatsView,
    val apps: InstalledApps,
    val calendars: CalendarChoices,
    val holidays: HolidaysState,
)
