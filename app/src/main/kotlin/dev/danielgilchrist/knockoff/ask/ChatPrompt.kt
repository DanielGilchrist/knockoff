package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.chat.ChatQuestion
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.group.KnownGroupsLoad
import dev.danielgilchrist.knockoff.settings.SettingsLoad

sealed interface ChatPrompt {
    data object Unknown : ChatPrompt

    data class Ready(
        val questions: List<ChatQuestion>,
    ) : ChatPrompt

    companion object {
        fun of(
            settings: SettingsLoad,
            seen: SeenChatsLoad,
            decisions: ChatDecisionsLoad,
            groups: KnownGroupsLoad,
        ): ChatPrompt =
            when (settings) {
                is SettingsLoad.Failure -> {
                    Unknown
                }

                is SettingsLoad.Usable -> {
                    when (seen) {
                        is SeenChatsLoad.Loaded -> grouped(settings.settings.rules, seen.seen, decisions, groups)
                        is SeenChatsLoad.Corrupt, is SeenChatsLoad.ReadFailed -> Unknown
                    }
                }
            }

        private fun grouped(
            rules: RuleSet,
            seen: SeenChats,
            decisions: ChatDecisionsLoad,
            groups: KnownGroupsLoad,
        ): ChatPrompt =
            when (groups) {
                is KnownGroupsLoad.Loaded -> decided(rules, seen, decisions, groups.groups)
                is KnownGroupsLoad.Corrupt, is KnownGroupsLoad.ReadFailed -> Unknown
            }

        private fun decided(
            rules: RuleSet,
            seen: SeenChats,
            decisions: ChatDecisionsLoad,
            groups: KnownGroups,
        ): ChatPrompt =
            when (decisions) {
                is ChatDecisionsLoad.Loaded -> ready(seen.questions(rules, decisions.decisions, groups))
                is ChatDecisionsLoad.Corrupt -> ready(seen.questions(rules, ChatDecisions.NONE, groups))
                is ChatDecisionsLoad.ReadFailed -> Unknown
            }

        private fun ready(questions: List<ChatQuestion>): Ready =
            Ready(
                questions.sortedWith(
                    compareBy<ChatQuestion, String>(String.CASE_INSENSITIVE_ORDER) {
                        it.chat.name.value
                    }.thenBy { it.chat.id.value },
                ),
            )
    }
}
