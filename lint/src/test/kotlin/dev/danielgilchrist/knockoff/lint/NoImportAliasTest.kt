package dev.danielgilchrist.knockoff.lint

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoImportAliasTest {
    private val rule = NoImportAlias(TestConfig())

    @Test
    fun `an aliased import is reported`() {
        val code =
            """
            import kotlin.error as bug

            fun fail(): Nothing = bug("x")
            """.trimIndent()

        assertEquals(
            listOf("Importing kotlin.error as bug hides it from name-based rules."),
            rule.lint(code).map { it.message },
        )
    }

    @Test
    fun `a plain import passes`() {
        val code =
            """
            import java.io.IOException

            fun failure(): IOException = IOException()
            """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }
}
