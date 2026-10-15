package dev.danielgilchrist.knockoff.lint

import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnusedImportTest {
    private val rule = UnusedImport(TestConfig())

    @Test
    fun `an import nothing refers to is reported`() {
        val code =
            """
            import java.time.Year
            import java.time.Instant

            fun now(): Instant = Instant.now()
            """.trimIndent()

        assertEquals(listOf("java.time.Year is imported but never used."), rule.lint(code).map { it.message })
    }

    @Test
    fun `types, calls and references all count as uses`() {
        val code =
            """
            import java.time.Instant
            import java.time.Duration
            import kotlin.math.max

            fun later(): Instant = Instant.now().plus(Duration.ofSeconds(max(1L, 2L)))

            val clock = Instant::now
            """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `operator and delegate imports are used implicitly`() {
        val code =
            """
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.setValue
            import kotlinx.coroutines.flow.component1

            fun nothing() = Unit
            """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `star imports are left alone`() {
        val code =
            """
            import java.time.*

            fun nothing() = Unit
            """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }
}
