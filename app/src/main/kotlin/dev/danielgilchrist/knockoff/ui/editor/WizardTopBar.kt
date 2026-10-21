package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WizardTopBar(
    step: WizardStep,
    onClose: () -> Unit,
) {
    Column {
        TopAppBar(
            title = { Text(text = "New rule") },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Discard")
                }
            },
        )
        LinearProgressIndicator(
            progress = { step.position / WizardStep.entries.size.toFloat() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics {
                        stateDescription = "Step ${step.position} of ${WizardStep.entries.size}"
                    },
        )
    }
}
