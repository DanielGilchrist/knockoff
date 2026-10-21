package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.ui.GroupNoun
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.RadioListItem

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
                supporting = narrowedSupporting(kind, accounts, rule.policy, narrowed),
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
                if (kind.hasAccounts) AccountRows(accounts = accounts, draft = draft, onDraftChange = onDraftChange)
                if (kind.hasChats) {
                    ChatRows(
                        app = app,
                        label = label,
                        ruleName = rule.name,
                        draft = draft,
                        seen = seen,
                        onDraftChange = onDraftChange,
                    )
                }
            }
        }
    }
}

internal fun narrowedSupporting(
    kind: AppKind,
    accounts: AccountChoices,
    policy: Policy,
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
    val others =
        when (policy) {
            Policy.DeliverOnlyDuring -> "Others always reach you."
            Policy.HoldDuring -> "Others are never held."
        }

    return choose + others
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
