package dev.danielgilchrist.knockoff.ui

internal data class ClosedState(
    val setup: Setup,
    val paired: PairedDevices,
    val settings: SettingsState,
    val saveStatus: SaveStatus,
    val preferencesStatus: SaveStatus,
    val newChats: NewChatsView,
    val apps: InstalledApps,
)
