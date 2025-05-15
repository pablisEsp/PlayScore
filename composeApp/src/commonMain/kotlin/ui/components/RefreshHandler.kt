package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

expect interface RefreshHandler {
    val isRefreshing: Boolean
    fun refresh()
}

@Composable
expect fun rememberRefreshHandler(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
): RefreshHandler

@Composable
expect fun RefreshableContainer(
    modifier: Modifier = Modifier,
    refreshHandler: RefreshHandler,
    content: @Composable BoxScope.() -> Unit
)