package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProblemSplitTest {
    private val whatsapp = COLLEAGUE_CHAT.packageName
    private val narrowed = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed)

    @Test
    fun `nothing chosen is split from every other problem`() {
        val others =
            listOf(
                DraftProblem.BlankName,
                DraftProblem.DuplicateName,
                DraftProblem.NoHours,
                DraftProblem.BreaksCoverAllHours,
                DraftProblem.HoursWithoutDays(1),
                DraftProblem.BlankBreakName(1),
                DraftProblem.BreakWithoutDays(1),
                DraftProblem.BlankPattern(whatsapp, 1),
                DraftProblem.InvalidPattern(whatsapp, 2, "bad"),
                DraftProblem.NotSaved("disk"),
            )
        val unchosen = DraftProblem.NothingChosen(whatsapp)

        assertEquals(ProblemSplit(listOf(unchosen), others), ProblemSplit.of(listOf(unchosen) + others))
        assertEquals(ProblemSplit.NONE, ProblemSplit.of(emptyList()))
    }

    @Test
    fun `an app's draft splits by how it parses`() {
        assertEquals(ProblemSplit.NONE, ProblemSplit.of(AppDraft.EVERYTHING.parse(whatsapp)))
        assertEquals(ProblemSplit.NONE, ProblemSplit.of(narrowed.copy(asking = NewChatsAsking.Ask).parse(whatsapp)))
        assertEquals(
            ProblemSplit(listOf(DraftProblem.NothingChosen(whatsapp)), emptyList()),
            ProblemSplit.of(narrowed.parse(whatsapp)),
        )
        assertEquals(
            ProblemSplit(emptyList(), listOf(DraftProblem.BlankPattern(whatsapp, 1))),
            ProblemSplit.of(narrowed.copy(patterns = listOf(PatternDraft(PatternField.ChatName, " "))).parse(whatsapp)),
        )
    }
}
