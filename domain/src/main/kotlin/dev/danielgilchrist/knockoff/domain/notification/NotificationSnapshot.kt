package dev.danielgilchrist.knockoff.domain.notification

class NotificationSnapshot(
    private val key: NotificationKey,
    private val packageName: PackageName,
    private val channelId: String?,
    private val traits: Set<NotificationTrait>,
) {
    fun parse(): PostedNotification {
        val channel =
            when (channelId) {
                null -> Channel.Unnamed
                else -> Channel.Named(ChannelId(channelId))
            }
        val origin = Origin(packageName, channel)

        return when {
            traits.isEmpty() -> PostedNotification.Snoozable(key, origin)
            else -> PostedNotification.Unsnoozable(key, origin, traits)
        }
    }
}
