package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.ui.components.AddButton

private val FieldSaver =
    listSaver<AddressField, String>(
        save = { field ->
            when (field) {
                AddressField.Closed -> emptyList()
                is AddressField.Open -> listOf(field.text)
            }
        },
        restore = { saved ->
            when (saved.size) {
                0 -> AddressField.Closed
                else -> AddressField.Open(saved.first(), AddressProblem.None)
            }
        },
    )

@Composable
internal fun AddressEntry(
    offered: Map<AccountId, GroupName>,
    draft: AppDraft,
    onDraftChange: (AppDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var field by rememberSaveable(stateSaver = FieldSaver) { mutableStateOf<AddressField>(AddressField.Closed) }

    when (val current = field) {
        AddressField.Closed -> {
            AddButton(
                text = "Add an account by address",
                onClick = { field = AddressField.EMPTY },
                modifier = modifier,
            )
        }

        is AddressField.Open -> {
            OpenField(
                current = current,
                onField = { field = it },
                submission = { current.submitted(offered, draft.accounts.keys) },
                onSubmit = { applied(it, draft, onDraftChange) },
                modifier = modifier,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OpenField(
    current: AddressField.Open,
    onField: (AddressField) -> Unit,
    submission: () -> AddressSubmission,
    onSubmit: (AddressSubmission) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    val inView = remember { BringIntoViewRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val close = {
        focusManager.clearFocus()
        onField(AddressField.Closed)
    }
    val submit = {
        val submitted = submission()
        val next = current.answered(submitted)

        onSubmit(submitted)
        when (next) {
            AddressField.Closed -> close()
            is AddressField.Open -> onField(next)
        }
    }

    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    val keyboardHeight = WindowInsets.ime.getBottom(LocalDensity.current)

    LaunchedEffect(current.problem, keyboardHeight) {
        withFrameNanos {}
        inView.bringIntoView()
    }

    Column(
        modifier = modifier.bringIntoViewRequester(inView),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = current.text,
            onValueChange = { onField(current.edited(it)) },
            label = { Text(text = "Email address") },
            singleLine = true,
            isError = current.problem is AddressProblem.Shown,
            supportingText = { ProblemText(current.problem) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (current.text.isNotBlank()) submit() }),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = close) { Text(text = "Cancel") }
            TextButton(onClick = submit, enabled = current.text.isNotBlank()) { Text(text = "Add") }
        }
    }
}

@Composable
private fun ProblemText(problem: AddressProblem) {
    when (problem) {
        AddressProblem.None -> {}

        is AddressProblem.Shown -> {
            Text(text = problem.message)
        }
    }
}

private fun applied(
    submission: AddressSubmission,
    draft: AppDraft,
    onDraftChange: (AppDraft) -> Unit,
) {
    when (submission) {
        is AddressSubmission.Added -> {
            onDraftChange(draft.withAddress(submission.address))
        }

        is AddressSubmission.TicksListed -> {
            onDraftChange(draft.withAddress(submission.address))
        }

        is AddressSubmission.MatchesGroup -> {
            onDraftChange(draft.withAccount(submission.id, submission.name))
        }

        AddressSubmission.Invalid, is AddressSubmission.Duplicate -> {}
    }
}
