package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.ui.AppDiscovery
import dev.danielgilchrist.knockoff.ui.GrammaticalNumber
import dev.danielgilchrist.knockoff.ui.GroupNoun
import dev.danielgilchrist.knockoff.ui.HeldPhrase
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.RadioListItem
import dev.danielgilchrist.knockoff.ui.components.SubHeading

private val NESTED_INDENT = 40.dp

@Composable
internal fun IncludeChoice(
    app: PackageName,
    label: String,
    rule: RuleDraft,
    draft: AppDraft,
    kind: AppKind,
    accounts: AccountChoices,
    seen: SeenChatsState,
    available: Int,
    discovery: AppDiscovery,
    onDraftChange: (AppDraft) -> Unit,
) {
    val narrowed = draft.mode == AppMode.Narrowed

    ListGroup(modifier = Modifier.selectableGroup()) {
        RadioListItem(
            headline = "All notifications",
            supporting = "Everything $label sends.",
            selected = draft.mode == AppMode.Everything,
            onClick = { onDraftChange(draft.copy(mode = AppMode.Everything)) },
        )
        if (draft.offersNarrowing(available)) {
            RadioListItem(
                headline = kind.narrowed(accounts.noun),
                supporting = narrowedSupporting(kind, accounts, narrowed),
                selected = narrowed,
                onClick = { onDraftChange(draft.copy(mode = AppMode.Narrowed)) },
            )
        }
        AnimatedVisibility(
            visible = narrowed,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(start = NESTED_INDENT)) {
                val hint: @Composable () -> Unit = {
                    if (draft.leavesNothingChosen(app)) {
                        Caption(
                            text = unchosenCaption(kind, accounts.noun),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }

                AccountSection(label, kind, accounts, draft, discovery, onDraftChange)
                when {
                    kind.hasChats -> {
                        if (kind.hasAccounts) ChatsHeading()
                        ChatRows(
                            app = app,
                            label = label,
                            ruleName = rule.name,
                            draft = draft,
                            seen = seen,
                            onDraftChange = onDraftChange,
                            beforeAsking = hint,
                        )
                    }

                    else -> {
                        hint()
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSection(
    label: String,
    kind: AppKind,
    accounts: AccountChoices,
    draft: AppDraft,
    discovery: AppDiscovery,
    onDraftChange: (AppDraft) -> Unit,
) {
    if (kind.hasAccounts) {
        if (kind.hasChats) AccountsHeading(label = label, noun = accounts.noun)
        AccountRows(accounts = accounts, draft = draft, discovery = discovery, onDraftChange = onDraftChange)
    }
}

@Composable
private fun ChatsHeading() {
    Spacer(modifier = Modifier.height(8.dp))
    SubHeading(text = "Chats")
}

@Composable
private fun AccountsHeading(
    label: String,
    noun: GroupNoun,
) {
    when (noun) {
        GroupNoun.Accounts -> {
            SubHeading(text = "Accounts")
        }

        GroupNoun.Categories -> {
            SubHeading(text = "Categories")
            Caption(
                text = "How $label sorts its own notifications.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

internal fun narrowedSupporting(
    kind: AppKind,
    accounts: AccountChoices,
    selected: Boolean,
): String {
    val choose =
        when {
            !selected && kind.hasAccounts && accounts.offered.isNotEmpty() -> {
                "Choose from ${accounts.noun.counted(accounts.offered.size)}. "
            }

            else -> {
                ""
            }
        }

    return "${choose}This rule leaves the rest alone."
}

internal fun unchosenCaption(
    kind: AppKind,
    noun: GroupNoun,
): String =
    when (kind) {
        AppKind.Plain -> "Add at least one pattern."

        AppKind.Accounts -> "Tick at least one ${kind.pick(noun)}."

        AppKind.Chats, AppKind.AccountsAndChats -> "Tick at least one ${kind.pick(
            noun,
        )}, or turn on Ask me about new chats."
    }

internal fun appHeld(
    label: String,
    mode: AppMode,
    kind: AppKind,
    noun: GroupNoun,
): HeldPhrase =
    when (mode) {
        AppMode.Everything -> {
            HeldPhrase.of(label, GrammaticalNumber.One)
        }

        AppMode.Narrowed -> {
            when (kind) {
                AppKind.Plain -> {
                    HeldPhrase.of("Notifications matching your patterns", GrammaticalNumber.Many)
                }

                AppKind.Accounts, AppKind.Chats, AppKind.AccountsAndChats -> {
                    HeldPhrase.of("The ${kind.more(noun)} you pick", GrammaticalNumber.Many)
                }
            }
        }
    }

internal fun moreCaption(
    label: String,
    kind: AppKind,
    noun: GroupNoun,
): String =
    when (kind) {
        AppKind.Plain -> {
            "Knockoff learns an app's accounts and chats from its notifications. If $label has them, " +
                "they'll show here once it has notified you."
        }

        AppKind.Accounts, AppKind.Chats, AppKind.AccountsAndChats -> {
            "If $label has more ${kind.more(noun)}, they'll show here once each has notified you."
        }
    }
