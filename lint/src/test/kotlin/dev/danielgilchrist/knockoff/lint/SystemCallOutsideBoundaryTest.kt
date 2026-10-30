package dev.danielgilchrist.knockoff.lint

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SystemCallOutsideBoundaryTest {
    private val rule =
        SystemCallOutsideBoundary(
            TestConfig("systemCalls" to listOf("snoozeNotification", "activeNotifications")),
        )

    @Test
    fun `a system call inside systemCall passes`() {
        val code =
            """
            fun snooze(listener: Listener) = systemCall { listener.snoozeNotification("key", 1L) }

            fun read(listener: Listener) = systemCall { listener.activeNotifications }
            """.trimIndent()

        assertEquals(emptyList<String>(), messages(code))
    }

    @Test
    fun `a system call nested deeper inside systemCall passes`() {
        val code =
            """
            fun keys(listener: Listener) = systemCall { listener.activeNotifications.map { it.key }.toSet() }
            """.trimIndent()

        assertEquals(emptyList<String>(), messages(code))
    }

    @Test
    fun `a system call or property outside systemCall is reported`() {
        val code =
            """
            fun snooze(listener: Listener) {
                listener.snoozeNotification("key", 1L)
            }

            fun read(listener: Listener) = run { listener.activeNotifications }
            """.trimIndent()

        assertEquals(
            listOf(
                "snoozeNotification reaches a system service outside systemCall. Wrap it in systemCall { } so a " +
                    "refusal or a binder failure becomes a SystemCall value instead of crashing the app.",
                "activeNotifications reaches a system service outside systemCall. Wrap it in systemCall { } so a " +
                    "refusal or a binder failure becomes a SystemCall value instead of crashing the app.",
            ),
            messages(code),
        )
    }

    @Test
    fun `names that aren't listed are ignored`() {
        assertEquals(emptyList<String>(), messages("fun cancel(job: Job) = job.cancel()"))
    }

    private fun messages(code: String): List<String> = rule.lint(code).map { it.message }
}
