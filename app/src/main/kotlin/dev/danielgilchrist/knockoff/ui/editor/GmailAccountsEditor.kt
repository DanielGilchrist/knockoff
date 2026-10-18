package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.ui.components.CategoryCard
import dev.danielgilchrist.knockoff.ui.components.RemovableChip

@Composable
internal fun GmailAccountsEditor(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pending = AccountAddress.parse(draft.pendingGmail)
    val add = { onDraftChange(draft.withPendingGmailAdded()) }

    CategoryCard(
        icon = R.drawable.ic_mail,
        title = "Gmail accounts",
        count = draft.gmailAccounts.size,
        modifier = modifier,
    ) {
        if (draft.gmailAccounts.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                draft.gmailAccounts.forEach { account ->
                    RemovableChip(
                        label = account.value,
                        onRemove = { onDraftChange(draft.copy(gmailAccounts = draft.gmailAccounts - account)) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = draft.pendingGmail,
            onValueChange = { onDraftChange(draft.copy(pendingGmail = it)) },
            label = { Text(text = "Add an account") },
            placeholder = { Text(text = "name@company.com") },
            isError = draft.pendingGmail.isNotBlank() && pending is AccountAddressParse.Invalid,
            supportingText = { Text(text = hint(draft.pendingGmail, pending)) },
            trailingIcon = {
                IconButton(onClick = add, enabled = pending is AccountAddressParse.Valid) {
                    Icon(painter = painterResource(R.drawable.ic_add), contentDescription = "Add account")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { add() }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun hint(
    typed: String,
    pending: AccountAddressParse,
): String =
    when (pending) {
        is AccountAddressParse.Valid -> {
            "Tap + or Done to add it"
        }

        is AccountAddressParse.Invalid -> {
            when {
                typed.isBlank() -> "Notifications from this inbox are held"
                else -> pending.detail
            }
        }
    }
