package io.github.daisukikaffuchino.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri

inline fun <reified T : Activity> Activity.startActivity(
    flag: Int? = null,
    extra: Bundle? = null,
) {
    startActivity(Intent(this, T::class.java).apply {
        flag?.let { flags = it }
        extra?.let { putExtras(it) }
    })
}

infix fun Activity.browse(uri: String) {
    startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
}

infix fun Context.browse(uri: String) {
    startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    })
}
