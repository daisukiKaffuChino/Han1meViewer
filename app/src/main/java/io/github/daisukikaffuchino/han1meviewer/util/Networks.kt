package io.github.daisukikaffuchino.han1meviewer.util

import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.state.HanimeErrorKind
import io.github.daisukikaffuchino.han1meviewer.logic.state.toHanimeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspend extension that allows to suspend [Call] inside coroutine.
 */
suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
        continuation.invokeOnCancellation { cancel() }
    }
}

/**
 * Run suspend catching
 *
 * @param block suspend block
 */
inline fun <R> runSuspendCatching(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (c: CancellationException) {
        throw c
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

/**
 * 将首页加载异常映射为对应的错误提示字符串资源。
 *
 * 优先按结构化错误类型判断常见网络问题，必要时回退到异常信息中的关键字匹配。
 *
 * @receiver 首页加载过程中抛出的异常
 * @return 错误提示的字符串资源 ID
 */
fun Throwable.toNetworkErrorMessageRes(): Int {
    val rawMessage = message.orEmpty().lowercase()
    val kind = toHanimeError().kind
    return when {
        kind == HanimeErrorKind.Dns ||
                rawMessage.contains("unable to resolve host") ||
                rawMessage.contains("no address associated with hostname") -> {
            R.string.home_error_dns
        }

        kind == HanimeErrorKind.Timeout || rawMessage.contains("timeout") -> {
            R.string.home_error_timeout
        }

        kind == HanimeErrorKind.Tls ||
                rawMessage.contains("ssl") ||
                rawMessage.contains("certificate") -> {
            R.string.home_error_ssl
        }

        kind == HanimeErrorKind.Connect || rawMessage.contains("failed to connect") -> {
            R.string.home_error_connect
        }

        kind == HanimeErrorKind.ConnectionReset -> {
            R.string.home_error_connection_interrupted
        }

        kind == HanimeErrorKind.IpBlocked -> {
            R.string.cloudflare_ip_block_warning
        }

        kind == HanimeErrorKind.CloudflareChallenge -> {
            R.string.cloudflare_network_mismatch
        }

        rawMessage.contains("connection reset") -> {
            R.string.home_error_connection_reset
        }

        rawMessage.contains("403") -> {
            R.string.home_error_forbidden
        }

        rawMessage.contains("404") -> {
            R.string.home_error_not_found
        }

        rawMessage.contains("500") || rawMessage.contains("502") ||
                rawMessage.contains("503") || rawMessage.contains("504") -> {
            R.string.home_error_server_unavailable
        }

        else -> {
            R.string.home_error_generic
        }
    }
}
