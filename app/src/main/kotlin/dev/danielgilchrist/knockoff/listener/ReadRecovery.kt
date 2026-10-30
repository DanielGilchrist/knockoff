package dev.danielgilchrist.knockoff.listener

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val READ_RETRIES: List<Duration> = listOf(1.seconds, 2.seconds, 4.seconds)

internal suspend fun recovering(
    retries: List<Duration> = READ_RETRIES,
    read: () -> NotificationRead,
): NotificationRead =
    retries.fold(read()) { last, wait ->
        when (last) {
            is NotificationRead.Unavailable -> {
                delay(wait)
                read()
            }

            is NotificationRead.Read, is NotificationRead.Refused -> {
                last
            }
        }
    }
