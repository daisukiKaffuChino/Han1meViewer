package io.github.daisukikaffuchino.utils

import androidx.core.app.ShareCompat

fun shareText(content: CharSequence, title: CharSequence? = null) {
    ShareCompat
        .IntentBuilder(ActivityManager.currentActivity.get() ?: applicationContext)
        .setType("text/plain")
        .setText(content)
        .setChooserTitle(title)
        .startChooser()
}
