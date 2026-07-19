package io.github.daisukikaffuchino.han1meviewer

import android.content.Context
import androidx.startup.Initializer
import io.github.daisukikaffuchino.utils.applicationContext

class HInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        applicationContext = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler(HCrashHandler)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}
