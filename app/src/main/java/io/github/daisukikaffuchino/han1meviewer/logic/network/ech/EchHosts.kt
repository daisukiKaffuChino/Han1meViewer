package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_HOSTNAME

/**
 * ECH only encrypts a connection when the peer supports it. Attempt it for
 * every host; fail-closed behavior for site domains is decided by
 * [EchTransportPolicy] so proxy routes can still fall back safely.
 */
object EchHosts {

    fun shouldTryEch(@Suppress("UNUSED_PARAMETER") host: String): Boolean = true

    fun isCoreDomain(host: String): Boolean {
        val normalized = host.lowercase()
        return HANIME_HOSTNAME.any { domain ->
            normalized == domain || normalized.endsWith(".$domain")
        }
    }

    fun isProtected(host: String): Boolean = shouldTryEch(host)
}
