package dev.danielgilchrist.knockoff.group

import dev.danielgilchrist.knockoff.domain.group.KnownGroups

sealed interface KnownGroupsLoad {
    data class Loaded(
        val groups: KnownGroups,
    ) : KnownGroupsLoad

    data class Corrupt(
        val detail: String,
    ) : KnownGroupsLoad

    data class ReadFailed(
        val detail: String,
    ) : KnownGroupsLoad
}
