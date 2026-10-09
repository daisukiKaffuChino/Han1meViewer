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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 结构化错误映射护栏。
 *
 * 重点守护**匹配顺序**：`IPBlockedException` 是 `CloudflareBlockedException` 的子类；
 * `SSLHandshakeException` / `UnknownHostException` / `SocketTimeoutException` / `ConnectException`
 * 都是 `IOException` 的子类，一旦顺序写错就会被错误归类。
 */
class HanimeErrorTest {

    @Test
    fun `domain exceptions map to their kind`() {
        assertEquals(HanimeErrorKind.IpBlocked, IPBlockedException("x").toHanimeError().kind)
        assertEquals(
            HanimeErrorKind.CloudflareChallenge,
            CloudflareBlockedException("x").toHanimeError().kind,
        )
        assertEquals(HanimeErrorKind.NotFound, HanimeNotFoundException("x").toHanimeError().kind)
        assertEquals(
            HanimeErrorKind.SessionExpired,
            LoginStateExpiredException().toHanimeError().kind,
        )
        assertEquals(HanimeErrorKind.NotLoggedIn, NotLoggedInException().toHanimeError().kind)
        assertEquals(
            HanimeErrorKind.InvalidCredentials,
            InvalidCredentialsException("x").toHanimeError().kind,
        )
        assertEquals(HanimeErrorKind.Parse, ParseException("x").toHanimeError().kind)
    }

    @Test
    fun `io exceptions are matched before generic IOException`() {
        assertEquals(HanimeErrorKind.Tls, SSLHandshakeException("x").toHanimeError().kind)
        assertEquals(HanimeErrorKind.Dns, UnknownHostException("x").toHanimeError().kind)
        assertEquals(HanimeErrorKind.Timeout, SocketTimeoutException("x").toHanimeError().kind)
        assertEquals(HanimeErrorKind.Connect, ConnectException("x").toHanimeError().kind)
        assertEquals(HanimeErrorKind.Network, IOException("x").toHanimeError().kind)
    }

    @Test
    fun `socket exceptions distinguish connection reset from generic network errors`() {
        assertEquals(
            HanimeErrorKind.ConnectionReset,
            SocketException("Connection reset").toHanimeError().kind,
        )
        assertEquals(
            HanimeErrorKind.Network,
            SocketException("broken pipe").toHanimeError().kind,
        )
    }

    @Test
    fun `unrelated exceptions are unknown`() {
        assertEquals(HanimeErrorKind.Unknown, IllegalStateException("x").toHanimeError().kind)
        assertEquals(HanimeErrorKind.Unknown, RuntimeException().toHanimeError().kind)
    }

    @Test
    fun `message is preserved`() {
        val error = HanimeNotFoundException("影片不存在").toHanimeError()
        assertEquals("影片不存在", error.message)
    }

    @Test
    fun `retryability reflects whether user action can help`() {
        assertFalse(HanimeErrorKind.SessionExpired.isRetryable)
        assertFalse(HanimeErrorKind.NotLoggedIn.isRetryable)
        assertFalse(HanimeErrorKind.NotFound.isRetryable)
        assertFalse(HanimeErrorKind.InvalidCredentials.isRetryable)
        assertTrue(HanimeErrorKind.Timeout.isRetryable)
        assertTrue(HanimeErrorKind.ConnectionReset.isRetryable)
        assertTrue(HanimeErrorKind.Network.isRetryable)
        assertTrue(HanimeErrorKind.CloudflareChallenge.isRetryable)
    }
}
