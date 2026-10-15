package dev.danielgilchrist.knockoff.lint

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnapprovedThrowTest {
    private val rule =
        UnapprovedThrow(
            TestConfig(
                "allowedDeclarations" to
                    listOf("sample.Schedule.coverage(Boolean)", "sample.ext(receiver: String, Int)", "sample.Interval"),
            ),
        )

    @Test
    fun `throwing in an approved function passes, including inside its lambdas`() {
        val code =
            """
            package sample

            class Schedule {
                fun coverage(ok: Boolean): Int {
                    check(ok) { "bug" }
                    return listOf(1).map { if (it < 0) throw IllegalStateException() else it }.sum()
                }
            }
            """.trimIndent()

        assertEquals(emptyList<String>(), throwing(code))
    }

    @Test
    fun `an overload of an approved function is not approved`() {
        val code =
            """
            package sample

            class Schedule {
                fun coverage(ok: Int): Int = error("bug")
            }
            """.trimIndent()

        assertEquals(listOf("error() in sample.Schedule.coverage(Int)"), throwing(code))
    }

    @Test
    fun `extension functions are approved by receiver and parameters`() {
        val code =
            """
            package sample

            fun String.ext(times: Int): String = error("bug")

            fun Int.ext(times: Int): String = error("bug")
            """.trimIndent()

        assertEquals(listOf("error() in sample.ext(receiver: Int, Int)"), throwing(code))
    }

    @Test
    fun `vararg parameters and extension properties are part of the owner`() {
        val code =
            """
            package sample

            fun spread(vararg values: Int): Int = error("bug")

            val String.size: Int get() = error("bug")
            """.trimIndent()

        assertEquals(
            listOf("error() in sample.spread(vararg Int)", "error() in sample.size(receiver: String)"),
            throwing(code),
        )
    }

    @Test
    fun `classes are approved by name for their initialisers`() {
        val code =
            """
            package sample

            class Interval(val start: Int, val end: Int) {
                init {
                    require(start < end)
                }
            }
            """.trimIndent()

        assertEquals(emptyList<String>(), throwing(code))
    }

    @Test
    fun `every throwing form is reported outside approved declarations`() {
        val code =
            """
            package sample

            fun everything(value: Int?, values: List<String>) {
                require(true)
                requireNotNull(value)
                check(true)
                checkNotNull(value)
                error("bug")
                TODO()
                throw IllegalStateException()
                value!!.inc()
                values.map(::error)
            }
            """.trimIndent()

        assertEquals(
            listOf(
                "require()",
                "requireNotNull()",
                "check()",
                "checkNotNull()",
                "error()",
                "TODO()",
                "throw",
                "!!",
                "::error",
            ).map { "$it in sample.everything(Int?, List<String>)" },
            throwing(code),
        )
    }

    @Test
    fun `findings say how to approve the declaration`() {
        val code =
            """
            package sample

            val broken: Int = error("bug")
            """.trimIndent()

        assertEquals(
            listOf(
                "error() in sample.broken isn't approved. Model the failure as a type, or approve sample.broken with " +
                    "a justification under allowedDeclarations if it guards a genuine bug that can't be typed.",
            ),
            rule.lint(code).map { it.message },
        )
    }

    @Test
    fun `other calls pass`() {
        val code =
            """
            fun fine(values: List<Int>): Int = values.sum()
            """.trimIndent()

        assertEquals(emptyList<String>(), throwing(code))
    }

    private fun throwing(code: String): List<String> = rule.lint(code).map { it.message.substringBefore(" isn't") }
}
