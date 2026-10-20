package dev.danielgilchrist.knockoff.domain.notification

sealed interface GroupRole {
    data object Member : GroupRole

    data class Summary(
        val showing: List<PostedNotification>,
    ) : GroupRole
}
