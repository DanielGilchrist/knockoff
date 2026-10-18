package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.danielgilchrist.knockoff.R

@Composable
internal fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onBack,
        ) { Icon(painter = painterResource(R.drawable.ic_back), contentDescription = "Back") }
        Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        trailing()
    }
}
