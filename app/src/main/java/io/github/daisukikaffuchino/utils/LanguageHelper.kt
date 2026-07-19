package io.github.daisukikaffuchino.utils

import android.os.LocaleList
import java.util.Locale

object LanguageHelper {
    val preferredLanguage: Locale
        get() = LocaleList.getDefault()[0]
}
