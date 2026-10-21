package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.editor.Checked
import dev.danielgilchrist.knockoff.ui.editor.RuleDraft

internal fun counted(
    count: Int,
    one: String,
    many: String,
): String =
    when (count) {
        1 -> "1 $one"
        else -> "$count $many"
    }

internal fun withArticle(noun: String): String =
    when (noun.firstOrNull()?.lowercaseChar()) {
        'a', 'e', 'i', 'o', 'u' -> "an $noun"
        else -> "a $noun"
    }

internal fun ruleCalled(name: String): String = name.trim().ifEmpty { "this rule" }

internal fun hoursOf(name: String): String = "${possessive(ruleCalled(name))} hours"

internal fun possessive(owner: String): String =
    when {
        owner.endsWith('s', ignoreCase = true) -> "$owner'"
        else -> "$owner's"
    }

internal fun RuleDraft.appEffect(held: HeldPhrase): String =
    when (val checked = schedule()) {
        is Checked.Problems -> ""
        is Checked.Ok -> HeldPhrases.effect(held, policy, checked.value.describeInSentence())
    }

internal fun SeenChatsState.describe(loaded: (List<Target.Chat>) -> String): String =
    when (this) {
        SeenChatsState.Loading -> "Loading chats…"
        is SeenChatsState.Failed -> "Couldn't read the chats seen so far: $detail"
        is SeenChatsState.Loaded -> loaded(chats)
    }
