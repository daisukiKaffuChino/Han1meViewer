package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import android.annotation.SuppressLint
import io.github.daisukikaffuchino.utils.LogUtil
import okhttp3.Interceptor
import okhttp3.Response
import org.conscrypt.Conscrypt
import org.conscrypt.DomainEncryptionMode
import org.conscrypt.NetworkSecurityPolicy
import org.conscrypt.metrics.CertificateTransparencyVerificationReason
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * In-process ECH transport backed by Conscrypt.
 *
 * Conscrypt locates the network policy through reflection on
 * [PolicyTrustManager.getNetworkSecurityPolicy]. Keep that method public and
 * keep the class from being renamed by R8, otherwise ECH is silently disabled.
 */
object ConscryptEch {

    private const val TAG = "HY-ECH"

    @Volatile
    var ready = false
        private set

    private val provider: java.security.Provider by lazy { Conscrypt.newProvider() }

    private val systemTrustManager: X509TrustManager by lazy {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
            ?: throw IllegalStateException("System X509TrustManager is unavailable")
    }

    val trustManager: X509TrustManager by lazy { PolicyTrustManager(systemTrustManager) }

    private val sslContext: SSLContext by lazy {
        SSLContext.getInstance("TLSv1.3", provider).apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }
    }

    val socketFactory: SSLSocketFactory by lazy { EchSocketFactory(sslContext.socketFactory) }

    fun install(): Boolean {
        if (ready) return true
        return runCatching {
            provider
            sslContext
            socketFactory
            ready = true
            LogUtil.i(TAG, "Conscrypt ECH ready, version=${Conscrypt.version()}")
            true
        }.getOrElse { throwable ->
            LogUtil.e(TAG, "Conscrypt ECH initialization failed", throwable)
            false
        }
    }

    @SuppressLint("CustomX509TrustManager")
    class PolicyTrustManager(private val delegate: X509TrustManager) : X509TrustManager {

        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            delegate.checkClientTrusted(chain, authType)
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            delegate.checkServerTrusted(chain, authType)
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = delegate.acceptedIssuers

        @Suppress("unused")
        fun getNetworkSecurityPolicy(): NetworkSecurityPolicy = POLICY
    }

    private val POLICY = object : NetworkSecurityPolicy {
        override fun isCertificateTransparencyVerificationRequired(hostname: String?): Boolean =
            false

        override fun getCertificateTransparencyVerificationReason(hostname: String?):
                CertificateTransparencyVerificationReason =
            CertificateTransparencyVerificationReason.UNKNOWN

        override fun getDomainEncryptionMode(hostname: String?): DomainEncryptionMode =
            if (hostname != null && EchHosts.shouldTryEch(hostname)) {
                DomainEncryptionMode.ENABLED
            } else {
                DomainEncryptionMode.DISABLED
            }
    }

    private val echUnavailable: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun markEchUnavailable(host: String) {
        if (echUnavailable.add(host.lowercase())) {
            LogUtil.i(TAG, "ECH rejected; using plaintext for non-core host=$host")
        }
    }

    fun echUnavailableHosts(): List<String> = echUnavailable.sorted()

    private class EchSocketFactory(private val delegate: SSLSocketFactory) : SSLSocketFactory() {

        override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

        override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

        private fun prepare(socket: Socket, host: String?): Socket {
            if (host == null || socket !is SSLSocket || !EchHosts.shouldTryEch(host)) {
                return socket
            }

            val core = EchHosts.isCoreDomain(host)
            if (echUnavailable.contains(host.lowercase())) {
                if (core) {
                    throw IOException("ECH is unavailable for protected host $host")
                }
                return socket
            }

            val config = EchDoh.echConfigList(host)
            if (config == null) {
                if (core) {
                    throw IOException("Unable to obtain an ECHConfigList for protected host $host")
                }
                markEchUnavailable(host)
                return socket
            }

            return try {
                Conscrypt.setEchConfigList(socket, config)
                LogUtil.d(TAG, "Injected ECH host=$host bytes=${config.size} core=$core")
                socket
            } catch (throwable: Throwable) {
                if (core) {
                    throw IOException("Failed to inject ECHConfigList for $host", throwable)
                }
                LogUtil.w(TAG, "Failed to inject ECH for $host: ${throwable.message}")
                markEchUnavailable(host)
                socket
            }
        }

        override fun createSocket(
            socket: Socket,
            host: String,
            port: Int,
            autoClose: Boolean
        ): Socket =
            prepare(delegate.createSocket(socket, host, port, autoClose), host)

        override fun createSocket(host: String, port: Int): Socket =
            prepare(delegate.createSocket(host, port), host)

        override fun createSocket(
            host: String,
            port: Int,
            localHost: InetAddress,
            localPort: Int,
        ): Socket = prepare(delegate.createSocket(host, port, localHost, localPort), host)

        override fun createSocket(host: InetAddress, port: Int): Socket =
            delegate.createSocket(host, port)

        override fun createSocket(
            address: InetAddress,
            port: Int,
            localAddress: InetAddress,
            localPort: Int,
        ): Socket = delegate.createSocket(address, port, localAddress, localPort)
    }
}

class EchRetryInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        return try {
            chain.proceed(request)
        } catch (throwable: Throwable) {
            if (!isEchRejected(throwable)) throw throwable

            if (EchHosts.isCoreDomain(host)) {
                LogUtil.i("HY-ECH", "ECH rejected for core host=$host; refreshing config")
                EchDoh.invalidateEch(host)
            } else {
                LogUtil.i("HY-ECH", "ECH rejected for host=$host; retrying as plaintext")
                ConscryptEch.markEchUnavailable(host)
            }
            chain.proceed(request)
        }
    }

    private fun isEchRejected(throwable: Throwable): Boolean =
        generateSequence(throwable) { it.cause }.any { cause ->
            cause.javaClass.simpleName.contains("EchRejected", ignoreCase = true) ||
                    cause.message?.contains("ECH_REJECTED", ignoreCase = true) == true
        }
}
