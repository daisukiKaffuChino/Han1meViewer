package io.github.daisukikaffuchino.han1meviewer.logic.network

/**
 * HTTP 失败分类。
 * 只负责“这是什么故障”，不负责“提示什么文案”。
 */
enum class NetworkFailure {
    /** 403 且命中 IP 封禁特征。 */
    IpBlocked,

    /** 403 且命中 Cloudflare 挑战页特征。 */
    CloudflareChallenge,

    /** 影片不存在 / 已失效（403 无特征、500）。 */
    NotFound,

    /** 404 且当前未登录。 */
    NotLoggedIn,

    /** 其它未分类失败，交由调用方按原始 code/message 处理。 */
    Unknown,
}

object NetworkFailureClassifier {

    const val IP_BLOCKED_MARKER = "you have been blocked"
    const val CLOUDFLARE_CHALLENGE_MARKER = "Just a moment"

    /**
     * @param statusCode HTTP 状态码
     * @param body 错误响应体（可为 null/空白）
     * @param isLoggedIn 当前是否已登录（仅 404 分支需要）
     */
    fun classify(statusCode: Int, body: String?, isLoggedIn: Boolean): NetworkFailure =
        when (statusCode) {
            403 -> when {
                body.isNullOrBlank() -> NetworkFailure.Unknown
                IP_BLOCKED_MARKER in body -> NetworkFailure.IpBlocked
                CLOUDFLARE_CHALLENGE_MARKER in body -> NetworkFailure.CloudflareChallenge
                else -> NetworkFailure.NotFound
            }

            500 -> NetworkFailure.NotFound

            404 -> if (!isLoggedIn) NetworkFailure.NotLoggedIn else NetworkFailure.Unknown

            else -> NetworkFailure.Unknown
        }
}
