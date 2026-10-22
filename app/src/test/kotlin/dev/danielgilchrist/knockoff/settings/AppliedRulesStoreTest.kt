package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.settings.AppliedRules
import dev.danielgilchrist.knockoff.domain.settings.Settings
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.ZoneId

class AppliedRulesStoreTest {
    @TempDir
    lateinit var directory: File

    private val applied = AppliedRules(Settings.DEFAULT.rules, ZoneId.of("Europe/London"))
    private val since = AppliedRules(Settings.DEFAULT.rules, ZoneId.of("Europe/Paris"))

    @Test
    fun `nothing applied yet loads as missing with no baseline`() =
        runTest {
            val load = store().load()

            assertEquals(AppliedRulesLoad.Missing, load)
            assertEquals(Baseline.Unknown, load.baseline)
        }

    @Test
    fun `a saved baseline loads back whole`() =
        runTest {
            val baseline = Baseline.Known(applied, listOf(since))

            assertEquals(AppliedRulesSave.Saved, store().save(baseline))

            val load = store().load()

            assertEquals(AppliedRulesLoad.Loaded(baseline), load)
            assertEquals(baseline, load.baseline)
        }

    @Test
    fun `a corrupt file fails to load with no baseline`() =
        runTest {
            File(directory, "applied-rules.json").writeText("not json")

            val load = store().load()

            assertInstanceOf(AppliedRulesLoad.Failed::class.java, load)
            assertEquals(Baseline.Unknown, load.baseline)
        }

    private fun store() = AppliedRulesStore(File(directory, "applied-rules.json"))
}
