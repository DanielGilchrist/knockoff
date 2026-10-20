package dev.danielgilchrist.knockoff.listener

internal sealed interface ActiveGroups {
    data class Read(
        val entries: List<GroupEntry>,
    ) : ActiveGroups

    data class Unavailable(
        val detail: String,
    ) : ActiveGroups
}
