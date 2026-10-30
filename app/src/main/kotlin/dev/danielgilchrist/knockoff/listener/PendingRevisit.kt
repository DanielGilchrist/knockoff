package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.rule.Evaluation

internal data class PendingRevisit(
    val gone: GroupEntry,
    val revisit: SummaryRevisit,
    val evaluation: Evaluation,
)
