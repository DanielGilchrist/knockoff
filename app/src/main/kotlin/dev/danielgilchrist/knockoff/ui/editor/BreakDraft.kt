package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.schedule.Break
import dev.danielgilchrist.knockoff.domain.schedule.BreakName
import dev.danielgilchrist.knockoff.domain.schedule.BreakNameParse
import java.time.LocalTime

data class BreakDraft(
    val name: String,
    val window: WindowDraft,
) {
    companion object {
        val LUNCH: BreakDraft
            get() =
                BreakDraft(
                    "Lunch",
                    WindowDraft.WORKING_HOURS.copy(start = LocalTime.NOON, end = LocalTime.of(13, 0)),
                )

        fun from(`break`: Break): BreakDraft = BreakDraft(`break`.name.value, WindowDraft.from(`break`.window))
    }

    fun parse(number: Int): Checked<Break> {
        val breakName: Checked<BreakName> =
            when (val parse = BreakName.parse(name)) {
                is BreakNameParse.Valid -> Checked.Ok(parse.name)
                BreakNameParse.Blank -> Checked.Problems(listOf(DraftProblem.BlankBreakName(number)))
            }

        return breakName.and(window.parse(DraftProblem.BreakWithoutDays(number)), ::Break)
    }
}
