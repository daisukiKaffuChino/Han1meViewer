package io.github.daisukikaffuchino.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

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

inline fun <reified T : Activity> Context.findActivityOrNull(): T? {
    var context = this
    while (context is ContextWrapper) {
        if (context is T) return context
        context = context.baseContext
    }
    return null
}

inline fun <reified T : Activity> Activity.startActivity(
    flag: Int? = null,
    extra: Bundle? = null,
) {
    startActivity(Intent(this, T::class.java).apply {
        flag?.let { flags = it }
        extra?.let { putExtras(it) }
    })
}

object LanguageHelper {
    val preferredLanguage: Locale
        get() = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
}
