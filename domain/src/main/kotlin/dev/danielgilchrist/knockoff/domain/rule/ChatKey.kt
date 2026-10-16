package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.PackageName

data class ChatKey(
    val packageName: PackageName,
    val id: ConversationId,
)
