package dev.danielgilchrist.knockoff.lint

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UntypedResultTest {
    private val rule = UntypedResult(TestConfig())
    private val result = "Result carries an untyped Throwable. Return a sealed result whose variants name each failure."
    private val runCatching =
        "runCatching catches every Throwable, including CancellationException. Catch the specific exception at the " +
            "boundary and return a sealed result."

    @Test
    fun `runCatching calls and references are reported`() {
        val code =
            """
            fun parse(text: String) = runCatching { text.toInt() }

            fun reference() = ::runCatching
            """.trimIndent()

        assertEquals(listOf(runCatching, runCatching), messages(code))
    }

    @Test
    fun `Result types are reported however they are written`() {
        val code =
            """
            fun plain(): Result<Int> = TODO()

            fun qualified(): kotlin.Result<Int> = TODO()

            fun nested(): List<Result<Int>> = TODO()
            """.trimIndent()

        assertEquals(listOf(result, result, result), messages(code))
    }

    @Test
    fun `an inferred Result is reported`() {
        val code =
            """
            fun inferred() = Result.success(1)
            """.trimIndent()

        assertEquals(listOf(result), messages(code))
    }

    @Test
    fun `sealed results pass`() {
        val code =
            """
            sealed interface ParseResult

            fun parse(text: String): ParseResult = TODO()
            """.trimIndent()

        assertEquals(emptyList<String>(), messages(code))
    }

    private fun messages(code: String): List<String> = rule.lint(code).map { it.message }
}
