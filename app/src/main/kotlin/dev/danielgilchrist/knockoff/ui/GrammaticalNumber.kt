package dev.danielgilchrist.knockoff.ui

enum class GrammaticalNumber {
    One,
    Many,
    ;

    companion object {
        fun of(count: Int): GrammaticalNumber =
            when (count) {
                1 -> One
                else -> Many
            }
    }
}
