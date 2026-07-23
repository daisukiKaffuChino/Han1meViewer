package io.github.daisukikaffuchino.han1meviewer.ui.screen.home.homepage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.utils.SonnerToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 将首页分类转换为高级搜索请求参数。
 *
 * 仅写入分类中存在的参数，避免向搜索页传递空值。
 *
 * @receiver 首页分类数据
 * @return 可直接用于高级搜索的参数映射
 */
internal fun HomeCategory.toAdvancedSearchParams(): Map<String, String> = buildMap {
    genre?.let { put("genre", it) }
    sort?.let { put("sort", it) }
    tags?.let { put("tags", it) }
}
/**
 * 下载远程图片并保存到系统相册。
 *
 * 通过 [MediaStore] 写入公共图片目录。
 * 保存成功后会在主线程显示完成提示。
 *
 * @param context 用于加载图片和访问 ContentResolver 的上下文
 * @param imageUrl 需要保存的图片地址
 */
internal suspend fun saveImageToGallery(context: Context, imageUrl: String) {
    val loader = SingletonImageLoader.get(context)
    val request = ImageRequest.Builder(context)
        .data(imageUrl)
        .build()
    val result = (loader.execute(request) as? SuccessResult)?.image
    val bitmap = result?.toBitmap() ?: return
    val filename = "IMG_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    val fos = uri?.let { context.contentResolver.openOutputStream(it) }
    fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    withContext(Dispatchers.Main) {
        SonnerToast.success(R.string.saved)
    }
}
