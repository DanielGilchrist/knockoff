package dev.danielgilchrist.knockoff.domain.change

sealed interface Step<out T> {
    data class First<T>(
        val value: T,
    ) : Step<T>

    data class Same<T>(
        val value: T,
    ) : Step<T>

    data class Moved<T>(
        val from: Baseline.Known<T>,
        val to: T,
    ) : Step<T>
}
