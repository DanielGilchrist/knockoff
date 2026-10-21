package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.ui.Editing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WizardHintsTest {
    private val whatsapp = COLLEAGUE_CHAT.packageName
    private val apps = Editing.Guided.START.copy(step = WizardStep.Apps)
    private val unchosen = ProblemSplit(listOf(DraftProblem.NothingChosen(whatsapp)), emptyList())

    @Test
    fun `the apps step asks for an app, then for a pick in any app that has none`() {
        assertEquals(listOf("Add at least one app."), hints(apps, ProblemSplit.NONE))
        val withApp = apps.copy(draft = apps.draft.copy(apps = mapOf(whatsapp to AppDraft.EVERYTHING)))

        assertEquals(emptyList<String>(), hints(withApp, ProblemSplit.NONE))
        assertEquals(listOf("Pick a chat for WhatsApp, or choose All notifications."), hints(withApp, unchosen))
    }

    @Test
    fun `other steps have no hints`() {
        listOf(WizardStep.Goal, WizardStep.Hours, WizardStep.Review).forEach { step ->
            assertEquals(emptyList<String>(), hints(apps.copy(step = step), unchosen))
        }
    }

    private fun hints(
        guided: Editing.Guided,
        shown: ProblemSplit,
    ): List<String> = wizardHints(guided, shown, { "WhatsApp" }, { "chat" })
}
