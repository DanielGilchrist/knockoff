package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.notification.PackageName
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
    private val apps: List<String>,
    private val gmailAccounts: List<String>,
    private val chats: List<StoredChat>,
    private val patterns: List<StoredPattern>,
    private val zone: String,
    private val asking: List<String>,
) {
    companion object {
        fun from(rule: Rule): StoredRule =
            StoredRule(
                name = rule.name.value,
                state = rule.state.name,
                policy = rule.policy.name,
                windows = rule.schedule.windows.map(StoredWindow::from),
                breaks = rule.schedule.breaks.map(StoredBreak::from),
                apps =
                    rule.targets.flatMap { target ->
                        when (target) {
                            is Target.App -> listOf(target.packageName.value)
                            is Target.Account, is Target.Chat, is Target.Pattern -> emptyList()
                        }
                    },
                gmailAccounts =
                    rule.targets.flatMap { target ->
                        when (target) {
                            is Target.App, is Target.Chat, is Target.Pattern -> emptyList()
                            is Target.Account -> listOf(target.gmailAddress())
                        }
                    },
                chats =
                    rule.targets.flatMap { target ->
                        when (target) {
                            is Target.App, is Target.Account, is Target.Pattern -> emptyList()
                            is Target.Chat -> listOf(StoredChat.from(target))
                        }
                    },
                patterns =
                    rule.targets.flatMap { target ->
                        when (target) {
                            is Target.App, is Target.Account, is Target.Chat -> emptyList()
                            is Target.Pattern -> listOf(StoredPattern.from(target))
                        }
                    },
                zone = rule.zone.stored(),
                asking = rule.asking.map { it.value },
            )
    }

    fun parse(): Parsed<Rule> =
        ruleName().then { ruleName ->
            ruleState()
                .then { ruleState ->
                    rulePolicy().then { rulePolicy ->
                        schedule().then { schedule ->
                            targets().then { targets ->
                                ruleZone().then { ruleZone ->
                                    Parsed.Ok(
                                        Rule(
                                            ruleName,
                                            ruleState,
                                            rulePolicy,
                                            schedule,
                                            targets,
                                            ruleZone,
                                            asking.map(::PackageName).toSet(),
                                        ),
                                    )
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

    private fun targets(): Parsed<Set<Target>> =
        gmailAccounts
            .parseEach("Gmail account") { raw ->
                when (val parse = AccountAddress.parse(raw)) {
                    is AccountAddressParse.Valid -> Parsed.Ok(parse.address.gmailAccount())
                    is AccountAddressParse.Invalid -> Parsed.Failed(parse.detail)
                }
            }.then { inboxes ->
                chats.parseEach("chat") { it.parse() }.then { parsedChats ->
                    val repeated = parsedChats.groupBy { it.key }.filterValues { it.size > 1 }.keys

                    when {
                        repeated.isEmpty() -> {
                            patterns.parseEach("pattern") { it.parse() }.then { parsedPatterns ->
                                Parsed.Ok(
                                    apps.map { Target.App(PackageName(it)) }.toSet() + inboxes + parsedChats +
                                        parsedPatterns,
                                )
                            }
                        }

                        else -> {
                            Parsed.Failed(
                                "duplicate chats ${repeated.map { "${it.packageName.value}/${it.id.value}" }}",
                            )
                        }
                    }
                }
            }
}
