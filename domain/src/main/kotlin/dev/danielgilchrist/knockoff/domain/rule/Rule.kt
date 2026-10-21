package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.schedule.Coverage
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneContext
import java.time.ZonedDateTime

data class Rule(
    val name: RuleName,
    val state: RuleState,
    val policy: Policy,
    val schedule: Schedule,
    val apps: RuleApps,
    val zone: RuleZone,
) {
    val targets: Set<Target> = apps.targets

    val asking: Set<PackageName> = apps.asking

    val chats: List<Target.Chat>
        get() =
            targets.flatMap { target ->
                when (target) {
                    is Target.Chat -> listOf(target)
                    is Target.App, is Target.Account, is Target.Pattern -> emptyList()
                }
            }

    fun withChat(chat: Target.Chat): Rule = copy(apps = apps.withChat(chat))

    fun holds(
        chat: Target.Chat,
        grouping: ChannelGrouping,
    ): Boolean = targets.any { it.holds(chat, grouping) }

    fun asks(chat: Target.Chat): Boolean = state == RuleState.Enabled && apps.asks(chat.packageName)

    internal fun withSeenChats(seen: SeenChats): Rule = copy(apps = apps.expanded(seen))

    fun localised(
        at: ZonedDateTime,
        zones: ZoneContext,
    ): ZonedDateTime = at.withZoneSameInstant(zones.zoneFor(zone))

    fun verdictFor(
        origin: Origin,
        at: ZonedDateTime,
        zones: ZoneContext,
    ): RuleVerdict = verdictWhere(at, zones) { it.matches(origin) }

    fun verdictFor(
        channel: ChannelKey,
        kind: ChannelKind,
        grouping: ChannelGrouping,
        at: ZonedDateTime,
        zones: ZoneContext,
    ): RuleVerdict = verdictWhere(at, zones) { it.covers(channel, kind, grouping) }

    private fun verdictWhere(
        at: ZonedDateTime,
        zones: ZoneContext,
        targeted: (Target) -> Boolean,
    ): RuleVerdict =
        when (state) {
            RuleState.Disabled -> {
                RuleVerdict.Abstains
            }

            RuleState.Enabled -> {
                when {
                    targets.none(targeted) -> RuleVerdict.Abstains
                    else -> scheduledVerdictAt(localised(at, zones))
                }
            }
        }

    private fun scheduledVerdictAt(at: ZonedDateTime): RuleVerdict =
        when (val coverage = schedule.coverageAt(at)) {
            is Coverage.Inside -> {
                when (policy) {
                    Policy.DeliverOnlyDuring -> RuleVerdict.Permits
                    Policy.HoldDuring -> RuleVerdict.Holds(Release.At(coverage.endsAt))
                }
            }

            is Coverage.Outside -> {
                when (policy) {
                    Policy.DeliverOnlyDuring -> RuleVerdict.Holds(Release.At(coverage.startsAt))
                    Policy.HoldDuring -> RuleVerdict.Permits
                }
            }

            Coverage.Always -> {
                when (policy) {
                    Policy.DeliverOnlyDuring -> RuleVerdict.Permits
                    Policy.HoldDuring -> RuleVerdict.Holds(Release.Never)
                }
            }
        }
}
