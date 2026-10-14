package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule

sealed interface EditTarget {
    data object New : EditTarget

    data class Existing(
        val rule: Rule,
    ) : EditTarget
}
