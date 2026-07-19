package io.github.daisukikaffuchino.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

lateinit var applicationContext: Context
    internal set

val application: Application
    get() = applicationContext as Application

val Context.activity: Activity?
    get() {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

inline fun <reified T : Activity> Context.findActivity(): T {
    return findActivityOrNull() ?: error("No activity of type ${T::class.java.simpleName} found")
}

inline fun <reified T : Activity> Context.findActivityOrNull(): T? {
    var context = this
    while (context is ContextWrapper) {
        if (context is T) return context
        context = context.baseContext
    }
    return null
}

fun Context.requireComponentActivity(): ComponentActivity {
    return (activity ?: ActivityManager.currentActivity.get()) as? ComponentActivity
        ?: error("No ComponentActivity found")
}
