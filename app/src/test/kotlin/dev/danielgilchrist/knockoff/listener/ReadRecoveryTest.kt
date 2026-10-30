package dev.danielgilchrist.knockoff.listener

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadRecoveryTest {
    private val unavailable = NotificationRead.Unavailable("binder full")
    private val read = NotificationRead.Read(emptyList())

    @Test
    fun `a read the system couldn't send is retried with growing waits until it works`() =
        runTest {
            val results = mutableListOf(unavailable, unavailable, read)

            assertEquals(read, recovering { results.removeAt(0) })
            assertEquals(3_000L, currentTime)
        }

    @Test
    fun `it gives up after the last retry`() =
        runTest {
            var reads = 0

            assertEquals(
                unavailable,
                recovering {
                    reads++
                    unavailable
                },
            )
            assertEquals(4, reads)
            assertEquals(7_000L, currentTime)
        }

    @Test
    fun `reads that worked or were refused aren't retried`() =
        runTest {
            listOf(NotificationRead.Refused("not allowed"), read).forEach { result ->
                var reads = 0

                assertEquals(
                    result,
                    recovering {
                        reads++
                        result
                    },
                )
                assertEquals(1, reads)
            }
            assertEquals(0L, currentTime)
        }
}
