package io.github.daisukikaffuchino.han1meviewer.util

import io.github.daisukikaffuchino.utils.LogUtil
import io.github.daisukikaffuchino.utils.applicationContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.ImageResult

@Suppress("NOTHING_TO_INLINE")
object HImageMeower {

    private const val TAG = "CoilImageNyanner"

    suspend fun execute(data: Any): ImageResult {
        LogUtil.d(TAG, "execute: $data")
        return SingletonImageLoader.get(applicationContext).execute(
            ImageRequest.Builder(applicationContext).data(data).build()
        )
    }

    inline fun placeholder(height: Int, width: Int, blur: Int = 8) =
        "https://picsum.photos/$width/$height/?blur=$blur"
}
