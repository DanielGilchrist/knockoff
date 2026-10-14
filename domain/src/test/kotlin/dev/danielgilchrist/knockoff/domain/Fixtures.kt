package dev.danielgilchrist.knockoff.domain

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.schedule.DailyHours
import dev.danielgilchrist.knockoff.domain.schedule.Schedule
import dev.danielgilchrist.knockoff.domain.schedule.ScheduleParse
import dev.danielgilchrist.knockoff.domain.schedule.WeekDays
import dev.danielgilchrist.knockoff.domain.schedule.WeekDaysParse
import dev.danielgilchrist.knockoff.domain.schedule.Window
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
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

fun schedule(vararg windows: Window): Schedule =
    when (val parse = Schedule.parse(windows.toList())) {
        is ScheduleParse.Valid -> parse.schedule
        ScheduleParse.Empty -> error("Fixture needs at least one window")
    }

val EVERY_DAY: WeekDays = days(*DayOfWeek.entries.toTypedArray())

val ALL_BUT_SUNDAY: WeekDays = days(*DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }.toTypedArray())

val WORK_HOURS: Schedule = schedule(window(days(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), "08:00", "17:00"))

fun address(raw: String): AccountAddress =
    when (val parse = AccountAddress.parse(raw)) {
        is AccountAddressParse.Valid -> parse.address
        is AccountAddressParse.Invalid -> error(parse.detail)
    }

fun rule(
    name: String,
    policy: Policy,
    schedule: Schedule,
    targets: Set<Target>,
    state: RuleState = RuleState.Enabled,
): Rule = Rule(ruleName(name), state, policy, schedule, targets)

fun ruleName(raw: String): RuleName =
    when (val parse = RuleName.parse(raw)) {
        is RuleNameParse.Valid -> parse.name
        RuleNameParse.Blank -> error("Fixture rule name is blank")
    }

fun ruleSet(vararg rules: Rule): RuleSet =
    when (val parse = RuleSet.parse(rules.toList())) {
        is RuleSetParse.Valid -> parse.rules
        is RuleSetParse.DuplicateNames -> error("Fixture rules share names: ${parse.names}")
    }

fun gmailOrigin(channelId: String): Origin = Origin(PackageName.GMAIL, Channel.Named(ChannelId(channelId)))

val SLACK = PackageName("com.Slack")

val WORK_ADDRESS = address("dan@work.example")

val SLACK_ORIGIN = Origin(SLACK, Channel.Named(ChannelId("messages")))

val WORK_GMAIL_ORIGIN = gmailOrigin("^nc_1_mail_dan@work.example")

val PERSONAL_GMAIL_ORIGIN = gmailOrigin("^nc_1_mail_dan@home.example")
