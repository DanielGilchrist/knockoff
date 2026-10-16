package dev.danielgilchrist.knockoff.domain.schedule

sealed interface ScheduleParse {
    data class Valid(
        val schedule: Schedule,
    ) : ScheduleParse

    data object Empty : ScheduleParse

    data object NothingLeftAfterBreaks : ScheduleParse
}
