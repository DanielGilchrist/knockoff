package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.Composable
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.toggled

@Composable
internal fun AccountRows(
    accounts: AccountChoices,
    draft: AppDraft,
    onDraftChange: (AppDraft) -> Unit,
) {
    accounts.offered.forEach { (id, name) ->
        CheckboxListItem(
            headline = name.value,
            checked = id in draft.accounts,
            onToggle = { onDraftChange(draft.copy(accounts = draft.accounts.toggled(id, name))) },
        )
    }
}
