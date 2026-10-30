package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.dayoff.CalendarId
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayRegion
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayRegionParse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface StoredHolidayCalendar {
    fun parse(): Parsed<HolidayCalendar>

    @Serializable
    @SerialName("none")
    data object None : StoredHolidayCalendar {
        override fun parse(): Parsed<HolidayCalendar> = Parsed.Ok(HolidayCalendar.None)
    }

    @Serializable
    @SerialName("chosen")
    data class Chosen(
        private val calendar: Long,
        private val regions: List<String>,
    ) : StoredHolidayCalendar {
        override fun parse(): Parsed<HolidayCalendar> =
            regions
                .parseEach("region") { raw ->
                    when (val parse = HolidayRegion.parse(raw)) {
                        is HolidayRegionParse.Valid -> Parsed.Ok(parse.region)
                        HolidayRegionParse.Blank -> Parsed.Failed("blank")
                    }
                }.then { Parsed.Ok(HolidayCalendar.Chosen(CalendarId(calendar), it.toSet())) }
    }

    companion object {
        fun from(calendar: HolidayCalendar): StoredHolidayCalendar =
            when (calendar) {
                HolidayCalendar.None -> None
                is HolidayCalendar.Chosen -> Chosen(calendar.calendar.value, calendar.regions.map { it.value })
            }
    }
}
