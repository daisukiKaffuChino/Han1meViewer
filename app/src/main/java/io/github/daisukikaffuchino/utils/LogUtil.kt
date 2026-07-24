package io.github.daisukikaffuchino.utils

import android.util.Log
import io.github.daisukikaffuchino.han1meviewer.BuildConfig

object LogUtil {
    const val DEFAULT_TAG = "Han1meViewer"

    @Volatile
    var enabled: Boolean = BuildConfig.DEBUG

    fun v(message: String) = v(DEFAULT_TAG, message)

    fun v(tag: String, message: String) = log { Log.v(tag, message) }

    fun d(message: String) = d(DEFAULT_TAG, message)

    fun d(tag: String, message: String) = log { Log.d(tag, message) }

    fun i(message: String) = i(DEFAULT_TAG, message)

    fun i(tag: String, message: String) = log { Log.i(tag, message) }

    fun w(message: String) = w(DEFAULT_TAG, message)

    fun w(message: String, throwable: Throwable?) = w(DEFAULT_TAG, message, throwable)

    fun w(tag: String, message: String) = log { Log.w(tag, message) }

    fun w(tag: String, message: String, throwable: Throwable?) = log {
        if (throwable == null) Log.w(tag, message) else Log.w(tag, message, throwable)
    }

    fun e(message: String) = e(DEFAULT_TAG, message)

    fun e(message: String, throwable: Throwable?) = e(DEFAULT_TAG, message, throwable)

    fun e(tag: String, message: String) = log { Log.e(tag, message) }

    fun e(tag: String, message: String, throwable: Throwable?) = log {
        if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
    }

    private inline fun log(block: () -> Unit) {
        if (enabled) block()
    }
}
