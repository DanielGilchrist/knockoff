package dev.danielgilchrist.knockoff.lint

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoTypeFilteringTest {
    private val rule = NoTypeFiltering(TestConfig())

    @Test
    fun `a safe cast is reported`() {
        val code = "fun text(value: Any): String? = value as? String"

        assertEquals(
            listOf("`as?` silently drops other types. Use an exhaustive when."),
            rule.lint(code).map { it.message },
        )
    }

    @Test
    fun `filterIsInstance is reported`() {
        val code = "fun texts(values: List<Any>): List<String> = values.filterIsInstance<String>()"

        assertEquals(
            listOf("filterIsInstance silently drops other types. Use an exhaustive when."),
            rule.lint(code).map { it.message },
        )
    }

    @Test
    fun `filterIsInstanceTo and references are reported`() {
        val code =
            """
            fun texts(values: List<Any>): MutableList<String> = values.filterIsInstanceTo(mutableListOf<String>())

            val filter = List<Any>::filterIsInstance
            """.trimIndent()

        assertEquals(
            listOf(
                "filterIsInstanceTo silently drops other types. Use an exhaustive when.",
                "filterIsInstance silently drops other types. Use an exhaustive when.",
            ),
            rule.lint(code).map { it.message },
        )
    }

    @Test
    fun `a plain cast and other filters pass`() {
        val code =
            """
            fun text(value: Any): String = value as String

            fun long(values: List<String>): List<String> = values.filter { it.length > 3 }
            """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }
}
