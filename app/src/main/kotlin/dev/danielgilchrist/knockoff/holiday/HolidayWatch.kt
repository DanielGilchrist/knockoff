package dev.danielgilchrist.knockoff.holiday

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import dev.danielgilchrist.knockoff.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import java.time.LocalDate

class HolidayWatch internal constructor(
    private val access: CalendarAccess,
    settings: SettingsStore,
    changes: () -> Flow<Unit>,
    scope: CoroutineScope,
) {
    private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HolidaysState> =
        combine(chosen(settings), refreshes.onStart { emit(Unit) }.flatMapLatest { changes() }) { calendar, _ ->
            calendar
        }.conflate()
            .scan<HolidayCalendar, HolidaysState>(
                HolidaysState.Loading,
            ) { previous, calendar -> loaded(calendar, previous) }
            .stateIn(scope, SharingStarted.Eagerly, HolidaysState.Loading)

    val holidays: Flow<Holidays> =
        state
            .transform { state ->
                when (state) {
                    HolidaysState.Loading -> {}

                    is HolidaysState.Loaded -> {
                        emit(state.holidays)
                    }
                }
            }.distinctUntilChanged()

    fun refresh() {
        refreshes.tryEmit(Unit)
    }

    suspend fun current(): Holidays = holidays.first()

    private fun loaded(
        calendar: HolidayCalendar,
        previous: HolidaysState,
    ): HolidaysState.Loaded =
        when (calendar) {
            HolidayCalendar.None -> HolidaysState.NotChosen
            is HolidayCalendar.Chosen -> HolidaysState.Chosen.of(calendar, read(calendar), previous)
        }

    private fun read(calendar: HolidayCalendar.Chosen): HolidayRead {
        val read = access.events(calendar.calendar, LocalDate.now())

        when (read) {
            is HolidayRead.Read -> {
                Log.i(LOG_TAG, "holiday calendar read: ${read.events.size} all-day events")
            }

            HolidayRead.NoPermission -> {
                Log.w(LOG_TAG, "holiday calendar unread: no calendar permission")
            }

            HolidayRead.Missing -> {
                Log.w(LOG_TAG, "holiday calendar unread: it's no longer on the device")
            }

            is HolidayRead.Failed -> {
                Log.w(
                    LOG_TAG,
                    "holiday calendar unread, keeping the last holidays: ${read.detail}",
                )
            }
        }

        return read
    }

    private fun chosen(settings: SettingsStore): Flow<HolidayCalendar> =
        settings.loads
            .transform { load ->
                when (load) {
                    is SettingsLoad.Usable -> {
                        emit(load.settings.holidayCalendar)
                    }

                    is SettingsLoad.Failure -> {}
                }
            }.distinctUntilChanged()
}
