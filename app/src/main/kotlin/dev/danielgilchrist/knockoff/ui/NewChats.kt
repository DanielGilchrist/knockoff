package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.group.KnownGroupsLoad

sealed interface NewChats {
    data object Loading : NewChats

    data class Ready(
        val chats: List<Target.Chat>,
    ) : NewChats

    data class Failed(
        val detail: String,
    ) : NewChats

    companion object {
        fun of(
            settings: SettingsState,
            seen: SeenChatsLoad,
            decisions: ChatDecisionsLoad,
            groups: KnownGroupsLoad,
        ): NewChats =
            when (settings) {
                SettingsState.Loading, is SettingsState.Failed -> {
                    Loading
                }

                is SettingsState.Ready -> {
                    when (seen) {
                        is SeenChatsLoad.Loaded -> grouped(settings, seen, decisions, groups)
                        is SeenChatsLoad.Corrupt -> Failed(seen.detail)
                        is SeenChatsLoad.ReadFailed -> Failed(seen.detail)
                    }
                }
            }

        private fun grouped(
            settings: SettingsState.Ready,
            seen: SeenChatsLoad.Loaded,
            decisions: ChatDecisionsLoad,
            groups: KnownGroupsLoad,
        ): NewChats =
            when (groups) {
                is KnownGroupsLoad.Loaded -> undecided(settings, seen, decisions, groups.groups)
                is KnownGroupsLoad.Corrupt -> Failed(groups.detail)
                is KnownGroupsLoad.ReadFailed -> Failed(groups.detail)
            }

        private fun undecided(
            settings: SettingsState.Ready,
            seen: SeenChatsLoad.Loaded,
            decisions: ChatDecisionsLoad,
            groups: KnownGroups,
        ): NewChats =
            when (decisions) {
                is ChatDecisionsLoad.Loaded -> {
                    Ready(
                        seen.seen.undecided(settings.settings.rules, decisions.decisions, groups),
                    )
                }

                is ChatDecisionsLoad.Corrupt -> {
                    Ready(
                        seen.seen.undecided(settings.settings.rules, ChatDecisions.NONE, groups),
                    )
                }

                is ChatDecisionsLoad.ReadFailed -> {
                    Failed(decisions.detail)
                }
            }
    }
}
