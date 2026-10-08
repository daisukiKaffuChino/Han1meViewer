package io.github.daisukikaffuchino.han1meviewer.logic.state

/** 统一的非分页页面状态。 */
sealed interface UiState<out T> {

    /** 加载中（尚无数据）。 */
    data object Loading : UiState<Nothing>

    /** 加载完成但没有内容。 */
    data object Empty : UiState<Nothing>

    /** 加载成功。 */
    data class Success<out T>(val data: T) : UiState<T>

    /** 加载失败；[error] 提供结构化视图，供调用方按 kind 分支。 */
    data class Error(val throwable: Throwable) : UiState<Nothing> {
        val error: HanimeError get() = throwable.toHanimeError()
    }
}
