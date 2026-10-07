package io.github.daisukikaffuchino.han1meviewer.logic.network

import kotlin.test.Test
import kotlin.test.assertEquals

/** 网络错误映射护栏。 */
class NetworkFailureClassifierTest {

    private fun classify(statusCode: Int, body: String?, isLoggedIn: Boolean = true) =
        NetworkFailureClassifier.classify(statusCode, body, isLoggedIn)

    @Test
    fun `403 with ip-block marker maps to IpBlocked`() {
        assertEquals(
            NetworkFailure.IpBlocked,
            classify(403, "<html>Sorry, you have been blocked</html>"),
        )
    }

    @Test
    fun `403 with cloudflare challenge marker maps to CloudflareChallenge`() {
        assertEquals(
            NetworkFailure.CloudflareChallenge,
            classify(403, "<html><title>Just a moment...</title></html>"),
        )
    }

    @Test
    fun `403 without known marker maps to NotFound`() {
        assertEquals(NetworkFailure.NotFound, classify(403, "<html>forbidden</html>"))
    }

    @Test
    fun `403 without body is unknown`() {
        assertEquals(NetworkFailure.Unknown, classify(403, null))
        assertEquals(NetworkFailure.Unknown, classify(403, ""))
        assertEquals(NetworkFailure.Unknown, classify(403, "   "))
    }

    @Test
    fun `500 maps to NotFound`() {
        assertEquals(NetworkFailure.NotFound, classify(500, "server error"))
    }

    @Test
    fun `404 depends on login state`() {
        assertEquals(NetworkFailure.NotLoggedIn, classify(404, "not found", isLoggedIn = false))
        assertEquals(NetworkFailure.Unknown, classify(404, "not found", isLoggedIn = true))
    }

    @Test
    fun `other codes are unknown`() {
        assertEquals(NetworkFailure.Unknown, classify(200, "ok"))
        assertEquals(NetworkFailure.Unknown, classify(302, "redirect"))
        assertEquals(NetworkFailure.Unknown, classify(502, "bad gateway"))
    }
}
