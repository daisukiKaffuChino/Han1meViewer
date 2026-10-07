package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.videogrid

import androidx.compose.foundation.lazy.grid.LazyGridState
import io.github.daisukikaffuchino.han1meviewer.logic.model.HanimeInfo
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState

/**
 * 判断视频网格是否需要加载更多。
 *
 * @param items 当前视频列表
 * @param state 加载状态
 * @return 是否需要触发加载更多
 */
fun LazyGridState.canLoadMore(
    items: List<HanimeInfo>,
    state: PagedUiState<*>,
): Boolean {
    if (items.isEmpty()) return false
    if (state is PagedUiState.Loading || state is PagedUiState.NoMoreData || state is PagedUiState.Error) {
        return false
    }
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return false
    return lastVisible >= layoutInfo.totalItemsCount - 4
}
