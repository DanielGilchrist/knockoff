package dev.danielgilchrist.knockoff.holiday

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import dev.danielgilchrist.knockoff.domain.dayoff.CalendarId
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayEvent
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal class CalendarAccess(
    private val context: Context,
) {
    companion object {
        private const val YEARS_AHEAD = 2L
        private const val ALL_DAY = 1
        private const val CALENDAR_ID = 0
        private const val CALENDAR_NAME = 1
        private const val CALENDAR_OWNER = 2
        private const val CALENDAR_ACCOUNT = 3
        private const val EVENT_BEGIN = 0
        private const val EVENT_END = 1
        private const val EVENT_ALL_DAY = 2
        private const val EVENT_DESCRIPTION = 3
        private val CALENDAR_COLUMNS =
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.OWNER_ACCOUNT,
                CalendarContract.Calendars.ACCOUNT_NAME,
            )
        private const val SYNCED = "${CalendarContract.Calendars.SYNC_EVENTS} = 1"
        private val EVENT_COLUMNS =
            arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.DESCRIPTION,
            )
    }

    fun calendars(): CalendarList =
        when (val query = query(CalendarContract.Calendars.CONTENT_URI, CALENDAR_COLUMNS, SYNCED, ::calendarsIn)) {
            is CalendarQuery.Rows -> CalendarList.Listed(query.value)
            CalendarQuery.NoPermission -> CalendarList.NoPermission
            is CalendarQuery.Failed -> CalendarList.Failed(query.detail)
        }

    fun events(
        id: CalendarId,
        today: LocalDate,
    ): HolidayRead =
        when (val list = calendars()) {
            CalendarList.NoPermission -> {
                HolidayRead.NoPermission
            }

            is CalendarList.Failed -> {
                HolidayRead.Failed(list.detail)
            }

            is CalendarList.Listed -> {
                when (val calendar = list.calendars.firstOrNull { it.id == id }) {
                    null -> HolidayRead.Missing
                    else -> eventsOf(calendar, today)
                }
            }
        }

    private fun eventsOf(
        calendar: DeviceCalendar,
        today: LocalDate,
    ): HolidayRead {
        val uri =
            CalendarContract.Instances.CONTENT_URI
                .buildUpon()
                .also { ContentUris.appendId(it, startOfUtc(today.minusDays(1))) }
                .also { ContentUris.appendId(it, startOfUtc(today.plusYears(YEARS_AHEAD))) }
                .build()
        val selection = "${CalendarContract.Instances.CALENDAR_ID} = ${calendar.id.value}"

        return when (val query = query(uri, EVENT_COLUMNS, selection, ::eventsIn)) {
            is CalendarQuery.Rows -> HolidayRead.Read(calendar, query.value)
            CalendarQuery.NoPermission -> HolidayRead.NoPermission
            is CalendarQuery.Failed -> HolidayRead.Failed(query.detail)
        }
    }

    private fun <T> query(
        uri: Uri,
        columns: Array<String>,
        selection: String?,
        read: (Cursor) -> T,
    ): CalendarQuery<T> =
        when (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)) {
            PackageManager.PERMISSION_GRANTED -> permittedQuery(uri, columns, selection, read)
            else -> CalendarQuery.NoPermission
        }

    private fun <T> permittedQuery(
        uri: Uri,
        columns: Array<String>,
        selection: String?,
        read: (Cursor) -> T,
    ): CalendarQuery<T> =
        try {
            when (val cursor = context.contentResolver.query(uri, columns, selection, null, null)) {
                null -> CalendarQuery.Failed("the calendar provider returned nothing for $uri")
                else -> cursor.use { CalendarQuery.Rows(read(it)) }
            }
        } catch (_: SecurityException) {
            CalendarQuery.NoPermission
        }

    private fun calendarsIn(cursor: Cursor): List<DeviceCalendar> =
        generateSequence { if (cursor.moveToNext()) cursor else null }
            .map {
                DeviceCalendar(
                    CalendarId(it.getLong(CALENDAR_ID)),
                    it.getString(CALENDAR_NAME).orEmpty(),
                    it.getString(CALENDAR_ACCOUNT).orEmpty(),
                    CalendarKind.of(it.getString(CALENDAR_OWNER).orEmpty()),
                )
            }.toList()

    private fun eventsIn(cursor: Cursor): List<HolidayEvent> =
        generateSequence { if (cursor.moveToNext()) cursor else null }
            .filter { it.getInt(EVENT_ALL_DAY) == ALL_DAY }
            .flatMap { row ->
                HolidayEvent.spanning(
                    utcDate(row.getLong(EVENT_BEGIN)),
                    utcDate(row.getLong(EVENT_END)),
                    HolidayKind.of(row.getString(EVENT_DESCRIPTION).orEmpty()),
                )
            }.toList()

    private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    private fun startOfUtc(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
