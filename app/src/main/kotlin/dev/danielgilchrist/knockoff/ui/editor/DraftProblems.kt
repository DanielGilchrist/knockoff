package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.ui.describe

@Composable
internal fun DraftProblems(
    problems: List<DraftProblem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        problems.forEach { Text(text = it.describe(), color = MaterialTheme.colorScheme.error) }
    }
}
