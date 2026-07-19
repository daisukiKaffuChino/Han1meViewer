package io.github.daisukikaffuchino.utils

import android.util.Base64

fun String.decodeFromStringByBase64(flag: Int = Base64.DEFAULT): String {
    return String(Base64.decode(toByteArray(), flag))
}
