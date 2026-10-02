package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.File

/**
 * Serves cacheable static GET requests over QUIC+ECH, falling back to the
 * regular TCP+ECH chain on any failure.
 */
class H3Interceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        if (request.method != "GET" || !HyEchH3.isStaticAsset(url)) {
            return chain.proceed(request)
        }
        if (!HyEchH3.shouldTryH3(url.host)) {
            return chain.proceed(request)
        }

        val file = runCatching { HyEchH3.fetchResourceToFile(url.toString()) }.getOrNull()
        if (file == null || !file.exists() || file.length() == 0L) {
            return chain.proceed(request)
        }
        EchLog.i("HY-ECH-H3", "H3 hit host=${url.host} bytes=${file.length()}")

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_3)
            .code(200)
            .message("OK (H3+ECH)")
            .body(FileBody(file, HyEchH3.mimeFor(url).toMediaTypeOrNull()))
            .build()
    }

    private class FileBody(
        private val file: File,
        private val mediaType: MediaType?,
    ) : ResponseBody() {

        override fun contentType(): MediaType? = mediaType

        override fun contentLength(): Long = file.length()

        override fun source(): BufferedSource = file.source().buffer()

        override fun close() {
            super.close()
            file.delete()
        }
    }
}
