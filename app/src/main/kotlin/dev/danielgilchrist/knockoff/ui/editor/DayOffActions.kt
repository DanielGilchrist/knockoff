package dev.danielgilchrist.knockoff.ui.editor

data class DayOffActions(
    val onTakeTodayOff: () -> Unit,
    val onEndDayOff: () -> Unit,
    val onEditDaysOff: () -> Unit,
)
