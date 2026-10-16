package dev.danielgilchrist.knockoff.domain.notification

class NotificationSnapshot(
    private val key: NotificationKey,
    private val packageName: PackageName,
    private val channelId: String?,
    private val shortcutId: String?,
    private val traits: Set<NotificationTrait>,
) {
    fun parse(): PostedNotification {
        val channel =
            when (channelId) {
                null -> Channel.Unnamed
                else -> Channel.Named(ChannelId(channelId))
            }
        val conversation =
            when {
                shortcutId.isNullOrBlank() -> Conversation.None
                else -> Conversation.Known(ConversationId(shortcutId))
            }
        val origin = Origin(packageName, channel, conversation)

        return when (val parse = NotificationTraits.parse(traits)) {
            NotificationTraitsParse.None -> PostedNotification.Snoozable(key, origin)
            is NotificationTraitsParse.Present -> PostedNotification.Unsnoozable(key, origin, parse.traits)
        }
    }
}
