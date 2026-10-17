package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.chat.ChatQuestion
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
        ): ChatPrompt =
            when (settings) {
                is SettingsLoad.Failure -> {
                    Unknown
                }

                is SettingsLoad.Usable -> {
                    when (seen) {
                        is SeenChatsLoad.Loaded -> {
                            when (decisions) {
                                is ChatDecisionsLoad.Loaded -> {
                                    ready(
                                        seen.seen.questions(settings.settings.rules, decisions.decisions),
                                    )
                                }

                                is ChatDecisionsLoad.Corrupt -> {
                                    ready(
                                        seen.seen.questions(settings.settings.rules, ChatDecisions.NONE),
                                    )
                                }

                                is ChatDecisionsLoad.ReadFailed -> {
                                    Unknown
                                }
                            }
                        }

                        is SeenChatsLoad.Corrupt, is SeenChatsLoad.ReadFailed -> {
                            Unknown
                        }
                    }
                }
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
