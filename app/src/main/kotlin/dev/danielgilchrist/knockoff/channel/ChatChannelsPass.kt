package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.rule.Target

internal data class ChatChannelsPass(
    val creation: ChatChannelsCreation,
    val waiting: Set<Target.Chat>,
)
