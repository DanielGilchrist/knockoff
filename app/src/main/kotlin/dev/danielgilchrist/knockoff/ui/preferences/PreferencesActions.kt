package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.domain.settings.Theme
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.ui.SystemActions

data class PreferencesActions(
    val system: SystemActions,
    val onDefaultZoneChange: (ZoneChoice) -> Unit,
    val onThemeChange: (Theme) -> Unit,
    val onOpenLink: () -> Unit,
    val onBack: () -> Unit,
)
