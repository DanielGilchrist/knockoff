package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.rule.Target

data class ChatChannelPlan(
    val create: Set<Target.Chat>,
    val newlyMissing: Set<Target.Chat>,
    val waiting: Set<Target.Chat>,
    val next: ChatChannelAttempts,
)
