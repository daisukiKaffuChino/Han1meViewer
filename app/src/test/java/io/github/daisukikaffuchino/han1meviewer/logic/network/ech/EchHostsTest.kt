package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EchHostsTest {

    @Test
    fun coreDomainsIncludeExactHostAndSubdomains() {
        assertTrue(EchHosts.isCoreDomain("hanime1.me"))
        assertTrue(EchHosts.isCoreDomain("www.hanime1.me"))
        assertTrue(EchHosts.isCoreDomain("JAVCHU.COM"))
        assertFalse(EchHosts.isCoreDomain("cdn.example.com"))
    }

    @Test
    fun echIsAttemptedForEveryHost() {
        assertTrue(EchHosts.shouldTryEch("hanime1.me"))
        assertTrue(EchHosts.shouldTryEch("cdn.example.com"))
    }
}
