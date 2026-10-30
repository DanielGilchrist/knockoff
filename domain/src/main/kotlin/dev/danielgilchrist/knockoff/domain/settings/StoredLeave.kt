package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.LeaveParse
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Serializable
internal data class StoredLeave(
    private val first: String,
    private val last: String,
) {
    companion object {
        fun from(leave: Leave): StoredLeave = StoredLeave(leave.first.toString(), leave.last.toString())
    }

    fun parse(): Parsed<Leave> =
        date(first).then { firstDate ->
            date(last).then { lastDate ->
                when (val parse = Leave.parse(firstDate, lastDate)) {
                    is LeaveParse.Valid -> Parsed.Ok(parse.leave)
                    LeaveParse.EndsBeforeStart -> Parsed.Failed("ends on $last before it starts on $first")
                }
            }
        }

    private fun date(text: String): Parsed<LocalDate> =
        try {
            Parsed.Ok(LocalDate.parse(text))
        } catch (_: DateTimeParseException) {
            Parsed.Failed("\"$text\" is not a date")
        }
}
