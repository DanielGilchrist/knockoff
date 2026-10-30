package dev.danielgilchrist.knockoff.domain.dayoff

sealed interface HolidayRegionParse {
    data class Valid(
        val region: HolidayRegion,
    ) : HolidayRegionParse

    data object Blank : HolidayRegionParse
}
