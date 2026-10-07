package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.logic.model.ProxyType

/**
 * ECH is opt-in. When the user selects a proxy route, the proxy must win over
 * direct-only ECH behavior so proxy-only networks remain usable.
 */
object EchTransportPolicy {

    fun isProxyRoute(): Boolean = isProxyRoute(SettingsRepository.proxyType)

    fun isProxyRoute(proxyType: Int): Boolean = proxyType != ProxyType.Direct.id

    fun shouldFailClosed(host: String): Boolean =
        shouldFailClosed(host, SettingsRepository.proxyType)

    fun shouldFailClosed(host: String, proxyType: Int): Boolean =
        EchHosts.isCoreDomain(host) && !isProxyRoute(proxyType)

    fun shouldUseH3(): Boolean = shouldUseH3(SettingsRepository.proxyType)

    fun shouldUseH3(proxyType: Int): Boolean = !isProxyRoute(proxyType)
}
