package io.github.daisukikaffuchino.han1meviewer.logic.state

/**
 * 统一的“页面级”状态模型（替代历史上的 `PageState` / `PageLoadingState` 重复实现）。
 *
 * 本次先替换 `PageState`（Home / GetchuPreview），`PageLoadingState` 的使用点
 * 会在后续子步骤中逐步迁移到本类型。
 */
sealed interface PagedUiState<out T> {

    /** 首次加载中（尚无数据）。 */
    data object Loading : PagedUiState<Nothing>

    /** 加载完成但没有内容。 */
    data object Empty : PagedUiState<Nothing>

    /** 已到列表末尾；[data] 为已加载到的数据（可空）。 */
    data class NoMoreData<out T>(val data: T? = null) : PagedUiState<T>

    /** 加载成功；[isRefreshing] 表示正在下拉刷新（此时仍展示旧数据）。 */
    data class Success<out T>(
        val data: T,
        val isRefreshing: Boolean = false,
    ) : PagedUiState<T>

    /**
     * 加载失败；[cached] 为上一次成功的数据（用于“有旧数据时继续展示”）。
     * [error] 提供结构化视图，供 UI 按 kind 分支。
     */
    data class Error<out T>(
        val throwable: Throwable,
        val cached: T? = null,
    ) : PagedUiState<T> {
        val error: HanimeError get() = throwable.toHanimeError()
    }
}

/** 当前可展示的数据（Success / NoMoreData / Error 的缓存）。 */
val <T> PagedUiState<T>.dataOrNull: T?
    get() = when (this) {
        is PagedUiState.Success -> data
        is PagedUiState.NoMoreData -> data
        is PagedUiState.Error -> cached
        else -> null
    }

val <T> PagedUiState<T>.isFirstPageLoading: Boolean
    get() = this is PagedUiState.Loading

val <T> PagedUiState<T>.isFirstPageError: Boolean
    get() = this is PagedUiState.Error && cached == null

val <T> PagedUiState<T>.isFirstPageEmpty: Boolean
    get() = this is PagedUiState.Empty
