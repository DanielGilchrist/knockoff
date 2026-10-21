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
import dev.danielgilchrist.knockoff.domain.rule.gmailAccount
import dev.danielgilchrist.knockoff.domain.rule.gmailAddress
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.ScheduleParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.ui.SeenChatsState

data class RuleDraft(
    val name: String,
    val state: RuleState,
    val policy: Policy,
    val windows: List<WindowDraft>,
    val breaks: List<BreakDraft>,
    val apps: Set<PackageName>,
    val gmailAccounts: Set<AccountAddress>,
    val chats: Map<ChatKey, Target.Chat>,
    val patterns: List<PatternDraft>,
    val pendingGmail: String,
    val zone: RuleZone,
    val asking: Set<PackageName>,
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
                patterns = emptyList(),
                pendingGmail = "",
                zone = RuleZone.Default,
                asking = emptySet(),
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
                                is Target.Account, is Target.Chat, is Target.Pattern -> emptyList()
                            }
                        }.toSet(),
                gmailAccounts =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App, is Target.Chat, is Target.Pattern -> {
                                    emptyList()
                                }

                                is Target.Account -> {
                                    when (val parse = AccountAddress.parse(target.gmailAddress())) {
                                        is AccountAddressParse.Valid -> listOf(parse.address)
                                        is AccountAddressParse.Invalid -> emptyList()
                                    }
                                }
                            }
                        }.toSet(),
                chats =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App, is Target.Account, is Target.Pattern -> emptyList()
                                is Target.Chat -> listOf(target)
                            }
                        }.associateBy { it.key },
                patterns =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App, is Target.Account, is Target.Chat -> emptyList()
                                is Target.Pattern -> listOf(PatternDraft.from(target))
                            }
                        },
                pendingGmail = "",
                zone = rule.zone,
                asking = rule.asking,
            )
    }

    fun parse(): Checked<Rule> =
        ruleName()
            .and(schedule()) { ruleName, schedule -> ruleName to schedule }
            .and(targets()) { (ruleName, schedule), targets ->
                Rule(ruleName, state, policy, schedule, targets, zone, asking)
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

    fun askableApps(seen: SeenChatsState): Set<PackageName> {
        val seenApps =
            when (seen) {
                is SeenChatsState.Loaded -> seen.chats.map { it.packageName }
                SeenChatsState.Loading, is SeenChatsState.Failed -> emptyList()
            }

        return (seenApps + chats.values.map { it.packageName }).toSet() - apps + asking
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

    private fun targets(): Checked<Set<Target>> =
        allGmailAccounts().and(parsedPatterns()) { accounts, parsed ->
            apps.map(Target::App).toSet() + accounts.map { it.gmailAccount() } + chats.values + parsed
        }

    private fun parsedPatterns(): Checked<List<Target.Pattern>> =
        patterns
            .mapIndexed { index, draft ->
                draft.parse(index + 1)
            }.all()

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
