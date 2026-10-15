package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.domain.settings.Theme
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice

data class PreferencesActions(
    val onDefaultZoneChange: (ZoneChoice) -> Unit,
    val onThemeChange: (Theme) -> Unit,
    val onBack: () -> Unit,
)
