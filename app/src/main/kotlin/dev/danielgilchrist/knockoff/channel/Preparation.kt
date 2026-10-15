package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.channel.ChannelPlan
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import java.time.ZonedDateTime

internal sealed interface Preparation {
    data class Ready(
        val rules: RuleSet,
        val plan: ChannelPlan,
        val now: ZonedDateTime,
    ) : Preparation

    data class Unavailable(
        val detail: String,
    ) : Preparation

    data class Failed(
        val detail: String,
    ) : Preparation
}
