package io.github.daisukikaffuchino.han1meviewer.logic.network.interceptor

import android.content.Context
import io.github.daisukikaffuchino.han1meviewer.logic.network.CloudflareVerificationCoordinator
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class CloudflareInterceptor(
    private val context: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 403 && response.header("cf-mitigated") == "challenge") {
            response.close()
            val verified = CloudflareVerificationCoordinator.verify(
                context = context,
                url = request.url.toString(),
            )
            if (!verified) {
                throw IOException("Cloudflare verification was cancelled, failed, or timed out")
            }
            return chain.proceed(request)
        }
        return response
    }
}
