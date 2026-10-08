package io.github.daisukikaffuchino.han1meviewer.util

import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.exception.CloudflareBlockedException
import io.github.daisukikaffuchino.han1meviewer.logic.exception.IPBlockedException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 错误文案映射护栏。
 *
 * `toNetworkErrorMessageRes()` 现在按结构化错误类型（HanimeErrorKind）判断，
 * 只在拿不到类型时回退到关键字匹配。
 */
class NetworkErrorMessageTest {

    @Test
    fun `typed network failures map to their message`() {
        assertEquals(
            R.string.home_error_dns,
            UnknownHostException("x").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.home_error_timeout,
            SocketTimeoutException("x").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.home_error_ssl,
            SSLHandshakeException("x").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.home_error_connect,
            ConnectException("x").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.home_error_connection_interrupted,
            SocketException("Connection reset").toNetworkErrorMessageRes(),
        )
    }

    @Test
    fun `blocked responses map to cloudflare messages`() {
        assertEquals(
            R.string.cloudflare_ip_block_warning,
            IPBlockedException("x").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.cloudflare_network_mismatch,
            CloudflareBlockedException("x").toNetworkErrorMessageRes(),
        )
    }

    @Test
    fun `unknown failures fall back to keyword matching`() {
        assertEquals(
            R.string.home_error_connection_reset,
            IOException("connection reset by peer").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.home_error_forbidden,
            IOException("HTTP 403").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.home_error_not_found,
            IOException("HTTP 404").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.home_error_server_unavailable,
            IOException("HTTP 502 Bad Gateway").toNetworkErrorMessageRes(),
        )
        assertEquals(
            R.string.home_error_generic,
            RuntimeException("something odd").toNetworkErrorMessageRes(),
        )
    }
}
