package dev.danielgilchrist.knockoff.domain.notification

data class Origin(
    val packageName: PackageName,
    val channel: Channel,
    val conversation: Conversation,
)
