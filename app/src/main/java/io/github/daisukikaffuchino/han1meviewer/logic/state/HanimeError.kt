package io.github.daisukikaffuchino.han1meviewer.logic.state

import io.github.daisukikaffuchino.han1meviewer.logic.exception.CloudflareBlockedException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.HanimeNotFoundException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.IPBlockedException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.InvalidCredentialsException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.LoginStateExpiredException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.NotLoggedInException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.ParseException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * 结构化错误类型（模型层）。
 *
 * 目的：让 UI / ViewModel **按 `kind` 分支**，而不是 `is SomeException` 地硬编码异常类。
 * 这样异常层级变化、文案变化都不会影响调用方。
 */
enum class HanimeErrorKind {
    /** IP 被封禁。 */
    IpBlocked,

    /** Cloudflare 挑战。 */
    CloudflareChallenge,

    /** 影片 / 页面不存在或已失效。 */
    NotFound,

    /** 登录态过期。 */
    SessionExpired,

    /** 未登录但访问了需要登录的资源。 */
    NotLoggedIn,

    /** 账号或密码错误。 */
    InvalidCredentials,

    /** HTML / JSON 解析失败。 */
    Parse,

    /** TLS 握手失败。 */
    Tls,

    /** DNS 解析失败。 */
    Dns,

    /** 请求超时。 */
    Timeout,

    /** 连接失败。 */
    Connect,

    /** 其它网络 IO 错误。 */
    Network,

    /** 连接被重置。 */
    ConnectionReset,

    /** 未分类。 */
    Unknown,
    ;

    /** 是否值得让用户点“重试”。 */
    val isRetryable: Boolean
        get() = when (this) {
            SessionExpired, NotLoggedIn, NotFound, InvalidCredentials -> false
            else -> true
        }
}

data class HanimeError(
    val kind: HanimeErrorKind,
    val message: String? = null,
) {
    val isRetryable: Boolean get() = kind.isRetryable
}

/** 把异常映射成结构化错误。 */
fun Throwable.toHanimeError(): HanimeError {
    // 注意顺序：IPBlockedException 是 CloudflareBlockedException 的子类；
    // SSLHandshakeException / SocketTimeoutException / UnknownHostException / ConnectException
    // 都是 IOException 的子类，必须排在 IOException 之前。
    val kind = when (this) {
        is IPBlockedException -> HanimeErrorKind.IpBlocked
        is CloudflareBlockedException -> HanimeErrorKind.CloudflareChallenge
        is HanimeNotFoundException -> HanimeErrorKind.NotFound
        is InvalidCredentialsException -> HanimeErrorKind.InvalidCredentials
        is LoginStateExpiredException -> HanimeErrorKind.SessionExpired
        is NotLoggedInException -> HanimeErrorKind.NotLoggedIn
        is ParseException -> HanimeErrorKind.Parse
        is SSLHandshakeException -> HanimeErrorKind.Tls
        is UnknownHostException -> HanimeErrorKind.Dns
        is SocketTimeoutException -> HanimeErrorKind.Timeout
        is ConnectException -> HanimeErrorKind.Connect
        is SocketException ->
            if (message?.contains("connection reset", ignoreCase = true) == true) {
                HanimeErrorKind.ConnectionReset
            } else {
                HanimeErrorKind.Network
            }

        is IOException -> HanimeErrorKind.Network
        else -> HanimeErrorKind.Unknown
    }
    return HanimeError(kind, message)
}
