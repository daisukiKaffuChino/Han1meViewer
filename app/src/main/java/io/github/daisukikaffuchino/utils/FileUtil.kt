package io.github.daisukikaffuchino.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmapOrNull
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
            e.printStackTrace()
            false
        }
    } == true
}
