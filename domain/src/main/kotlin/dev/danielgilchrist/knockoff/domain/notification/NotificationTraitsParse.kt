package dev.danielgilchrist.knockoff.domain.notification

sealed interface NotificationTraitsParse {
    data class Present(
        val traits: NotificationTraits,
    ) : NotificationTraitsParse

    data object None : NotificationTraitsParse
}
