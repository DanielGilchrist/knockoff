package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.group.KnownGroups

data class TargetChoices(
    val apps: InstalledApps,
    val chats: SeenChatsState,
    val groups: KnownGroups,
    val link: LinkState,
)
