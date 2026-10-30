package dev.danielgilchrist.knockoff.ui.preferences

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayEvent
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayRegion
import dev.danielgilchrist.knockoff.holiday.CalendarKind
import dev.danielgilchrist.knockoff.holiday.DeviceCalendar
import dev.danielgilchrist.knockoff.holiday.HolidayRead
import dev.danielgilchrist.knockoff.holiday.HolidaysState
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.ListScreen
import dev.danielgilchrist.knockoff.ui.components.NoticeAction
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone
import dev.danielgilchrist.knockoff.ui.components.RadioListItem
import dev.danielgilchrist.knockoff.ui.components.ScreenNavigation
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.describeSource
import dev.danielgilchrist.knockoff.ui.saveProblem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SHOWN_HOLIDAYS = 5
private val HOLIDAY_DATE = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.UK)

@Composable
internal fun HolidaysScreen(
    chosen: HolidayCalendar,
    calendars: CalendarChoices,
    holidays: HolidaysState,
    saveStatus: SaveStatus,
    actions: HolidaysActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)

    ListScreen(title = "Public holidays", navigation = ScreenNavigation.Back(actions.onBack), modifier = modifier) {
        saveProblem(saveStatus)
        item {
            Caption(
                text =
                    "Rules that take public holidays off treat the days in this calendar as days off. " +
                        "Google Calendar can add one for your country under Settings › Holidays in your region.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        when (calendars) {
            CalendarChoices.Loading -> {
                item { Caption(text = "Finding calendars…", modifier = Modifier.padding(horizontal = 16.dp)) }
            }

            CalendarChoices.NotAllowed -> {
                item { AccessNotice(action = NoticeAction.Button("Allow", actions.onAllowCalendar)) }
            }

            CalendarChoices.Blocked -> {
                item { AccessNotice(action = NoticeAction.Button("Open settings", actions.onOpenAppSettings)) }
            }

            is CalendarChoices.Failed -> {
                item {
                    NoticeCard(
                        title = "Calendars couldn't be listed",
                        body = calendars.detail,
                        tone = NoticeTone.Problem,
                    )
                }
            }

            is CalendarChoices.Listed -> {
                listedItems(chosen, calendars.calendars, holidays, actions)
            }
        }
    }
}

private fun LazyListScope.listedItems(
    chosen: HolidayCalendar,
    calendars: List<DeviceCalendar>,
    holidays: HolidaysState,
    actions: HolidaysActions,
) {
    val others = calendars.filter { it.kind == CalendarKind.Other }

    item { SectionHeader(text = "Holiday calendars") }
    item {
        ListGroup {
            RadioListItem(
                headline = "None",
                selected = chosen == HolidayCalendar.None,
                onClick = { actions.onChoose(HolidayCalendar.None) },
            )
            CalendarRows(calendars.filter { it.kind == CalendarKind.Holidays }, chosen, actions)
        }
    }
    when (chosen) {
        HolidayCalendar.None -> {}

        is HolidayCalendar.Chosen -> {
            chosenItems(chosen, holidays, actions)
        }
    }
    if (others.isNotEmpty()) {
        item { SectionHeader(text = "Other calendars") }
        item {
            Caption(
                text = "Every all-day event in one of these counts as a public holiday.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item { ListGroup { CalendarRows(others, chosen, actions) } }
    }
}

private fun LazyListScope.chosenItems(
    chosen: HolidayCalendar.Chosen,
    holidays: HolidaysState,
    actions: HolidaysActions,
) {
    when (holidays) {
        HolidaysState.Loading, HolidaysState.NotChosen -> {}

        is HolidaysState.Chosen -> {
            when (val read = holidays.read) {
                is HolidayRead.Read -> {
                    regionItems(chosen, HolidayEvent.regionsIn(read.events), actions)
                    upcomingItems(holidays)
                }

                HolidayRead.NoPermission, HolidayRead.Missing, is HolidayRead.Failed -> {
                    item {
                        NoticeCard(
                            title = "Holidays couldn't be read",
                            body = holidays.describeSource(),
                            tone = NoticeTone.Problem,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.regionItems(
    chosen: HolidayCalendar.Chosen,
    regions: Set<HolidayRegion>,
    actions: HolidaysActions,
) {
    if (regions.isNotEmpty()) {
        item { SectionHeader(text = "Regions") }
        when {
            chosen.regions.isEmpty() -> {
                item {
                    NoticeCard(
                        title = "Tick your region",
                        body =
                            "Some holidays in this calendar, possibly even national ones, " +
                                "only count in the regions you tick.",
                        tone = NoticeTone.Nudge,
                    )
                }
            }

            else -> {
                item {
                    Caption(
                        text = "Holidays for only some regions count once you tick yours.",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
        item {
            ListGroup {
                regions.sortedBy { it.value }.forEach { region ->
                    CheckboxListItem(
                        headline = region.value,
                        checked = region in chosen.regions,
                        onToggle = {
                            val toggled =
                                when (region) {
                                    in chosen.regions -> chosen.regions - region
                                    else -> chosen.regions + region
                                }

                            actions.onChoose(chosen.copy(regions = toggled))
                        },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.upcomingItems(holidays: HolidaysState.Chosen) {
    val today = LocalDate.now()
    val upcoming =
        holidays.holidays.dates
            .filter { it >= today }
            .sorted()
            .take(SHOWN_HOLIDAYS)

    item { SectionHeader(text = "Coming up") }
    when {
        upcoming.isEmpty() -> {
            item {
                Caption(
                    text = "No public holidays in the next two years.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        else -> {
            item {
                Caption(
                    text = upcoming.joinToString("\n") { it.format(HOLIDAY_DATE) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarRows(
    calendars: List<DeviceCalendar>,
    chosen: HolidayCalendar,
    actions: HolidaysActions,
) {
    val shared =
        calendars
            .groupingBy { it.name }
            .eachCount()
            .filterValues { it > 1 }
            .keys

    calendars.forEach { calendar ->
        RadioListItem(
            headline = calendar.name,
            supporting = if (calendar.name in shared) calendar.account else "",
            selected =
                when (chosen) {
                    HolidayCalendar.None -> false
                    is HolidayCalendar.Chosen -> chosen.calendar == calendar.id
                },
            onClick = { actions.onChoose(HolidayCalendar.Chosen(calendar.id, emptySet())) },
        )
    }
}

@Composable
private fun AccessNotice(
    action: NoticeAction,
    modifier: Modifier = Modifier,
) {
    NoticeCard(
        title = "Calendar access needed",
        body = "Knockoff reads public holidays from a calendar on this phone. Nothing leaves the phone.",
        tone = NoticeTone.Nudge,
        action = action,
        modifier = modifier,
    )
}
