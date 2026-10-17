package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.ChatKey
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.ScheduleParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone

data class RuleDraft(
    val name: String,
    val state: RuleState,
    val policy: Policy,
    val windows: List<WindowDraft>,
    val breaks: List<BreakDraft>,
    val apps: Set<PackageName>,
    val gmailAccounts: Set<AccountAddress>,
    val chats: Map<ChatKey, Target.Chat>,
    val patterns: Set<Target.Pattern>,
    val pendingGmail: String,
    val zone: RuleZone,
) {
    companion object {
        val NEW =
            RuleDraft(
                name = "",
                state = RuleState.Enabled,
                policy = Policy.DeliverOnlyDuring,
                windows = listOf(WindowDraft.WORKING_HOURS),
                breaks = emptyList(),
                apps = emptySet(),
                gmailAccounts = emptySet(),
                chats = emptyMap(),
                patterns = emptySet(),
                pendingGmail = "",
                zone = RuleZone.Default,
            )

        fun from(rule: Rule): RuleDraft =
            RuleDraft(
                name = rule.name.value,
                state = rule.state,
                policy = rule.policy,
                windows = rule.schedule.windows.map(WindowDraft::from),
                breaks = rule.schedule.breaks.map(BreakDraft::from),
                apps =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App -> listOf(target.packageName)
                                is Target.GmailInbox, is Target.Chat, is Target.Pattern -> emptyList()
                            }
                        }.toSet(),
                gmailAccounts =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App, is Target.Chat, is Target.Pattern -> emptyList()
                                is Target.GmailInbox -> listOf(target.address)
                            }
                        }.toSet(),
                chats =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App, is Target.GmailInbox, is Target.Pattern -> emptyList()
                                is Target.Chat -> listOf(target)
                            }
                        }.associateBy { it.key },
                patterns =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App, is Target.GmailInbox, is Target.Chat -> emptyList()
                                is Target.Pattern -> listOf(target)
                            }
                        }.toSet(),
                pendingGmail = "",
                zone = rule.zone,
            )
    }

    fun parse(): Checked<Rule> =
        ruleName()
            .and(schedule()) { ruleName, schedule -> ruleName to schedule }
            .and(allGmailAccounts()) { (ruleName, schedule), accounts ->
                Rule(
                    ruleName,
                    state,
                    policy,
                    schedule,
                    apps.map(Target::App).toSet() + accounts.map(Target::GmailInbox) + chats.values + patterns,
                    zone,
                )
            }

    fun withBreakAdded(): RuleDraft {
        val days = windows.flatMap { it.days }.toSet()
        val added =
            when {
                days.isEmpty() -> BreakDraft.LUNCH
                else -> BreakDraft.LUNCH.copy(window = BreakDraft.LUNCH.window.copy(days = days))
            }

        return copy(breaks = breaks + added)
    }

    fun withPendingGmailAdded(): RuleDraft =
        when (val parse = AccountAddress.parse(pendingGmail)) {
            is AccountAddressParse.Valid -> copy(gmailAccounts = gmailAccounts + parse.address, pendingGmail = "")
            is AccountAddressParse.Invalid -> this
        }

    private fun ruleName(): Checked<RuleName> =
        when (val parse = RuleName.parse(name)) {
            is RuleNameParse.Valid -> Checked.Ok(parse.name)
            RuleNameParse.Blank -> Checked.Problems(listOf(DraftProblem.BlankName))
        }

    private fun schedule(): Checked<Schedule> {
        val checkedWindows =
            when {
                windows.isEmpty() -> Checked.Problems(listOf(DraftProblem.NoHours))
                else -> windows.mapIndexed(::hoursChecked).all()
            }
        val checkedBreaks = breaks.mapIndexed { index, draft -> draft.parse(index + 1) }.all()

        return when (val checked = checkedWindows.and(checkedBreaks, Schedule::parse)) {
            is Checked.Problems -> {
                checked
            }

            is Checked.Ok -> {
                when (val parse = checked.value) {
                    is ScheduleParse.Valid -> Checked.Ok(parse.schedule)
                    ScheduleParse.Empty -> Checked.Problems(listOf(DraftProblem.NoHours))
                    ScheduleParse.NothingLeftAfterBreaks -> Checked.Problems(listOf(DraftProblem.BreaksCoverAllHours))
                }
            }
        }
    }

    private fun hoursChecked(
        index: Int,
        window: WindowDraft,
    ): Checked<Window> = window.parse(DraftProblem.HoursWithoutDays(index + 1))

    private fun allGmailAccounts(): Checked<Set<AccountAddress>> =
        when {
            pendingGmail.isBlank() -> {
                Checked.Ok(gmailAccounts)
            }

            else -> {
                when (val parse = AccountAddress.parse(pendingGmail)) {
                    is AccountAddressParse.Valid -> {
                        Checked.Ok(gmailAccounts + parse.address)
                    }

                    is AccountAddressParse.Invalid -> {
                        Checked.Problems(
                            listOf(DraftProblem.UnaddedAccount(parse.detail)),
                        )
                    }
                }
            }
        }
}
