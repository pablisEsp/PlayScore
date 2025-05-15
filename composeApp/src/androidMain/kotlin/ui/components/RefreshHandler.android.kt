package ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

actual interface RefreshHandler {
    actual val isRefreshing: Boolean
    actual fun refresh()
}

class AndroidRefreshHandler(
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
): RefreshHandler = AndroidRefreshHandler(isRefreshing, onRefresh)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun RefreshableContainer(
    modifier: Modifier,
    refreshHandler: RefreshHandler,
    content: @Composable BoxScope.() -> Unit
) {
    // Call rememberPullToRefreshState without parameters
    val pullToRefreshState = rememberPullToRefreshState()


    PullToRefreshBox(
        isRefreshing = refreshHandler.isRefreshing,
        onRefresh = { refreshHandler.refresh() },
        state = pullToRefreshState,
        modifier = modifier
    ) {
        Box {
            content()
        }
    }
}