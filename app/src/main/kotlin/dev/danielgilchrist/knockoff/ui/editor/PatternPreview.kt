package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.rule.Target

sealed interface PatternPreview {
    data object Empty : PatternPreview

    data class Invalid(
        val detail: String,
    ) : PatternPreview

    data class Valid(
        val target: Target.Pattern,
    ) : PatternPreview
}
