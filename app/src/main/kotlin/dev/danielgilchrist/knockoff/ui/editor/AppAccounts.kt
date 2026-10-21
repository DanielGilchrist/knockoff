package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.Composable
import dev.danielgilchrist.knockoff.ui.AppDiscovery
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.toggled

@Composable
internal fun AccountRows(
    accounts: AccountChoices,
    draft: AppDraft,
    discovery: AppDiscovery,
    onDraftChange: (AppDraft) -> Unit,
) {
    accounts.offered.forEach { (id, name) ->
        CheckboxListItem(
            headline = name.value,
            checked = id in draft.accounts,
            supporting =
                when (accounts.sightingOf(id, discovery)) {
                    AccountSighting.Seen -> ""
                    AccountSighting.NotChecked -> "Not checked yet"
                    AccountSighting.CouldNotCheck -> "Couldn't check"
                    AccountSighting.NotOnPhone -> "Not on this phone"
                },
            onToggle = { onDraftChange(draft.copy(accounts = draft.accounts.toggled(id, name))) },
        )
    }
}
