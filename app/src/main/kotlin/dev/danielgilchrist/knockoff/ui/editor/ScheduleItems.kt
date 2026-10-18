package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import dev.danielgilchrist.knockoff.ui.components.AddButton
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
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
        AddButton(
            text = "Add more hours",
            onClick = { onDraftChange(draft.copy(windows = draft.windows + WindowDraft.WORKING_HOURS)) },
        )
    }
}

internal fun LazyListScope.breakItems(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
) {
    item {
        Column {
            SectionHeader(text = "Breaks")
            Caption(text = "Time taken out of the hours, such as lunch.")
            if (draft.windows.any { it.runsPastMidnight() }) {
                Caption(
                    text = "A break counts on the day it starts. For Fri 22:00 to 06:00, pick Sat for a 02:00 break.",
                )
            }
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
        AddButton(text = "Add break", onClick = { onDraftChange(draft.withBreakAdded()) })
    }
}
