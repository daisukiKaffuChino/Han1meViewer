package io.github.daisukikaffuchino.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.core.net.toUri
import io.github.daisukikaffuchino.han1meviewer.FILE_PROVIDER_AUTHORITY
import io.github.daisukikaffuchino.han1meviewer.HJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.OutputStream

val File?.folderSize: Long
    get() {
        var size = 0L
        val files = this?.listFiles()
        files?.forEach { file -> size += if (file.isDirectory) file.folderSize else file.length() }
        return size
    }

fun File.createFileIfNotExists(): Boolean {
    return if (!exists()) {
        parentFile?.mkdirs()
        createNewFile()
    } else {
        isFile
    }
}

fun Drawable.saveTo(
    outputStream: OutputStream,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100,
): Boolean {
    return toBitmapOrNull()?.run {
        try {
            outputStream.buffered().use { stream ->
                compress(format, quality, stream)
            }
        } catch (e: Exception) {
            LogUtil.e("FileUtil", "保存图片失败", e)
            false
        }
    } == true
}

/**
 * Validates a downloaded video and returns a shareable URI.
 */
fun Context.getDownloadedHanimeVideoUri(
    uri: String,
    onFileNotFound: (() -> Unit)? = null,
): Uri? {
    val videoUri = uri.toUri()
    if (videoUri.scheme == ContentResolver.SCHEME_CONTENT) {
        try {
            contentResolver.openFileDescriptor(videoUri, "r")?.use { pfd ->
                if (pfd.statSize <= 0) {
                    onFileNotFound?.invoke()
                    return null
                }
            }
        } catch (_: Exception) {
            onFileNotFound?.invoke()
            return null
        }
        return videoUri
    }

    val videoFile = File(videoUri.path ?: "")
    if (!videoFile.exists()) {
        onFileNotFound?.invoke()
        return null
    }
    return FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, videoFile)
}

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> loadAssetAs(filePath: String): T? = runCatching {
    applicationContext.assets.open(filePath).use { inputStream ->
        HJson.decodeFromStream<T>(inputStream)
    }
}.getOrNull()
