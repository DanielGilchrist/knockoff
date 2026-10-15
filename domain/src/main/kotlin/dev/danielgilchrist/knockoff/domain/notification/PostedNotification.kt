package dev.danielgilchrist.knockoff.domain.notification

sealed interface PostedNotification {
    val key: NotificationKey
    val origin: Origin

    @ConsistentCopyVisibility
    data class Snoozable internal constructor(
        override val key: NotificationKey,
        override val origin: Origin,
    ) : PostedNotification

    @ConsistentCopyVisibility
    data class Unsnoozable internal constructor(
        override val key: NotificationKey,
        override val origin: Origin,
        val traits: NotificationTraits,
    ) : PostedNotification

    fun describe(): String {
        val channel =
            when (val channel = origin.channel) {
                is Channel.Named -> channel.id.value
                Channel.Unnamed -> "(none)"
            }

        return "key=${key.value} package=${origin.packageName.value} channel=$channel"
    }
}
