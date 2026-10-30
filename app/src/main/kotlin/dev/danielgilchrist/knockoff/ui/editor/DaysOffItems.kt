package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import dev.danielgilchrist.knockoff.domain.dayoff.PublicHolidays
import dev.danielgilchrist.knockoff.ui.components.AddButton
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.components.SwitchListItem
import dev.danielgilchrist.knockoff.ui.components.groupedItemColors
import dev.danielgilchrist.knockoff.ui.daysOffCaption
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.describeSpan
import java.time.LocalDate

internal fun LazyListScope.daysOffItems(
    draft: RuleDraft,
    facts: SectionFacts,
    onDraftChange: (RuleDraft) -> Unit,
) {
    val today = facts.today
    val daysOff = draft.daysOff
    val leave = daysOff.upcoming(today)

    item {
        Caption(text = facts.policy.daysOffCaption())
    }
    item { SectionHeader(text = "Public holidays") }
    item {
        ListGroup {
            SwitchListItem(
                headline = "Take public holidays off",
                supporting = facts.holidaySource,
                checked =
                    when (daysOff.publicHolidays) {
                        PublicHolidays.Off -> true
                        PublicHolidays.AsUsual -> false
                    },
                onToggle = {
                    val toggled =
                        when (daysOff.publicHolidays) {
                            PublicHolidays.Off -> PublicHolidays.AsUsual
                            PublicHolidays.AsUsual -> PublicHolidays.Off
                        }

                    onDraftChange(draft.copy(daysOff = daysOff.copy(publicHolidays = toggled)))
                },
            )
        }
    }
    item { SectionHeader(text = "Other days off") }
    when {
        leave.isEmpty() -> {
            item { Caption(text = "None booked.") }
        }

        else -> {
            item {
                ListGroup {
                    leave.forEach { booked ->
                        LeaveRow(
                            leave = booked,
                            today = today,
                            onRemove = { onDraftChange(draft.copy(daysOff = daysOff.cancelling(booked))) },
                        )
                    }
                }
            }
        }
    }
    item {
        AddLeave(today = today, onAdd = { onDraftChange(draft.copy(daysOff = daysOff.taking(it, today))) })
    }
}

@Composable
private fun LeaveRow(
    leave: Leave,
    today: LocalDate,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = leave.describe(today)

    ListItem(
        headlineContent = { Text(text = label) },
        supportingContent = { Text(text = leave.describeSpan()) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Remove $label")
            }
        },
        colors = groupedItemColors(),
        modifier = modifier,
    )
}

@Composable
private fun AddLeave(
    today: LocalDate,
    onAdd: (Leave) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    AddButton(
        text = "Add days off",
        supporting = "Such as a sick day or annual leave",
        onClick = { picking = true },
        modifier = modifier,
    )
    if (picking) {
        LeavePicker(
            today = today,
            onPick = {
                picking = false
                onAdd(it)
            },
            onDismiss = { picking = false },
        )
    }
}
