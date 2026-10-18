package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R

@Composable
internal fun EditCard(
    title: String,
    removal: CardRemoval,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        val top =
            when (removal) {
                CardRemoval.None -> 16.dp
                is CardRemoval.Remove -> 4.dp
            }

        Column(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = top, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                when (removal) {
                    CardRemoval.None -> {}

                    is CardRemoval.Remove -> {
                        IconButton(onClick = removal.onRemove) {
                            Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = removal.label)
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}
