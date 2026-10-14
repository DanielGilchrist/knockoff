package dev.danielgilchrist.knockoff.ui.editor

sealed interface Checked<out T> {
    data class Ok<T>(
        val value: T,
    ) : Checked<T>

    data class Problems(
        val problems: List<DraftProblem>,
    ) : Checked<Nothing>

    fun <U, R> and(
        other: Checked<U>,
        combine: (T, U) -> R,
    ): Checked<R> =
        when (this) {
            is Ok -> {
                when (other) {
                    is Ok -> Ok(combine(value, other.value))
                    is Problems -> other
                }
            }

            is Problems -> {
                when (other) {
                    is Ok -> this
                    is Problems -> Problems(problems + other.problems)
                }
            }
        }
}

fun <T> List<Checked<T>>.all(): Checked<List<T>> {
    val start: Checked<List<T>> = Checked.Ok(emptyList())

    return fold(start) { checked, next -> checked.and(next, List<T>::plus) }
}
