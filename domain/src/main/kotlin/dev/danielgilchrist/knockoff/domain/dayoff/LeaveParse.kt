package dev.danielgilchrist.knockoff.domain.dayoff

sealed interface LeaveParse {
    data class Valid(
        val leave: Leave,
    ) : LeaveParse

    data object EndsBeforeStart : LeaveParse
}
