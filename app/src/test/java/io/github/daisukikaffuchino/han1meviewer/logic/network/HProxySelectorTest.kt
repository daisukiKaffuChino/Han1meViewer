package io.github.daisukikaffuchino.han1meviewer.logic.network

import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.model.AppSettings
import io.github.daisukikaffuchino.han1meviewer.logic.model.ProxyType
import io.github.daisukikaffuchino.han1meviewer.logic.model.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

class HProxySelectorTest {

    @Test
    fun explicitProxyAppliesToCoreDomain() = runBlocking {
        SettingsRepository.update {
            it.copy(
                proxyType = ProxyType.Http,
                proxyIp = "127.0.0.1",
                proxyPort = 8080,
            )
        }

        val proxies = HProxySelector().select(URI("https://hanime1.me/"))

        assertEquals(1, proxies.size)
        assertEquals(Proxy.Type.HTTP, proxies.single().type())
        assertEquals(
            InetSocketAddress("127.0.0.1", 8080),
            proxies.single().address(),
        )
    }

    @Test
    fun directRouteUsesNoProxyForCoreDomain() = runBlocking {
        SettingsRepository.update { it.copy(proxyType = ProxyType.Direct) }

        val proxies = HProxySelector().select(URI("https://hanime1.me/"))

        assertEquals(listOf(Proxy.NO_PROXY), proxies)
    }

    private class FakeSettingsStore(initial: AppSettings) : SettingsStore {
        override val settings = MutableStateFlow(initial)

        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun installSettingsRepository() {
            runCatching {
                SettingsRepository.install(FakeSettingsStore(AppSettings()))
            }
        }
    }
}
