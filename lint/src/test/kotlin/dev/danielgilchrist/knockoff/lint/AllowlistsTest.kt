package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.Rule
import dev.detekt.test.TestConfig
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class AllowlistsTest {
    private val root = File("..").canonicalFile

    @ParameterizedTest
    @ValueSource(strings = ["domain", "app"])
    fun `every approved exception is still caught`(module: String) {
        val caught =
            findings(module, UnapprovedCatch(TestConfig()))
                .map { it.substringAfter("Catching ").substringBefore(" isn't") }
                .toSet()

        assertEquals(emptyList<String>(), entries(module, "allowedExceptions").filterNot { it in caught })
    }

    @ParameterizedTest
    @ValueSource(strings = ["domain", "app"])
    fun `every exception approved only in one place is still caught there`(module: String) {
        val caught =
            findings(module, UnapprovedCatch(TestConfig()))
                .map { it.substringAfter("as \"").substringBefore("\" under") }
                .toSet()

        assertEquals(emptyList<String>(), optionalEntries(module, "allowedOnlyIn").filterNot { it in caught })
    }

    @ParameterizedTest
    @ValueSource(strings = ["domain", "app"])
    fun `every approved declaration still throws`(module: String) {
        val throwing =
            findings(module, UnapprovedThrow(TestConfig()))
                .map { it.substringAfter(" in ").substringBefore(" isn't") }
                .toSet()

        assertEquals(emptyList<String>(), entries(module, "allowedDeclarations").filterNot { it in throwing })
    }

    private fun findings(
        module: String,
        rule: Rule,
    ): List<String> =
        File(root, "$module/src/main")
            .walk()
            .filter { it.extension == "kt" }
            .flatMap { rule.lint(it.readText()) }
            .map { it.message }
            .toList()

    private fun optionalEntries(
        module: String,
        key: String,
    ): List<String> =
        when {
            File(root, "config/detekt/$module.yml").readLines().any { it.trim() == "$key:" } -> entries(module, key)
            else -> emptyList()
        }

    private fun entries(
        module: String,
        key: String,
    ): List<String> {
        val lines = File(root, "config/detekt/$module.yml").readLines().map { it.trim() }

        return when {
            "$key: []" in lines -> {
                emptyList()
            }

            "$key:" in lines -> {
                lines
                    .dropWhile { it != "$key:" }
                    .drop(1)
                    .takeWhile { it.startsWith("- ") || it.startsWith("#") }
                    .filter { it.startsWith("- ") }
                    .map { it.removePrefix("- ") }
            }

            else -> {
                fail("$module.yml must list $key as a block list or as []")
            }
        }
    }
}
