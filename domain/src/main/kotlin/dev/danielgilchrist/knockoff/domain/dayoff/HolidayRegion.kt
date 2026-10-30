package dev.danielgilchrist.knockoff.domain.dayoff

@JvmInline
value class HolidayRegion private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String): HolidayRegionParse =
            when {
                raw.isBlank() -> HolidayRegionParse.Blank
                else -> HolidayRegionParse.Valid(HolidayRegion(raw.trim()))
            }
    }
}
