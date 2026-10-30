package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar

data class HolidaysActions(
    val onAllowCalendar: () -> Unit,
    val onOpenAppSettings: () -> Unit,
    val onChoose: (HolidayCalendar) -> Unit,
    val onBack: () -> Unit,
)
