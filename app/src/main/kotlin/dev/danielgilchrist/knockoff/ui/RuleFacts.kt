package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.holiday.HolidaysState

internal data class RuleFacts(
    val choices: TargetChoices,
    val saveStatus: SaveStatus,
    val holidays: HolidaysState,
)
