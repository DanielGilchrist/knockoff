package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun RuleNameField(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = draft.name,
        onValueChange = { onDraftChange(draft.copy(name = it)) },
        label = { Text(text = "Name") },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}
