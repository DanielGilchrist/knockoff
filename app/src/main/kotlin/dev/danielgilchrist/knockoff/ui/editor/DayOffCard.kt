package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.dayoff.DayOff
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.ui.components.IconBadge
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.NoticeAction
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone
import dev.danielgilchrist.knockoff.ui.components.groupedItemColors
import dev.danielgilchrist.knockoff.ui.dayOffEffect
import dev.danielgilchrist.knockoff.ui.takeTodayOff
import dev.danielgilchrist.knockoff.ui.takeTodayOffEffect

@Composable
internal fun DayOffCard(
    dayOff: DayOff,
    policy: Policy,
    apps: String,
    actions: DayOffActions,
    modifier: Modifier = Modifier,
) {
    when (dayOff) {
        DayOff.Scheduled -> {
            ListGroup(modifier = modifier) {
                ListItem(
                    headlineContent = { Text(text = policy.takeTodayOff()) },
                    supportingContent = { Text(text = policy.takeTodayOffEffect(apps)) },
                    leadingContent = { IconBadge(icon = R.drawable.ic_event) },
                    colors = groupedItemColors(),
                    modifier = Modifier.clickable(onClick = actions.onTakeTodayOff),
                )
            }
        }

        DayOff.OnLeave -> {
            NoticeCard(
                title = "Day off today",
                body = policy.dayOffEffect(apps),
                tone = NoticeTone.Status,
                action = NoticeAction.Button("End day off", actions.onEndDayOff),
                modifier = modifier,
            )
        }

        DayOff.PublicHoliday -> {
            NoticeCard(
                title = "Public holiday today",
                body = policy.dayOffEffect(apps),
                tone = NoticeTone.Status,
                action = NoticeAction.Button("Change days off", actions.onEditDaysOff),
                modifier = modifier,
            )
        }
    }
}
