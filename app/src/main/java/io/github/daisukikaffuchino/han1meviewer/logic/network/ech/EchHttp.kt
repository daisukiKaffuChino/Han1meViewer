package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import okhttp3.Dns
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Installs the Conscrypt TLS socket factory, protected-host DNS resolver, H3
 * interceptor, and ECH retry handling.
 */
fun OkHttpClient.Builder.echTransport(
    fallbackDns: Dns = Dns.SYSTEM,
): OkHttpClient.Builder = this
    .apply { ConscryptEch.install() }
    .sslSocketFactory(ConscryptEch.socketFactory, ConscryptEch.trustManager)
    .dns(EchDns(fallbackDns))
    .addInterceptor(H3Interceptor())
    .addInterceptor(EchRetryInterceptor())

object EchHttp {

    val isReady: Boolean get() = ConscryptEch.ready

    fun onDohSettingsChanged() = EchDoh.invalidateAll()

    /**
     * Login responses attach credentials to a 302 response, so redirects must
     * stay visible to the caller.
     */
    val loginClient: OkHttpClient by lazy {
        ConscryptEch.install()
        OkHttpClient.Builder()
            .sslSocketFactory(ConscryptEch.socketFactory, ConscryptEch.trustManager)
            .dns(EchDns())
            .cookieJar(okhttp3.CookieJar.NO_COOKIES)
            .followRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(EchRetryInterceptor())
            .build()
    }
}
