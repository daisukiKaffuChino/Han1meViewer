package io.github.daisukikaffuchino.utils

import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService

fun copyTextToClipboard(
    text: CharSequence?,
    label: CharSequence? = null,
) {
    val clipboardManager = applicationContext.getSystemService<ClipboardManager>()
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun CharSequence?.copyToClipboard(label: CharSequence? = null) {
    copyTextToClipboard(this, label)
}

val textFromClipboard: CharSequence?
    get() {
        val context = applicationContext
        val clipData = context.getSystemService<ClipboardManager>()?.primaryClip ?: return null
        return if (clipData.itemCount > 0) {
            clipData.getItemAt(0)?.coerceToText(context)
        } else {
            null
        }
    }
