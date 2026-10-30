package dev.danielgilchrist.knockoff.holiday

internal sealed interface CalendarQuery<out T> {
    data class Rows<T>(
        val value: T,
    ) : CalendarQuery<T>

    data object NoPermission : CalendarQuery<Nothing>

    data class Failed(
        val detail: String,
    ) : CalendarQuery<Nothing>
}
