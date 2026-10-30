package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.LeaveParse
import java.time.LocalDate

sealed interface LeaveChoice {
    data object Unpicked : LeaveChoice

    data class Picked(
        val leave: Leave,
    ) : LeaveChoice

    companion object {
        fun of(
            first: LocalDate?,
            last: LocalDate?,
        ): LeaveChoice =
            when (first) {
                null -> Unpicked
                else -> picked(first, last ?: first)
            }

        private fun picked(
            first: LocalDate,
            last: LocalDate,
        ): LeaveChoice =
            when (val parse = Leave.parse(first, last)) {
                is LeaveParse.Valid -> Picked(parse.leave)
                LeaveParse.EndsBeforeStart -> Unpicked
            }
    }
}
