package io.github.daisukikaffuchino.han1meviewer.logic.network

import io.github.daisukikaffuchino.han1meviewer.EMPTY_STRING
import io.github.daisukikaffuchino.han1meviewer.logic.network.NetworkErrorMapper.handleException
import io.github.daisukikaffuchino.han1meviewer.logic.network.NetworkErrorMapper.throwRequestException
import io.github.daisukikaffuchino.han1meviewer.logic.state.PagedUiState
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.replaceBackupMediaCdnHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * 单网页请求：成功（或落到 [permittedSuccessCode]）时把响应体交给 [action] 解析成 [UiState]。
 *
 * @param permittedSuccessCode 用于处理特殊情况，比如播放清单修改成功返回 302。
 */
internal fun <T> websiteIOFlow(
    request: suspend () -> Response<ResponseBody>,
    permittedSuccessCode: IntArray? = null,
    action: (String) -> UiState<T>,
): Flow<UiState<T>> = flow {
    val requestResult = request.invoke()
    val resultBody = requestResult.body()?.string()?.replaceBackupMediaCdnHost()
    val permitted = permittedSuccessCode?.contains(requestResult.code()) == true
    if (permitted || requestResult.isSuccessful) {
        emit(action.invoke(resultBody ?: EMPTY_STRING))
    } else {
        requestResult.throwRequestException()
    }
}.catch { e ->
    emit(UiState.Error(handleException(e)))
}.flowOn(Dispatchers.IO)

/**
 * 用于有 page 分页的情况。
 */
internal fun <T> pageIOFlow(
    request: suspend () -> Response<ResponseBody>,
    action: (String) -> PagedUiState<T>,
): Flow<PagedUiState<T>> = flow {
    val requestResult = request.invoke()
    val resultBody = requestResult.body()?.string()?.replaceBackupMediaCdnHost()
    if (requestResult.isSuccessful && resultBody != null) {
        emit(action.invoke(resultBody))
    } else {
        requestResult.throwRequestException()
    }
}.catch { e ->
    emit(PagedUiState.Error(handleException(e)))
}.flowOn(Dispatchers.IO)

/**
 * 用于影片界面（成功但响应体为空时也不报错）。
 */
internal fun <T> videoIOFlow(
    request: suspend () -> Response<ResponseBody>,
    action: (String) -> UiState<T>,
): Flow<UiState<T>> = flow {
    val requestResult = request.invoke()
    val resultBody = requestResult.body()?.string()?.replaceBackupMediaCdnHost()
    if (requestResult.isSuccessful && resultBody != null) {
        emit(action.invoke(resultBody))
    } else {
        requestResult.throwRequestException()
    }
}.catch { e ->
    emit(UiState.Error(handleException(e)))
}.flowOn(Dispatchers.IO)
