package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.NarrowedParse
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking

data class AppDraft(
    val mode: AppMode,
    val accounts: Map<ChannelGroupId, GroupName>,
    val chats: Map<ConversationId, ChatPick>,
    val patterns: List<PatternDraft>,
    val asking: NewChatsAsking,
) {
    companion object {
        val EVERYTHING: AppDraft
            get() = AppDraft(AppMode.Everything, emptyMap(), emptyMap(), emptyList(), NewChatsAsking.DontAsk)

        fun from(scope: AppScope): AppDraft =
            when (scope) {
                AppScope.Everything -> {
                    EVERYTHING
                }

                is AppScope.Narrowed -> {
                    AppDraft(
                        AppMode.Narrowed,
                        scope.accounts,
                        scope.chats,
                        scope.patterns.map(PatternDraft::from),
                        scope.asking,
                    )
                }
            }
    }

    val picksSomething: Boolean
        get() = accounts.isNotEmpty() || chats.isNotEmpty() || patterns.isNotEmpty() || asking == NewChatsAsking.Ask

    fun leavesNothingChosen(app: PackageName): Boolean = ProblemSplit.of(parse(app)).unchosen.isNotEmpty()

    fun offersNarrowing(available: Int): Boolean = available > 0 || mode == AppMode.Narrowed || picksSomething

    fun withLearnedNames(learned: Map<ChannelGroupId, GroupName>): AppDraft =
        copy(accounts = accounts.mapValues { (id, name) -> learned.getOrDefault(id, name) })

    fun parse(app: PackageName): Checked<AppScope> =
        when (mode) {
            AppMode.Everything -> Checked.Ok(AppScope.Everything)
            AppMode.Narrowed -> narrowed(app)
        }

    private fun narrowed(app: PackageName): Checked<AppScope> =
        when (
            val checked =
                patterns.mapIndexed { index, pattern -> pattern.parse(app, index + 1) }.all()
        ) {
            is Checked.Problems -> {
                checked
            }

            is Checked.Ok -> {
                when (val parse = AppScope.Narrowed.parse(accounts, chats.values, checked.value.toSet(), asking)) {
                    is NarrowedParse.Valid -> Checked.Ok(parse.scope)
                    NarrowedParse.NothingChosen -> Checked.Problems(listOf(DraftProblem.NothingChosen(app)))
                }
            }
        }
}
