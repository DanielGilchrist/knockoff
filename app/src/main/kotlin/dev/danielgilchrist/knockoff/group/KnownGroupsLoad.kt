package dev.danielgilchrist.knockoff.group

import dev.danielgilchrist.knockoff.domain.group.KnownGroups

sealed interface KnownGroupsLoad {
    val known: KnownGroups
        get() =
            when (this) {
                is Loaded -> groups
                is Corrupt, is ReadFailed -> KnownGroups.NONE
            }

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
