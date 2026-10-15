package dev.danielgilchrist.knockoff.lint

import dev.detekt.test.utils.compileContentForTest
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class SuppressionsTest {
    companion object {
        private val SUPPRESSING = setOf("Suppress", "SuppressWarnings", "SuppressLint")
    }

    private val root = File("..").canonicalFile

    @Test
    fun `no source suppresses a lint`() {
        val suppressions =
            listOf("domain", "app", "lint")
                .flatMap { module -> File(root, "$module/src").walk().filter { it.extension == "kt" }.toList() }
                .flatMap { file -> suppressionsIn(file.readText()).map { "${file.relativeTo(root)}: $it" } }

        assertEquals(emptyList<String>(), suppressions)
    }

    @Test
    fun `suppressions are found however they are written`() {
        val code =
            """
            @file:kotlin.Suppress("all")

            @Suppress("UnapprovedThrow")
            fun first(): Int = 1

            @SuppressWarnings("unused")
            fun second(): Int = 2

            @SuppressLint("QueryPermissionsNeeded")
            fun third(): Int = 3

            @JvmName("fourth")
            fun fourth(): String = "@Suppress"
            """.trimIndent()

        assertEquals(
            listOf(
                "@file:kotlin.Suppress(\"all\")",
                "@Suppress(\"UnapprovedThrow\")",
                "@SuppressWarnings(\"unused\")",
                "@SuppressLint(\"QueryPermissionsNeeded\")",
            ),
            suppressionsIn(code),
        )
    }

    private fun suppressionsIn(code: String): List<String> =
        compileContentForTest(code)
            .collectDescendantsOfType<KtAnnotationEntry>()
            .filter { it.shortName?.asString() in SUPPRESSING }
            .map { it.text }
}
