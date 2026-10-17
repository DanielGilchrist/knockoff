package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import dev.danielgilchrist.knockoff.ui.removedAt
import dev.danielgilchrist.knockoff.ui.replacedAt

internal fun LazyListScope.hoursItems(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
) {
    itemsIndexed(draft.windows) { index, window ->
        WindowEditor(
            number = index + 1,
            window = window,
            onWindowChange = { onDraftChange(draft.copy(windows = draft.windows.replacedAt(index, it))) },
            onRemove = { onDraftChange(draft.copy(windows = draft.windows.removedAt(index))) },
        )
    }
    item {
        OutlinedButton(onClick = { onDraftChange(draft.copy(windows = draft.windows + WindowDraft.WORKING_HOURS)) }) {
            Text(text = "Add hours")
        }
    }
}

internal fun LazyListScope.breakItems(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
) {
    item {
        Column {
            Text(text = "Breaks", style = MaterialTheme.typography.titleSmall)
            Text(
                text =
                    "Time taken out of the hours, such as lunch. A break counts on the day it starts, so in " +
                        "overnight hours a break after midnight needs the following day: for Fri 22:00–06:00, " +
                        "pick Sat for a 02:00 break.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    itemsIndexed(draft.breaks) { index, breakDraft ->
        BreakEditor(
            number = index + 1,
            draft = breakDraft,
            onDraftChange = { onDraftChange(draft.copy(breaks = draft.breaks.replacedAt(index, it))) },
            onRemove = { onDraftChange(draft.copy(breaks = draft.breaks.removedAt(index))) },
        )
    }
    item {
        OutlinedButton(onClick = { onDraftChange(draft.withBreakAdded()) }) {
            Text(text = "Add break")
        }
    }
}
