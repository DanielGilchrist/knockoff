package dev.danielgilchrist.knockoff.ui.components

sealed interface NoticeAction {
    data object None : NoticeAction

    data class Button(
        val label: String,
        val onClick: () -> Unit,
    ) : NoticeAction
}
