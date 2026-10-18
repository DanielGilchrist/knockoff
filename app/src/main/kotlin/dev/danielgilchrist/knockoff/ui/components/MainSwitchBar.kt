package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.rule.RuleState

@Composable
internal fun MainSwitchBar(
    text: String,
    state: RuleState,
    onStateChange: (RuleState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val checked =
        when (state) {
            RuleState.Enabled -> true
            RuleState.Disabled -> false
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .toggleable(value = checked, onValueChange = {
                    onStateChange(if (it) RuleState.Enabled else RuleState.Disabled)
                }, role = Role.Switch),
        shape = MaterialTheme.shapes.extraLarge,
        color =
            when {
                checked -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHighest
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        }
    }
}
