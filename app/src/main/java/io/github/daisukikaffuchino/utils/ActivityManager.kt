package io.github.daisukikaffuchino.utils

import android.app.Activity
import android.content.Intent
import java.lang.ref.WeakReference
import kotlin.system.exitProcess

object ActivityManager {
    @JvmStatic
    var currentActivity: WeakReference<Activity?> = WeakReference(null)

    @JvmStatic
    fun restart(killProcess: Boolean = true) {
        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) {
            applicationContext.startActivity(intent)
        }
        if (killProcess) exitProcess(0)
    }
}
