package dev.danielgilchrist.knockoff.ui

@ConsistentCopyVisibility
data class HeldPhrase private constructor(
    val text: String,
    val number: GrammaticalNumber,
    val opening: String,
) {
    fun followedBy(
        rest: List<HeldPhrase>,
        join: (String, List<String>) -> String,
    ): HeldPhrase {
        val texts = rest.map(HeldPhrase::text)
        val joined =
            when {
                rest.isEmpty() -> number
                else -> GrammaticalNumber.Many
            }

        return HeldPhrase(join(text, texts), joined, join(opening, texts))
    }

    companion object {
        fun of(
            text: String,
            number: GrammaticalNumber,
        ): HeldPhrase = HeldPhrase(text, number, text)

        fun some(things: String): HeldPhrase = HeldPhrase("some $things", GrammaticalNumber.Many, "Some $things")
    }
}
