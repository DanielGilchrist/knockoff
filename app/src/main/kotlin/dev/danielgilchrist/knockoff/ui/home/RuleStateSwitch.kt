package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.rule.RuleState

@Composable
internal fun RuleStateSwitch(
    state: RuleState,
    onStateChange: (RuleState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked =
            when (state) {
                RuleState.Enabled -> true
                RuleState.Disabled -> false
            },
        onCheckedChange = { checked -> onStateChange(if (checked) RuleState.Enabled else RuleState.Disabled) },
        modifier = modifier,
    )
}
