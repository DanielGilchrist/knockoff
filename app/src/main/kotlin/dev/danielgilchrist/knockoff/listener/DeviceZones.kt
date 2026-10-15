package dev.danielgilchrist.knockoff.listener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.ZoneId

internal fun deviceZones(context: Context): Flow<ZoneId> =
    callbackFlow {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    trySend(zoneOf(intent))
                }
            }

        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIMEZONE_CHANGED), Context.RECEIVER_NOT_EXPORTED)
        trySend(ZoneId.systemDefault())
        awaitClose { context.unregisterReceiver(receiver) }
    }.conflate().distinctUntilChanged()

private fun zoneOf(intent: Intent): ZoneId =
    when (val extra = ZoneExtra.of(intent)) {
        is ZoneExtra.Zone -> {
            extra.zone
        }

        ZoneExtra.Missing -> {
            Log.w(LOG_TAG, "time zone change without a zone, using the system default")
            ZoneId.systemDefault()
        }

        is ZoneExtra.Invalid -> {
            Log.w(LOG_TAG, "time zone change with an unusable zone, using the system default: ${extra.detail}")
            ZoneId.systemDefault()
        }
    }
