package dev.danielgilchrist.knockoff.ui.home

internal sealed interface ChatInitial {
    data object None : ChatInitial

    data class Letter(
        val text: String,
    ) : ChatInitial

    companion object {
        fun of(name: String): ChatInitial =
            name
                .codePoints()
                .filter(Character::isLetterOrDigit)
                .findFirst()
                .let { first ->
                    when {
                        first.isPresent -> Letter(String(Character.toChars(first.asInt)).uppercase())
                        else -> None
                    }
                }
    }
}
