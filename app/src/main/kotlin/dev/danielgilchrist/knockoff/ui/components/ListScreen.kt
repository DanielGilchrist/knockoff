package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListScreen(
    title: String,
    navigation: ScreenNavigation,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    fab: ScreenFab = ScreenFab.None,
    bar: ScreenBar = ScreenBar.Large,
    content: LazyListScope.() -> Unit,
) {
    val scroll =
        when (bar) {
            ScreenBar.Large -> TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
            ScreenBar.Small -> TopAppBarDefaults.pinnedScrollBehavior()
        }
    val direction = LocalLayoutDirection.current
    val bottomSpace =
        when (fab) {
            ScreenFab.None -> 24.dp
            is ScreenFab.Extended -> 96.dp
        }

    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            val heading: @Composable () -> Unit = {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val back: @Composable () -> Unit = { BackIcon(navigation) }

            when (bar) {
                ScreenBar.Large -> {
                    LargeTopAppBar(
                        title = heading,
                        navigationIcon = back,
                        actions = actions,
                        scrollBehavior = scroll,
                    )
                }

                ScreenBar.Small -> {
                    TopAppBar(title = heading, navigationIcon = back, actions = actions, scrollBehavior = scroll)
                }
            }
        },
        floatingActionButton = {
            when (fab) {
                ScreenFab.None -> {}

                is ScreenFab.Extended -> {
                    ExtendedFloatingActionButton(
                        text = { Text(text = fab.text) },
                        icon = { Icon(painter = painterResource(fab.icon), contentDescription = null) },
                        onClick = fab.onClick,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.imePadding(),
            contentPadding =
                PaddingValues(
                    start = padding.calculateStartPadding(direction) + 16.dp,
                    end = padding.calculateEndPadding(direction) + 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + bottomSpace,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun BackIcon(navigation: ScreenNavigation) {
    when (navigation) {
        ScreenNavigation.Root -> {}

        is ScreenNavigation.Back -> {
            IconButton(onClick = navigation.onBack) {
                Icon(painter = painterResource(R.drawable.ic_back), contentDescription = "Back")
            }
        }
    }
}
