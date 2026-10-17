package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.rule.Target

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
        ): NewChats =
            when (settings) {
                SettingsState.Loading, is SettingsState.Failed -> {
                    Loading
                }

                is SettingsState.Ready -> {
                    when (seen) {
                        is SeenChatsLoad.Loaded -> undecided(settings, seen, decisions)
                        is SeenChatsLoad.Corrupt -> Failed(seen.detail)
                        is SeenChatsLoad.ReadFailed -> Failed(seen.detail)
                    }
                }
            }

        private fun undecided(
            settings: SettingsState.Ready,
            seen: SeenChatsLoad.Loaded,
            decisions: ChatDecisionsLoad,
        ): NewChats =
            when (decisions) {
                is ChatDecisionsLoad.Loaded -> Ready(seen.seen.undecided(settings.settings.rules, decisions.decisions))
                is ChatDecisionsLoad.Corrupt -> Ready(seen.seen.undecided(settings.settings.rules, ChatDecisions.NONE))
                is ChatDecisionsLoad.ReadFailed -> Failed(decisions.detail)
            }
    }
}
