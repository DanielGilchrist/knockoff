package dev.danielgilchrist.knockoff.domain

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.channel.Importance
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.GroupNameParse
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.ConversationTitle
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait
import dev.danielgilchrist.knockoff.domain.notification.NotificationTraits
import dev.danielgilchrist.knockoff.domain.notification.NotificationTraitsParse
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse
import dev.danielgilchrist.knockoff.domain.schedule.Break
import dev.danielgilchrist.knockoff.domain.schedule.BreakName
import dev.danielgilchrist.knockoff.domain.schedule.BreakNameParse
import dev.danielgilchrist.knockoff.domain.schedule.DailyHours
import dev.danielgilchrist.knockoff.domain.schedule.Interval
import dev.danielgilchrist.knockoff.domain.schedule.IntervalParse
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.ScheduleParse
import dev.danielgilchrist.knockoff.domain.schedule.WeekDays
import dev.danielgilchrist.knockoff.domain.schedule.WeekDaysParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

val LONDON: ZoneId = ZoneId.of("Europe/London")

fun london(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(LONDON)

fun londonWithOffset(
    text: String,
    offset: ZoneOffset,
): ZonedDateTime = ZonedDateTime.ofLocal(LocalDateTime.parse(text), LONDON, offset)

fun days(vararg days: DayOfWeek): WeekDays =
    when (val parse = WeekDays.parse(days.toSet())) {
        is WeekDaysParse.Valid -> parse.days
        WeekDaysParse.Empty -> error("Fixture needs at least one day")
    }

fun window(
    days: WeekDays,
    start: String,
    end: String,
): Window = Window(days, DailyHours.between(LocalTime.parse(start), LocalTime.parse(end)))

fun schedule(
    vararg windows: Window,
    breaks: List<Break> = emptyList(),
): Schedule =
    when (val parse = Schedule.parse(windows.toList(), breaks)) {
        is ScheduleParse.Valid -> parse.schedule
        ScheduleParse.Empty -> error("Fixture needs at least one window")
        ScheduleParse.NothingLeftAfterBreaks -> error("Fixture breaks cover every window")
    }

fun breakNamed(
    name: String,
    window: Window,
): Break =
    when (val parse = BreakName.parse(name)) {
        is BreakNameParse.Valid -> Break(parse.name, window)
        BreakNameParse.Blank -> error("Fixture break needs a name")
    }

val EVERY_DAY: WeekDays = days(*DayOfWeek.entries.toTypedArray())

val ALL_BUT_SUNDAY: WeekDays = days(*DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }.toTypedArray())

val WORK_HOURS: Schedule = schedule(window(days(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), "08:00", "17:00"))

fun groupName(raw: String): GroupName =
    when (val parse = GroupName.parse(raw)) {
        is GroupNameParse.Valid -> parse.name
        GroupNameParse.Blank -> error("Fixture group name is blank")
    }

fun rule(
    name: String,
    policy: Policy,
    schedule: Schedule,
    targets: Set<Target>,
    state: RuleState = RuleState.Enabled,
    zone: RuleZone = RuleZone.Default,
    asking: Set<PackageName> = emptySet(),
): Rule = Rule(ruleName(name), state, policy, schedule, ruleApps(targets, asking), zone)

fun ruleName(raw: String): RuleName =
    when (val parse = RuleName.parse(raw)) {
        is RuleNameParse.Valid -> parse.name
        RuleNameParse.Blank -> error("Fixture rule name is blank")
    }

fun ruleSet(vararg rules: Rule): RuleSet =
    when (val parse = RuleSet.parse(rules.toList(), ZoneChoice.Device)) {
        is RuleSetParse.Valid -> parse.rules
        is RuleSetParse.DuplicateNames -> error("Fixture rules share names: ${parse.names}")
    }

fun gmailOrigin(channelId: String): Origin =
    Origin(
        PackageName.GMAIL,
        Channel.Named(ChannelId(channelId)),
        Conversation.None,
        OriginGroup.Known(gmailGroup(channelId.removePrefix("^nc_1_mail_"))),
    )

fun gmailGroup(address: String): ChannelGrouping = ChannelGrouping.Grouped(ChannelGroupId("^ncg_1_account_$address"))

val SLACK = PackageName("com.Slack")

val WORK_ADDRESS = groupName("dan@work.example")

val SLACK_ORIGIN = Origin(SLACK, Channel.Named(ChannelId("messages")), Conversation.None, OriginGroup.Unknown)

val WORK_GMAIL_ORIGIN = gmailOrigin("^nc_1_mail_dan@work.example")

val PERSONAL_GMAIL_ORIGIN = gmailOrigin("^nc_1_mail_dan@home.example")

val WHATSAPP = PackageName("com.whatsapp")

val WHATSAPP_CHATS = ChannelId("individual_chat_defaults_4")

val COLLEAGUE = ConversationId("1000000000001@lid")

val FRIEND = ConversationId("1000000000002@lid")

val COLLEAGUE_CHAT = Target.Chat(WHATSAPP, WHATSAPP_CHATS, COLLEAGUE, ConversationName("Colleague"))

val COLLEAGUE_CHANNEL = ChannelKey(WHATSAPP, ChannelId("7c0ffee0-conversation"))

val COLLEAGUE_KIND = ChannelKind.ConversationOf(WHATSAPP_CHATS, COLLEAGUE)

fun whatsappOrigin(conversation: Conversation): Origin =
    Origin(WHATSAPP, Channel.Named(WHATSAPP_CHATS), conversation, OriginGroup.Unknown)

fun traits(vararg traits: NotificationTrait): NotificationTraits =
    when (val parse = NotificationTraits.parse(traits.toSet())) {
        is NotificationTraitsParse.Present -> parse.traits
        NotificationTraitsParse.None -> error("Fixture needs at least one trait")
    }

internal fun interval(
    start: Instant,
    end: Instant,
): Interval =
    when (val parse = Interval.parse(start, end)) {
        is IntervalParse.Valid -> parse.interval
        IntervalParse.Empty -> error("Fixture interval must end after it starts")
    }

fun sharedChannels(vararg channels: Pair<ChannelKey, Importance>): Map<ChannelKey, ReadChannel> =
    channels.associate { (key, importance) ->
        key to
            ReadChannel(importance, ChannelKind.Shared, ChannelGrouping.Ungrouped)
    }

fun known(
    id: ConversationId,
    name: String = "Colleague",
): Conversation = Conversation.Known(id, ConversationTitle.Named(ConversationName(name)))

fun textPattern(source: String): TextPattern =
    when (val parse = TextPattern.parse(source)) {
        is TextPatternParse.Valid -> parse.pattern
        TextPatternParse.Blank, is TextPatternParse.Invalid -> error("Fixture pattern is invalid: $source")
    }

fun GroupName.gmailAccount(): Target.Account =
    Target.Account(PackageName.GMAIL, ChannelGroupId("^ncg_1_account_$value"), this)
