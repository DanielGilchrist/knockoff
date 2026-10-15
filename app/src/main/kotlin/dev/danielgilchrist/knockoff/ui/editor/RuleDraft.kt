package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.schedule.DailyHours
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.ScheduleParse
import dev.danielgilchrist.knockoff.domain.schedule.WeekDays
import dev.danielgilchrist.knockoff.domain.schedule.WeekDaysParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone

data class RuleDraft(
    val name: String,
    val state: RuleState,
    val policy: Policy,
    val windows: List<WindowDraft>,
    val apps: Set<PackageName>,
    val gmailAccounts: Set<AccountAddress>,
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
                apps = emptySet(),
                gmailAccounts = emptySet(),
                pendingGmail = "",
                zone = RuleZone.Default,
            )

        fun from(rule: Rule): RuleDraft =
            RuleDraft(
                name = rule.name.value,
                state = rule.state,
                policy = rule.policy,
                windows = rule.schedule.windows.map(WindowDraft::from),
                apps =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App -> listOf(target.packageName)
                                is Target.GmailInbox -> emptyList()
                            }
                        }.toSet(),
                gmailAccounts =
                    rule.targets
                        .flatMap { target ->
                            when (target) {
                                is Target.App -> emptyList()
                                is Target.GmailInbox -> listOf(target.address)
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
                    apps.map(Target::App).toSet() + accounts.map(Target::GmailInbox),
                    zone,
                )
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

    private fun schedule(): Checked<Schedule> =
        windows
            .mapIndexed { index, draft ->
                when (val parse = WeekDays.parse(draft.days)) {
                    is WeekDaysParse.Valid -> Checked.Ok(Window(parse.days, DailyHours.between(draft.start, draft.end)))
                    WeekDaysParse.Empty -> Checked.Problems(listOf(DraftProblem.HoursWithoutDays(index + 1)))
                }
            }.all()
            .let { checked ->
                when (checked) {
                    is Checked.Problems -> {
                        checked
                    }

                    is Checked.Ok -> {
                        when (val parse = Schedule.parse(checked.value)) {
                            is ScheduleParse.Valid -> Checked.Ok(parse.schedule)
                            ScheduleParse.Empty -> Checked.Problems(listOf(DraftProblem.NoHours))
                        }
                    }
                }
            }

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
