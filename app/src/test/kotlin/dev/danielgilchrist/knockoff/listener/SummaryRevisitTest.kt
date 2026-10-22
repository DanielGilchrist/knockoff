package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SummaryRevisitTest {
    @Test
    fun `a snoozed notification revisits only autogroup summaries`() {
        assertEquals(
            SummaryRevisit.AutogroupSummaries,
            SummaryRevisit.afterRemoval(NotificationListenerService.REASON_SNOOZED),
        )
        assertEquals(
            listOf(SummaryRevisit.AllSummaries, SummaryRevisit.AllSummaries),
            listOf(NotificationListenerService.REASON_CANCEL, NotificationListenerService.REASON_APP_CANCEL)
                .map(SummaryRevisit::afterRemoval),
        )
    }

    @Test
    fun `each revisit covers its summaries and never members`() {
        assertEquals(
            listOf(false, true, true),
            GroupPosition.entries.map(SummaryRevisit.AllSummaries::covers),
        )
        assertEquals(
            listOf(false, false, true),
            GroupPosition.entries.map(SummaryRevisit.AutogroupSummaries::covers),
        )
    }
}
