package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.dayoff.DaysOff
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.ScheduleParse
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.RuleZoneParse
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredRule(
    private val name: String,
    private val state: String,
    private val policy: String,
    private val windows: List<StoredWindow>,
    private val breaks: List<StoredBreak>,
    private val zone: String,
    private val apps: List<StoredApp>,
    private val daysOff: List<StoredLeave>,
    private val publicHolidays: String,
) {
    companion object {
        fun from(rule: Rule): StoredRule =
            StoredRule(
                name = rule.name.value,
                state = rule.state.name,
                policy = rule.policy.name,
                windows = rule.schedule.windows.map(StoredWindow::from),
                breaks = rule.schedule.breaks.map(StoredBreak::from),
                zone = rule.zone.stored(),
                apps = rule.apps.scopes.map { (packageName, scope) -> StoredApp.from(packageName, scope) },
                daysOff = rule.daysOff.leave.map(StoredLeave::from),
                publicHolidays = rule.daysOff.publicHolidays.name,
            )
    }

    fun parse(): Parsed<Rule> =
        ruleName().then { ruleName ->
            ruleState()
                .then { ruleState ->
                    rulePolicy().then { rulePolicy ->
                        schedule().then { schedule ->
                            ruleApps().then { ruleApps ->
                                ruleZone().then { ruleZone ->
                                    ruleDaysOff().then { ruleDaysOff ->
                                        Parsed.Ok(
                                            Rule(
                                                ruleName,
                                                ruleState,
                                                rulePolicy,
                                                schedule,
                                                ruleApps,
                                                ruleZone,
                                                ruleDaysOff,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }.within("\"${ruleName.value}\"")
        }

    private fun ruleName(): Parsed<RuleName> =
        when (val parse = RuleName.parse(name)) {
            is RuleNameParse.Valid -> Parsed.Ok(parse.name)
            RuleNameParse.Blank -> Parsed.Failed("blank name")
        }

    private fun ruleState(): Parsed<RuleState> =
        when (val known = RuleState.entries.firstOrNull { it.name == state }) {
            null -> Parsed.Failed("unknown state \"$state\"")
            else -> Parsed.Ok(known)
        }

    private fun rulePolicy(): Parsed<Policy> =
        when (val known = Policy.entries.firstOrNull { it.name == policy }) {
            null -> Parsed.Failed("unknown policy \"$policy\"")
            else -> Parsed.Ok(known)
        }

    private fun schedule(): Parsed<Schedule> =
        windows.parseEach("window") { it.parse() }.then { parsedWindows ->
            breaks.parseEach("break") { it.parse() }.then { parsedBreaks ->
                when (val parse = Schedule.parse(parsedWindows, parsedBreaks)) {
                    is ScheduleParse.Valid -> Parsed.Ok(parse.schedule)
                    ScheduleParse.Empty -> Parsed.Failed("no windows")
                    ScheduleParse.NothingLeftAfterBreaks -> Parsed.Failed("breaks cover all of its hours")
                }
            }
        }

    private fun ruleZone(): Parsed<RuleZone> =
        when (val parse = RuleZone.parse(zone)) {
            is RuleZoneParse.Valid -> Parsed.Ok(parse.zone)
            is RuleZoneParse.Unknown -> Parsed.Failed("unknown zone \"${parse.cause.raw}\": ${parse.cause.detail}")
        }

    private fun ruleApps(): Parsed<RuleApps> =
        apps.parseEach("app") { it.parse() }.then { parsed ->
            val repeated = parsed.groupBy { it.first }.filterValues { it.size > 1 }.keys

            when {
                repeated.isEmpty() -> Parsed.Ok(RuleApps.of(parsed.toMap<PackageName, AppScope>()))
                else -> Parsed.Failed("duplicate apps ${repeated.map { it.value }}")
            }
        }

    private fun ruleDaysOff(): Parsed<DaysOff> =
        daysOff.parseEach("day off") { it.parse() }.then { leave ->
            when (val known = PublicHolidays.entries.firstOrNull { it.name == publicHolidays }) {
                null -> Parsed.Failed("unknown public holidays \"$publicHolidays\"")
                else -> Parsed.Ok(DaysOff(leave, known))
            }
        }
}
