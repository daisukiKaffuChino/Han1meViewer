package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import okhttp3.Dns
import okhttp3.OkHttpClient

/**
 * Installs the Conscrypt TLS socket factory, protected-host DNS resolver, H3
 * interceptor, and ECH retry handling.
 */
fun OkHttpClient.Builder.echTransport(
    fallbackDns: Dns = Dns.SYSTEM,
): OkHttpClient.Builder = if (!SettingsRepository.useEch) {
    dns(fallbackDns)
} else this
    .apply { ConscryptEch.install() }
    .sslSocketFactory(ConscryptEch.socketFactory, ConscryptEch.trustManager)
    .dns(EchDns(fallbackDns))
    .addInterceptor(H3Interceptor())
    .addInterceptor(EchRetryInterceptor())

object EchHttp {

    fun onDohSettingsChanged() = EchDoh.invalidateAll()
}
