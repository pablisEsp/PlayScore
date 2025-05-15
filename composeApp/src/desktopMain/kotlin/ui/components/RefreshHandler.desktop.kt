// In desktopMain
package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual interface RefreshHandler {
    actual val isRefreshing: Boolean
    actual fun refresh()
}

class DesktopRefreshHandler(
    override val isRefreshing: Boolean,
    private val onRefresh: () -> Unit
) : RefreshHandler {
    override fun refresh() {
        onRefresh()
    }
}

@Composable
actual fun rememberRefreshHandler(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
): RefreshHandler = DesktopRefreshHandler(isRefreshing, onRefresh)


@Composable
actual fun RefreshableContainer(
    modifier: Modifier,
    refreshHandler: RefreshHandler,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        // No pull-refresh indicator for desktop
    }
}