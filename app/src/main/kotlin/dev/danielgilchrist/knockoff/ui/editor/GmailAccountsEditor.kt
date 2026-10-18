package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.ui.components.SectionHeader

@Composable
internal fun GmailAccountsEditor(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pending = AccountAddress.parse(draft.pendingGmail)

    Column(modifier = modifier) {
        SectionHeader(text = "Gmail accounts")
        draft.gmailAccounts.forEach { account ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = account.value, modifier = Modifier.weight(1f))
                TextButton(onClick = { onDraftChange(draft.copy(gmailAccounts = draft.gmailAccounts - account)) }) {
                    Text(text = "Remove")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft.pendingGmail,
                onValueChange = { onDraftChange(draft.copy(pendingGmail = it)) },
                label = { Text(text = "Account address") },
                isError = draft.pendingGmail.isNotBlank() && pending is AccountAddressParse.Invalid,
                supportingText = {
                    when (pending) {
                        is AccountAddressParse.Valid -> {
                            Text(text = "Press Add, or it's added when you carry on")
                        }

                        is AccountAddressParse.Invalid -> {
                            if (draft.pendingGmail.isNotBlank()) {
                                Text(
                                    text = pending.detail,
                                )
                            }
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onDraftChange(draft.withPendingGmailAdded()) },
                enabled = pending is AccountAddressParse.Valid,
            ) {
                Text(text = "Add")
            }
        }
    }
}
