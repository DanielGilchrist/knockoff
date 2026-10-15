package dev.danielgilchrist.knockoff.lint

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnapprovedCatchTest {
    private val rule = UnapprovedCatch(TestConfig("allowedExceptions" to listOf("IOException")))

    @Test
    fun `an approved exception type passes`() {
        assertEquals(emptyList<String>(), messages(catching("IOException")))
    }

    @Test
    fun `an approved exception type passes when fully qualified`() {
        assertEquals(emptyList<String>(), messages(catching("java.io.IOException")))
    }

    @Test
    fun `an unapproved exception type is reported`() {
        assertEquals(
            listOf(
                "Catching IllegalStateException isn't approved here. Return a sealed result from the boundary that " +
                    "can fail, or approve the type with a justification under allowedExceptions.",
            ),
            messages(catching("IllegalStateException")),
        )
    }

    @Test
    fun `nothing is approved by default`() {
        assertEquals(1, UnapprovedCatch(TestConfig()).lint(catching("IOException")).size)
    }

    @Test
    fun `a catch operator is reported`() {
        val code =
            """
            fun recover(values: Sequence<Int>) = values.catch { 0 }
            """.trimIndent()

        assertEquals(
            listOf(
                "A catch operator handles every Throwable except cancellation. Convert specific exceptions to a " +
                    "sealed result where the failure happens.",
            ),
            messages(code),
        )
    }

    private fun catching(type: String): String =
        """
        fun read(): String =
            try {
                "text"
            } catch (exception: $type) {
                "failed"
            }
        """.trimIndent()

    private fun messages(code: String): List<String> = rule.lint(code).map { it.message }
}
