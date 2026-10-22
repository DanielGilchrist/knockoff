package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.settings.AppliedRules

interface AppliedRulesKeeper {
    suspend fun load(): AppliedRulesLoad

    suspend fun save(baseline: Baseline.Known<AppliedRules>): AppliedRulesSave
}
