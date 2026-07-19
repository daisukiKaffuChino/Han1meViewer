package io.github.daisukikaffuchino.han1meviewer

import io.github.daisukikaffuchino.utils.ActivityManager

object HCrashHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        e.printStackTrace()
        ActivityManager.restart(killProcess = true)
    }
}
