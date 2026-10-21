package dev.danielgilchrist.knockoff.domain

import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.PatternPick
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.Target

fun ruleApps(
    targets: Set<Target>,
    asking: Set<PackageName>,
): RuleApps =
    RuleApps.of(
        (targets.map { it.packageName } + asking)
            .distinct()
            .associateWith { packageName ->
                val own = targets.filter { it.packageName == packageName }

                when {
                    own.any { it == Target.App(packageName) } -> AppScope.Everything
                    else -> narrowed(own, packageName in asking)
                }
            },
    )

private fun narrowed(
    targets: List<Target>,
    asks: Boolean,
): AppScope =
    AppScope.Narrowed.chosen(
        accounts = targets.flatMap(::accountOf).toMap(),
        chats = targets.flatMap(::chatOf),
        patterns = targets.flatMap(::patternOf).toSet(),
        asking =
            when {
                asks -> NewChatsAsking.Ask
                else -> NewChatsAsking.DontAsk
            },
    )

private fun accountOf(target: Target): List<Pair<ChannelGroupId, GroupName>> =
    when (target) {
        is Target.Account -> listOf(target.group to target.label)
        is Target.App, is Target.Chat, is Target.Pattern -> emptyList()
    }

private fun chatOf(target: Target): List<ChatPick> =
    when (target) {
        is Target.Chat -> listOf(ChatPick.of(target))
        is Target.App, is Target.Account, is Target.Pattern -> emptyList()
    }

private fun patternOf(target: Target): List<PatternPick> =
    when (target) {
        is Target.Pattern -> listOf(PatternPick.of(target))
        is Target.App, is Target.Account, is Target.Chat -> emptyList()
    }
