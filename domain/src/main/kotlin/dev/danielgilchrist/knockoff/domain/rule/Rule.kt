package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.notification.Origin
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
    val targets: Set<Target>,
    val zone: RuleZone,
) {
    val chats: List<Target.Chat>
        get() =
            targets.flatMap { target ->
                when (target) {
                    is Target.Chat -> listOf(target)
                    is Target.App, is Target.GmailInbox, is Target.Pattern -> emptyList()
                }
            }

    fun withChat(chat: Target.Chat): Rule = copy(targets = targets - chats.filter { it.key == chat.key }.toSet() + chat)

    fun holds(chat: Target.Chat): Boolean = targets.any { it.holds(chat) }

    internal fun withSeenChats(seen: SeenChats): Rule {
        val latest = seen.chats.associateBy { it.key }
        val picked = chats
        val refreshed =
            picked.map { chat ->
                when (val fresh = latest[chat.key]) {
                    null -> chat
                    else -> chat.copy(channel = fresh.channel)
                }
            }
        val pickedKeys = picked.map { it.key }.toSet()
        val matching = seen.chats.filter { chat -> chat.key !in pickedKeys && targets.any { it.expandsTo(chat) } }

        return copy(targets = targets - picked.toSet() + refreshed + matching)
    }

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
        at: ZonedDateTime,
        zones: ZoneContext,
    ): RuleVerdict = verdictWhere(at, zones) { it.covers(channel, kind) }

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
