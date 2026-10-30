package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.rule.Evaluation

internal interface HeldNotifications {
    fun sweep(evaluation: Evaluation)

    suspend fun recheckSnoozed(
        before: Baseline.Known<Evaluation>,
        after: Evaluation,
    ): RecheckStart
}
