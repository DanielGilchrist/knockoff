package dev.danielgilchrist.knockoff.ui.preferences

import dev.danielgilchrist.knockoff.holiday.CalendarList
import dev.danielgilchrist.knockoff.holiday.DeviceCalendar
import dev.danielgilchrist.knockoff.ui.PermissionAsk

sealed interface CalendarChoices {
    data object Loading : CalendarChoices

    data class Listed(
        val calendars: List<DeviceCalendar>,
    ) : CalendarChoices

    data object NotAllowed : CalendarChoices

    data object Blocked : CalendarChoices

    data class Failed(
        val detail: String,
    ) : CalendarChoices

    companion object {
        fun of(
            list: CalendarList,
            ask: PermissionAsk,
        ): CalendarChoices =
            when (list) {
                is CalendarList.Listed -> {
                    Listed(list.calendars.sortedBy { it.name.lowercase() })
                }

                is CalendarList.Failed -> {
                    Failed(list.detail)
                }

                CalendarList.NoPermission -> {
                    when (ask) {
                        PermissionAsk.Askable -> NotAllowed
                        PermissionAsk.Blocked -> Blocked
                    }
                }
            }
    }
}
