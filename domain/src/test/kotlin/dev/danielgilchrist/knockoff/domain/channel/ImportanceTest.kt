package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class ImportanceTest {
    @Test
    fun `default and louder importances alert`() {
        listOf(Importance.Default, Importance.High, Importance.Max).map { it.loudness() } shouldBe
            listOf(
                Loudness.Alerting(AlertingImportance.Default),
                Loudness.Alerting(AlertingImportance.High),
                Loudness.Alerting(AlertingImportance.Max),
            )
    }

    @Test
    fun `quieter importances are quiet`() {
        listOf(Importance.None, Importance.Min, Importance.Low).map { it.loudness() } shouldBe
            listOf(Loudness.Quiet, Loudness.Quiet, Loudness.Quiet)
    }

    @Test
    fun `alerting importances name their importance`() {
        AlertingImportance.entries.map { it.importance } shouldBe
            listOf(Importance.Default, Importance.High, Importance.Max)
    }
}
