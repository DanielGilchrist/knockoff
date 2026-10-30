package dev.danielgilchrist.knockoff.system

sealed interface SystemCall<out T> {
    data class Done<T>(
        val value: T,
    ) : SystemCall<T>

    sealed interface Failed : SystemCall<Nothing> {
        val detail: String
    }

    data class Refused(
        override val detail: String,
    ) : Failed

    data class Unavailable(
        override val detail: String,
    ) : Failed
}
