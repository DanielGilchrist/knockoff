package dev.danielgilchrist.knockoff.holiday

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.system.SystemCall
import dev.danielgilchrist.knockoff.system.systemCall
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

internal fun calendarChanges(context: Context): Flow<Unit> =
    callbackFlow {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    trySend(Unit)
                }
            }
        val watching = watch(context, observer)

        trySend(Unit)
        awaitClose {
            when (watching) {
                CalendarWatching.Watching -> {
                    systemCall { context.contentResolver.unregisterContentObserver(observer) }
                }

                CalendarWatching.NotWatching -> {}
            }
        }
    }.conflate()

private fun watch(
    context: Context,
    observer: ContentObserver,
): CalendarWatching =
    when (
        val call =
            systemCall { context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer) }
    ) {
        is SystemCall.Done -> {
            CalendarWatching.Watching
        }

        is SystemCall.Failed -> {
            Log.w(LOG_TAG, "not watching calendars: ${call.detail}")
            CalendarWatching.NotWatching
        }
    }
