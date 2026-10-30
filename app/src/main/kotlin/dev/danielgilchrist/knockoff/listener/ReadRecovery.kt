package dev.danielgilchrist.knockoff.listener

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal val READ_RETRIES: List<Duration> = listOf(1.seconds, 2.seconds, 4.seconds)
internal val LATER_RETRIES: List<Duration> = listOf(30.seconds, 2.minutes, 10.minutes)

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

internal suspend fun persisting(
    later: List<Duration> = LATER_RETRIES,
    read: () -> NotificationRead,
): NotificationRead =
    later.fold(recovering(read = read)) { last, wait ->
        when (last) {
            is NotificationRead.Unavailable -> {
                delay(wait)
                recovering(read = read)
            }

            is NotificationRead.Read, is NotificationRead.Refused -> {
                last
            }
        }
    }
