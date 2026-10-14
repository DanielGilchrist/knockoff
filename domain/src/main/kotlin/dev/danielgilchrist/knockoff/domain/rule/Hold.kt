package dev.danielgilchrist.knockoff.domain.rule

internal data class Hold(
    val rule: RuleName,
    val release: Release,
)
