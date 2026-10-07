package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import io.github.daisukikaffuchino.han1meviewer.logic.model.ProxyType
import io.github.daisukikaffuchino.han1meviewer.logic.model.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EchTransportPolicyTest {

    @Test
    fun echDefaultsToOff() {
        assertFalse(AppSettings().useEch)
    }

    @Test
    fun directRouteAllowsFailClosedAndH3() {
        val direct = ProxyType.Direct.id

        assertFalse(EchTransportPolicy.isProxyRoute(direct))
        assertTrue(EchTransportPolicy.shouldFailClosed("hanime1.me", direct))
        assertTrue(EchTransportPolicy.shouldUseH3(direct))
    }

    @Test
    fun everyProxyModeDisablesFailClosedAndH3() {
        val proxyTypes = listOf(
            ProxyType.System.id,
            ProxyType.Http.id,
            ProxyType.Socks.id,
        )

        proxyTypes.forEach { proxyType ->
            assertTrue(EchTransportPolicy.isProxyRoute(proxyType))
            assertFalse(EchTransportPolicy.shouldFailClosed("hanime1.me", proxyType))
            assertFalse(EchTransportPolicy.shouldUseH3(proxyType))
        }
    }

    @Test
    fun nonCoreHostsNeverFailClosed() {
        assertFalse(EchTransportPolicy.shouldFailClosed("cdn.example.com", ProxyType.Direct.id))
    }
}
