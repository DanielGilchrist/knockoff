package dev.danielgilchrist.knockoff.domain.settings

internal sealed interface Parsed<out T> {
    data class Ok<T>(
        val value: T,
    ) : Parsed<T>

    data class Failed(
        val detail: String,
    ) : Parsed<Nothing>

    fun <R> then(next: (T) -> Parsed<R>): Parsed<R> =
        when (this) {
            is Ok -> next(value)
            is Failed -> this
        }

    fun within(context: String): Parsed<T> =
        when (this) {
            is Ok -> this
            is Failed -> Failed("$context: $detail")
        }
}

internal fun <T, R> List<T>.parseEach(
    label: String,
    parse: (T) -> Parsed<R>,
): Parsed<List<R>> =
    withIndex().fold<IndexedValue<T>, Parsed<List<R>>>(Parsed.Ok(emptyList())) { parsed, (index, item) ->
        parsed.then { done -> parse(item).within("$label ${index + 1}").then { Parsed.Ok(done + it) } }
    }
