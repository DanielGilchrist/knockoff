package dev.danielgilchrist.knockoff.channel

import android.app.NotificationManager
import dev.danielgilchrist.knockoff.domain.channel.Importance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AndroidImportanceTest {
    @Test
    fun `every importance maps to its Android level`() {
        assertEquals(
            listOf(
                NotificationManager.IMPORTANCE_NONE,
                NotificationManager.IMPORTANCE_MIN,
                NotificationManager.IMPORTANCE_LOW,
                NotificationManager.IMPORTANCE_DEFAULT,
                NotificationManager.IMPORTANCE_HIGH,
                NotificationManager.IMPORTANCE_MAX,
            ),
            Importance.entries.map(AndroidImportance::level),
        )
    }

    @Test
    fun `every Android level parses back to its importance`() {
        assertEquals(
            Importance.entries.map { ImportanceParse.Known(it) },
            Importance.entries.map { AndroidImportance.parse(AndroidImportance.level(it)) },
        )
    }

    @Test
    fun `an unspecified or unknown level is unknown`() {
        assertEquals(
            listOf(ImportanceParse.Unknown(NotificationManager.IMPORTANCE_UNSPECIFIED), ImportanceParse.Unknown(42)),
            listOf(NotificationManager.IMPORTANCE_UNSPECIFIED, 42).map(AndroidImportance::parse),
        )
    }
}
