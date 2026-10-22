package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.rule.Evaluation

internal sealed interface Applied {
    data object Pending : Applied

    data class Active(
        val evaluation: Evaluation,
    ) : Applied
}
