package dev.danielgilchrist.knockoff.domain.schedule

internal data class WeekSpan(
    val startNano: Long,
    val endNano: Long,
) {
    companion object {
        const val NANOS_PER_DAY = 86_400_000_000_000L
        const val NANOS_PER_WEEK = 7 * NANOS_PER_DAY
    }

    fun fromPreviousWeek(): WeekSpan = WeekSpan(startNano - NANOS_PER_WEEK, endNano - NANOS_PER_WEEK)
}
