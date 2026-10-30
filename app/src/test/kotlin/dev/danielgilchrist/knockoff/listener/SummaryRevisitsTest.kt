package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.notification.NotificationCategory
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.NotificationSnapshot
import dev.danielgilchrist.knockoff.domain.notification.NotificationStyle
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.settings.Settings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryRevisitsTest {
    private val evaluation = Evaluation(Settings.DEFAULT.rules, ZoneOffset.UTC, Holidays.NONE)

    @Test
    fun `removals in a burst are revisited together once it goes quiet`() =
        runTest {
            val batches = mutableListOf<List<String>>()
            val revisits = SummaryRevisits(backgroundScope) { batch -> batches += batch.map { it.gone.key } }

            (1..64).forEach { revisits.add(removed("chat$it")) }
            advanceTimeBy(499)
            runCurrent()
            assertEquals(emptyList<List<String>>(), batches)

            advanceTimeBy(2)
            runCurrent()
            assertEquals(listOf((1..64).map { "chat$it" }), batches)
        }

    @Test
    fun `a removal after the burst starts its own batch`() =
        runTest {
            val batches = mutableListOf<List<String>>()
            val revisits = SummaryRevisits(backgroundScope) { batch -> batches += batch.map { it.gone.key } }

            revisits.add(removed("first"))
            advanceTimeBy(501)
            runCurrent()
            revisits.add(removed("second"))
            advanceTimeBy(501)
            runCurrent()

            assertEquals(listOf(listOf("first"), listOf("second")), batches)
        }

    private fun removed(key: String): PendingRevisit =
        PendingRevisit(
            GroupEntry(
                key,
                "com.whatsapp",
                "chats",
                GroupPosition.Member,
                NotificationSnapshot(
                    NotificationKey(key),
                    PackageName("com.whatsapp"),
                    channelId = null,
                    group = OriginGroup.Unknown,
                    subText = null,
                    category = NotificationCategory.Other,
                    style = NotificationStyle.Other,
                    shortcutId = null,
                    titles = emptyList(),
                    traits = emptySet(),
                ).parse(),
            ),
            SummaryRevisit.AutogroupSummaries,
            evaluation,
        )
}
