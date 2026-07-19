package io.github.daisukikaffuchino.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmapOrNull
import java.io.OutputStream

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
