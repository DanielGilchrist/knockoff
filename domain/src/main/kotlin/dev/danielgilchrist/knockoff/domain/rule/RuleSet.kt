package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelPlan
import dev.danielgilchrist.knockoff.domain.channel.ChannelState
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
import dev.danielgilchrist.knockoff.domain.chat.ChatChannelNeed
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.schedule.Coverage
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.domain.zone.ZoneContext
import java.time.Duration
import java.time.ZonedDateTime

@ConsistentCopyVisibility
data class RuleSet private constructor(
    val rules: List<Rule>,
    val defaultZone: ZoneChoice,
) {
    companion object {
        val SNOOZE_CEILING: Duration = Duration.ofDays(7)

        internal fun of(
            rule: Rule,
            defaultZone: ZoneChoice,
        ): RuleSet = RuleSet(listOf(rule), defaultZone)

        internal fun parse(
            rules: List<Rule>,
            defaultZone: ZoneChoice,
        ): RuleSetParse {
            val duplicates =
                rules
                    .groupingBy { it.name }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys

            return when {
                duplicates.isEmpty() -> RuleSetParse.Valid(RuleSet(rules.toList(), defaultZone))
                else -> RuleSetParse.DuplicateNames(duplicates)
            }
        }
    }

    fun withRules(rules: List<Rule>): RuleSetParse = parse(rules, defaultZone)

    fun withDefaultZone(zone: ZoneChoice): RuleSet = RuleSet(rules, zone)

    fun decide(
        notification: PostedNotification,
        now: ZonedDateTime,
    ): Decision =
        when (notification) {
            is PostedNotification.Unsnoozable -> Decision.Leave(LeaveReason.Unsnoozable(notification.traits))
            is PostedNotification.Snoozable -> decide(notification.origin, now)
        }

    fun channelPackages(silenced: SilencedChannels): Set<PackageName> =
        enabled().flatMap { rule -> rule.targets.map { it.packageName } }.toSet() + silenced.packages

    fun chatChannelNeeds(
        read: Map<ChannelKey, ReadChannel>,
        silenced: SilencedChannels,
    ): Set<ChatChannelNeed> =
        enabled()
            .flatMap { rule ->
                rule.targets.flatMap { target ->
                    when (target) {
                        is Target.App, is Target.GmailInbox, is Target.Pattern -> emptyList()
                        is Target.Chat -> listOf(target)
                    }
                }
            }.filter { chat -> read.none { (key, channel) -> chat.covers(key, channel.kind) } }
            .map { chat ->
                val parent = ChannelKey(chat.packageName, chat.channel)

                when {
                    parent !in read -> ChatChannelNeed.ParentMissing(chat)
                    silenced.saves(parent) -> ChatChannelNeed.ParentSilenced(chat)
                    else -> ChatChannelNeed.Create(chat)
                }
            }.toSet()

    fun nextBoundary(now: ZonedDateTime): Boundary =
        enabled()
            .filter { it.targets.isNotEmpty() }
            .flatMap { rule ->
                when (
                    val coverage =
                        rule.schedule.coverageAt(
                            rule.localised(now, ZoneContext(now.zone, defaultZone)),
                        )
                ) {
                    is Coverage.Inside -> listOf(coverage.endsAt.withZoneSameInstant(now.zone))
                    is Coverage.Outside -> listOf(coverage.startsAt.withZoneSameInstant(now.zone))
                    Coverage.Always -> emptyList()
                }
            }.minOrNull()
            .let { time ->
                when (time) {
                    null -> Boundary.None
                    else -> Boundary.At(time)
                }
            }

    fun planChannels(
        read: Map<ChannelKey, ReadChannel>,
        silenced: SilencedChannels,
        now: ZonedDateTime,
    ): ChannelPlan {
        val zones = ZoneContext(now.zone, defaultZone)

        return silenced.plan(read) { key, channel ->
            when {
                holds { it.verdictFor(key, channel.kind, now, zones) }.isEmpty() -> ChannelState.Permitted
                else -> ChannelState.Held
            }
        }
    }

    private fun decide(
        origin: Origin,
        now: ZonedDateTime,
    ): Decision {
        val zones = ZoneContext(now.zone, defaultZone)
        val holding = holds { it.verdictFor(origin, now, zones) }
        val permitting =
            rules
                .filter {
                    it.verdictFor(
                        origin,
                        now,
                        zones,
                    ) == RuleVerdict.Permits
                }.map { it.name }
                .toSet()

        return when {
            holding.isNotEmpty() -> releaseFrom(origin, now, at = now, heldBy = holding.map { it.rule }.toSet(), zones)
            permitting.isEmpty() -> Decision.Leave(LeaveReason.NoRuleTargets)
            else -> Decision.Leave(LeaveReason.Permitted(by = permitting))
        }
    }

    private tailrec fun releaseFrom(
        origin: Origin,
        now: ZonedDateTime,
        at: ZonedDateTime,
        heldBy: Set<RuleName>,
        zones: ZoneContext,
    ): Decision.Snooze {
        val ceiling = now.plus(SNOOZE_CEILING)

        return when (val release = holds { it.verdictFor(origin, at, zones) }.firstOrNull()?.release) {
            null -> {
                Decision.Snooze(now, at, SnoozeEnd.RulesPermit, heldBy)
            }

            Release.Never -> {
                Decision.Snooze(now, ceiling, SnoozeEnd.CeilingReached, heldBy)
            }

            is Release.At -> {
                when {
                    release.time.isAfter(ceiling) -> Decision.Snooze(now, ceiling, SnoozeEnd.CeilingReached, heldBy)
                    else -> releaseFrom(origin, now, release.time.withZoneSameInstant(now.zone), heldBy, zones)
                }
            }
        }
    }

    private fun holds(verdictOf: (Rule) -> RuleVerdict): List<Hold> =
        rules.flatMap { rule ->
            when (val verdict = verdictOf(rule)) {
                is RuleVerdict.Holds -> listOf(Hold(rule.name, verdict.release))
                RuleVerdict.Abstains, RuleVerdict.Permits -> emptyList()
            }
        }

    private fun enabled(): List<Rule> = rules.filter { it.state == RuleState.Enabled }
}
