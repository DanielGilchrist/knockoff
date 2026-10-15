package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelPlan
import dev.danielgilchrist.knockoff.domain.channel.ChannelState
import dev.danielgilchrist.knockoff.domain.channel.Importance
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
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

    fun recheck(
        snoozed: PostedNotification,
        before: RuleSet,
        now: ZonedDateTime,
    ): Recheck {
        val previous = before.decide(snoozed, now)

        return when (val current = decide(snoozed, now)) {
            is Decision.Snooze -> {
                when {
                    previous is Decision.Snooze && previous.until == current.until -> Recheck.Unchanged
                    else -> Recheck.Resnooze(current)
                }
            }

            is Decision.Leave -> {
                when (previous) {
                    is Decision.Snooze -> Recheck.Release(current)
                    is Decision.Leave -> Recheck.Unchanged
                }
            }
        }
    }

    fun channelPackages(silenced: SilencedChannels): Set<PackageName> =
        enabled().flatMap { rule -> rule.targets.map { it.packageName } }.toSet() + silenced.packages

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
        read: Map<ChannelKey, Importance>,
        silenced: SilencedChannels,
        now: ZonedDateTime,
    ): ChannelPlan =
        silenced.plan(read) { key ->
            when {
                holdsAt(key.origin, now, ZoneContext(now.zone, defaultZone)).isEmpty() -> ChannelState.Permitted
                else -> ChannelState.Held
            }
        }

    private fun enabled(): List<Rule> = rules.filter { it.state == RuleState.Enabled }

    private fun decide(
        origin: Origin,
        now: ZonedDateTime,
    ): Decision {
        val zones = ZoneContext(now.zone, defaultZone)
        val holding = holdsAt(origin, now, zones)
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

        return when (val release = holdsAt(origin, at, zones).firstOrNull()?.release) {
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

    private fun holdsAt(
        origin: Origin,
        at: ZonedDateTime,
        zones: ZoneContext,
    ): List<Hold> =
        rules.flatMap { rule ->
            when (val verdict = rule.verdictFor(origin, at, zones)) {
                is RuleVerdict.Holds -> listOf(Hold(rule.name, verdict.release))
                RuleVerdict.Abstains, RuleVerdict.Permits -> emptyList()
            }
        }
}
