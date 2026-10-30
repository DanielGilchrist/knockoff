package dev.danielgilchrist.knockoff

import dev.danielgilchrist.knockoff.domain.dayoff.HolidayRegion
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayRegionParse
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.LeaveParse
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.GroupNameParse
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target
import java.time.LocalDate

val COLLEAGUE_CHAT =
    Target.Chat(
        PackageName("com.whatsapp"),
        ChannelId("individual_chat_defaults_4"),
        ConversationId("1000000000001@lid"),
        ConversationName("Colleague"),
    )

fun address(raw: String): AccountAddress =
    when (val parse = AccountAddress.parse(raw)) {
        is AccountAddressParse.Valid -> parse.address
        AccountAddressParse.NotAnAddress -> error("Fixture address isn't an address")
    }

fun groupName(raw: String): GroupName =
    when (val parse = GroupName.parse(raw)) {
        is GroupNameParse.Valid -> parse.name
        GroupNameParse.Blank -> error("Fixture group name is blank")
    }

fun leave(
    first: String,
    last: String,
): Leave =
    when (val parse = Leave.parse(LocalDate.parse(first), LocalDate.parse(last))) {
        is LeaveParse.Valid -> parse.leave
        LeaveParse.EndsBeforeStart -> error("Fixture leave ends before it starts")
    }

fun region(raw: String): HolidayRegion =
    when (val parse = HolidayRegion.parse(raw)) {
        is HolidayRegionParse.Valid -> parse.region
        HolidayRegionParse.Blank -> error("Fixture region is blank")
    }
