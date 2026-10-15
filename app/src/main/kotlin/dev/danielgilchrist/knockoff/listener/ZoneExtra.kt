package dev.danielgilchrist.knockoff.listener

import android.content.Intent
import java.time.DateTimeException
import java.time.ZoneId

internal sealed interface ZoneExtra {
    data class Zone(
        val zone: ZoneId,
    ) : ZoneExtra

    data object Missing : ZoneExtra

    data class Invalid(
        val detail: String,
    ) : ZoneExtra

    companion object {
        fun of(intent: Intent): ZoneExtra =
            when (val raw = intent.getStringExtra(Intent.EXTRA_TIMEZONE)) {
                null -> {
                    Missing
                }

                else -> {
                    try {
                        Zone(ZoneId.of(raw))
                    } catch (exception: DateTimeException) {
                        Invalid(exception.toString())
                    }
                }
            }
    }
}
