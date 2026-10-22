package dev.danielgilchrist.knockoff.domain.settings

internal sealed interface StoredAppliedRulesDocument {
    fun toBaseline(): AppliedRulesDecode
}
