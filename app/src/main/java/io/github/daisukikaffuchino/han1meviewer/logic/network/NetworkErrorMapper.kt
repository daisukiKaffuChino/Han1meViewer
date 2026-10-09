package io.github.daisukikaffuchino.han1meviewer.logic.network

import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository.isAlreadyLogin
import io.github.daisukikaffuchino.han1meviewer.logic.exception.CloudflareBlockedException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.HanimeNotFoundException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.IPBlockedException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.NotLoggedInException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.ParseException
import io.github.daisukikaffuchino.utils.LogUtil
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody
import retrofit2.Response
import javax.net.ssl.SSLHandshakeException

/**
 * 把 HTTP 失败统一映射为**类型化的、不带本地化文案**的异常。
 *
 * 只负责「失败 -> 类型」的判定，不负责文案；文案由 UI 层按 `HanimeErrorKind`
 * 解析（见 `Throwable.toUiMessage`），因此这里不依赖 Android 资源。
 */
internal object NetworkErrorMapper {

    /**
     * 请求未成功（且不在允许的成功码内）时抛出对应的异常。
     */
    fun Response<ResponseBody>.throwRequestException(): Nothing {
        val body = errorBody()?.string()
        when (NetworkFailureClassifier.classify(code(), body, isAlreadyLogin)) {
            // 主要出現在影片界面，當你 v 數不大時會報 403
            NetworkFailure.IpBlocked ->
                throw IPBlockedException("IP blocked")

            NetworkFailure.CloudflareChallenge ->
                throw CloudflareBlockedException("Cloudflare challenge")

            // 主要出現在影片界面，當你 v 數不大 / 很大時會報 403 / 500
            NetworkFailure.NotFound ->
                throw HanimeNotFoundException("Video not found")

            NetworkFailure.NotLoggedIn ->
                throw NotLoggedInException()

            NetworkFailure.Unknown ->
                throw IllegalStateException("${code()} ${message()}")
        }
    }

    /**
     * 归一化异常：取消照常抛出；解析失败 / TLS 握手失败只记日志并原样返回，
     * 保留原始 cause 与堆栈，文案交给 UI 按 `kind` 解析。
     */
    fun handleException(e: Throwable): Throwable {
        return when (e) {
            is CancellationException -> throw e
            is ParseException -> {
                LogUtil.e("NetworkErrorMapper", "解析失败", e)
                e
            }

            is SSLHandshakeException -> {
                LogUtil.e("NetworkErrorMapper", "TLS 握手失败", e)
                e
            }

            else -> {
                LogUtil.e("NetworkErrorMapper", "请求失败", e)
                e
            }
        }
    }
}
