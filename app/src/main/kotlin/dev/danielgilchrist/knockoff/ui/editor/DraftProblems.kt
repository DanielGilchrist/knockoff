package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.ui.TargetChoices
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone
import dev.danielgilchrist.knockoff.ui.describe

@Composable
internal fun DraftProblems(
    problems: List<DraftProblem>,
    draft: RuleDraft,
    stored: RuleApps,
    choices: TargetChoices,
    modifier: Modifier = Modifier,
    title: String = "Fix before saving",
) {
    if (problems.isNotEmpty()) {
        NoticeCard(
            title = title,
            body =
                problemsBody(
                    problems.map { it.describe(choices.apps::labelOf) { app -> pickOf(app, draft, stored, choices) } },
                ),
            tone = NoticeTone.Problem,
            modifier = modifier,
        )
    }
}

internal fun pickOf(
    app: PackageName,
    draft: RuleDraft,
    stored: RuleApps,
    choices: TargetChoices,
): String {
    val appDraft = draft.apps.getOrDefault(app, AppDraft.EVERYTHING)
    val (kind, accounts) = AppKind.forApp(app, appDraft, stored, choices)

    return kind.pick(accounts.noun)
}

internal fun problemsBody(described: List<String>): String =
    when (described.size) {
        1 -> described.single()
        else -> described.joinToString("\n") { "• $it" }
    }
