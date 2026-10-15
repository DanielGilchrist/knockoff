package dev.danielgilchrist.knockoff.domain.change

sealed interface Change<out T> {
    data object Pending : Change<Nothing>

    data class Initial<T>(
        val value: T,
    ) : Change<T>

    data class Changed<T>(
        val before: T,
        val after: T,
    ) : Change<T>
}

fun <T> Change<T>.next(value: T): Change<T> =
    when (this) {
        Change.Pending -> Change.Initial(value)
        is Change.Initial -> Change.Changed(before = this.value, after = value)
        is Change.Changed -> Change.Changed(before = after, after = value)
    }
