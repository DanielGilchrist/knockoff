package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Policy
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

internal fun ruleCalled(name: String): String = name.trim().ifEmpty { "this rule" }

internal fun hoursOf(name: String): String {
    val owner = ruleCalled(name)

    return when {
        owner.endsWith('s', ignoreCase = true) -> "$owner' hours"
        else -> "$owner's hours"
    }
}

internal fun RuleDraft.appEffect(): String =
    when (val checked = schedule()) {
        is Checked.Problems -> {
            ""
        }

        is Checked.Ok -> {
            when (policy) {
                Policy.DeliverOnlyDuring -> "Reaches you only ${checked.value.describeInSentence()}."
                Policy.HoldDuring -> "Held ${checked.value.describeInSentence()}."
            }
        }
    }

internal fun SeenChatsState.describe(loaded: (List<Target.Chat>) -> String): String =
    when (this) {
        SeenChatsState.Loading -> "Loading chats…"
        is SeenChatsState.Failed -> "Couldn't read the chats seen so far: $detail"
        is SeenChatsState.Loaded -> loaded(chats)
    }
