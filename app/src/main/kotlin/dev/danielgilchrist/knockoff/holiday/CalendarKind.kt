package dev.danielgilchrist.knockoff.holiday

enum class CalendarKind {
    Holidays,
    Other,
    ;

    companion object {
        private const val GOOGLE_HOLIDAYS = "#holiday@group.v.calendar.google.com"

        fun of(owner: String): CalendarKind =
            when {
                owner.endsWith(GOOGLE_HOLIDAYS) -> Holidays
                else -> Other
            }
    }
}
