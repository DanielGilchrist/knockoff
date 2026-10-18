package dev.danielgilchrist.knockoff.ui.editor

sealed interface WindowPosition {
    data object Only : WindowPosition

    data class OneOf(
        val number: Int,
    ) : WindowPosition

    companion object {
        fun of(
            index: Int,
            count: Int,
        ): WindowPosition =
            when (count) {
                1 -> Only
                else -> OneOf(index + 1)
            }
    }
}
