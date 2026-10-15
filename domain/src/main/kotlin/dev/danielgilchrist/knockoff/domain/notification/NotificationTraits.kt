package dev.danielgilchrist.knockoff.domain.notification

@ConsistentCopyVisibility
data class NotificationTraits private constructor(
    val values: Set<NotificationTrait>,
) {
    companion object {
        fun parse(traits: Set<NotificationTrait>): NotificationTraitsParse =
            when {
                traits.isEmpty() -> NotificationTraitsParse.None
                else -> NotificationTraitsParse.Present(NotificationTraits(traits.toSet()))
            }
    }
}
