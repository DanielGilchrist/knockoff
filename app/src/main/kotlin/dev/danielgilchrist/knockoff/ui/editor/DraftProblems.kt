package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone
import dev.danielgilchrist.knockoff.ui.describe

@Composable
internal fun DraftProblems(
    problems: List<DraftProblem>,
    modifier: Modifier = Modifier,
) {
    if (problems.isNotEmpty()) {
        NoticeCard(
            title = "Fix before saving",
            body = problems.joinToString("\n") { "• ${it.describe()}" },
            tone = NoticeTone.Problem,
            modifier = modifier,
        )
    }
}
