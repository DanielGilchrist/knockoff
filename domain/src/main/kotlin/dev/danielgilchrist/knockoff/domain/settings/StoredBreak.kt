package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.schedule.Break
import dev.danielgilchrist.knockoff.domain.schedule.BreakName
import dev.danielgilchrist.knockoff.domain.schedule.BreakNameParse
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredBreak(
    private val name: String,
    private val window: StoredWindow,
) {
    companion object {
        fun from(`break`: Break): StoredBreak = StoredBreak(`break`.name.value, StoredWindow.from(`break`.window))
    }

    fun parse(): Parsed<Break> =
        when (val parse = BreakName.parse(name)) {
            BreakNameParse.Blank -> Parsed.Failed("blank break name")
            is BreakNameParse.Valid -> window.parse().then { Parsed.Ok(Break(parse.name, it)) }
        }
}
