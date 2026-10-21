package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
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
    val apps: Map<PackageName, AppDraft>,
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
                apps = emptyMap(),
                zone = RuleZone.Default,
            )

        fun from(rule: Rule): RuleDraft =
            RuleDraft(
                name = rule.name.value,
                state = rule.state,
                policy = rule.policy,
                windows = rule.schedule.windows.map(WindowDraft::from),
                breaks = rule.schedule.breaks.map(BreakDraft::from),
                apps = rule.apps.scopes.mapValues { (_, scope) -> AppDraft.from(scope) },
                zone = rule.zone,
            )
    }

    fun parse(): Checked<Rule> =
        ruleName()
            .and(schedule()) { ruleName, schedule -> ruleName to schedule }
            .and(ruleApps()) { (ruleName, schedule), ruleApps ->
                Rule(ruleName, state, policy, schedule, ruleApps, zone)
            }

    fun withApps(added: Set<PackageName>): RuleDraft =
        copy(apps = apps + (added - apps.keys).associateWith { AppDraft.EVERYTHING })

    fun withApp(
        app: PackageName,
        draft: AppDraft,
    ): RuleDraft = copy(apps = apps + (app to draft))

    fun withoutApp(app: PackageName): RuleDraft = copy(apps = apps - app)

    fun withLearnedNames(groups: KnownGroups): RuleDraft =
        copy(
            apps =
                apps.mapValues { (app, draft) ->
                    draft.withLearnedNames(AccountChoices.learnedNames(groups.groupsOf(app)))
                },
        )

    fun withBreakAdded(): RuleDraft {
        val days = windows.flatMap { it.days }.toSet()
        val added =
            when {
                days.isEmpty() -> BreakDraft.LUNCH
                else -> BreakDraft.LUNCH.copy(window = BreakDraft.LUNCH.window.copy(days = days))
            }

        return copy(breaks = breaks + added)
    }

    private fun ruleName(): Checked<RuleName> =
        when (val parse = RuleName.parse(name)) {
            is RuleNameParse.Valid -> Checked.Ok(parse.name)
            RuleNameParse.Blank -> Checked.Problems(listOf(DraftProblem.BlankName))
        }

    fun schedule(): Checked<Schedule> {
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

    private fun ruleApps(): Checked<RuleApps> =
        apps
            .map { (app, draft) -> draft.parse(app).map { scope -> app to scope } }
            .all()
            .map { scopes -> RuleApps.of(scopes.toMap()) }
}
