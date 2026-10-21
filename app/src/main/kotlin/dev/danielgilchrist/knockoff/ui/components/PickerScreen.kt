package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.danielgilchrist.knockoff.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PickerScreen(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    searchHint: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    done: String = "Done",
    close: PickerClose = PickerClose.None,
    content: LazyListScope.() -> Unit,
) {
    Dialog(
        onDismissRequest =
            when (close) {
                PickerClose.None -> onDone
                is PickerClose.Discard -> close.onClose
            },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        when (close) {
                            PickerClose.None -> {}

                            is PickerClose.Discard -> {
                                IconButton(onClick = close.onClose) {
                                    Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Close")
                                }
                            }
                        }
                    },
                    actions = { TextButton(onClick = onDone) { Text(text = done) } },
                )
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(text = searchHint) },
                    leadingIcon = { Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    content = content,
                )
            }
        }
    }
}
