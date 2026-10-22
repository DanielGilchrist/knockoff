package dev.danielgilchrist.knockoff.domain.change

sealed interface Baseline<out T> {
    data object Unknown : Baseline<Nothing>

    data class Known<T>(
        val reconciled: T,
        val since: List<T>,
    ) : Baseline<T> {
        companion object {
            fun <T> of(reconciled: T): Known<T> = Known(reconciled, emptyList())
        }

        val candidates: List<T> get() = listOf(reconciled) + since

        fun skipped(to: T): Known<T> = if (to in candidates) this else copy(since = since + to)

        fun <R> map(transform: (T) -> R): Known<R> = Known(transform(reconciled), since.map(transform))
    }
}

fun <T> Baseline<T>.stepTo(value: T): Step<T> =
    when (this) {
        Baseline.Unknown -> Step.First(value)
        is Baseline.Known -> if (since.isEmpty() && reconciled == value) Step.Same(value) else Step.Moved(this, value)
    }
