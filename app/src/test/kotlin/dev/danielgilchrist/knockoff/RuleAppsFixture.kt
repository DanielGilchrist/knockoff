package dev.danielgilchrist.knockoff

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.AppScope
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.NarrowedParse
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
    when (
        val parse =
            AppScope.Narrowed.parse(
                accounts = targets.filterIsInstance<Target.Account>().associate { it.group to it.label },
                chats = targets.filterIsInstance<Target.Chat>().map(ChatPick::of),
                patterns = targets.filterIsInstance<Target.Pattern>().map(PatternPick::of).toSet(),
                asking =
                    when {
                        asks -> NewChatsAsking.Ask
                        else -> NewChatsAsking.DontAsk
                    },
            )
    ) {
        is NarrowedParse.Valid -> parse.scope
        NarrowedParse.NothingChosen -> error("Fixture app is narrowed to nothing")
    }
