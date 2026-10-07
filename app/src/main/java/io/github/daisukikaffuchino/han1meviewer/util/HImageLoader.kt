package io.github.daisukikaffuchino.han1meviewer.util

import io.github.daisukikaffuchino.utils.LogUtil
import io.github.daisukikaffuchino.utils.applicationContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.ImageResult

@Suppress("NOTHING_TO_INLINE")
object HImageLoader {

    private const val TAG = "CoilImageLoader"

    suspend fun execute(data: Any): ImageResult {
        LogUtil.d(TAG, "execute: $data")
        return SingletonImageLoader.get(applicationContext).execute(
            ImageRequest.Builder(applicationContext).data(data).build()
        )
    }
}
