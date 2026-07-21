package io.github.daisukikaffuchino.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.URLDecoder
import java.net.URLEncoder

private fun Context.sp(
    name: String = packageName,
    mode: Int = Context.MODE_PRIVATE,
): SharedPreferences {
    return getSharedPreferences(name, mode)
}

fun <T> putSpValue(
    key: String,
    value: T,
    name: String = applicationContext.packageName,
) {
    applicationContext.sp(name = name).edit {
        when (value) {
            is Long -> putLong(key, value)
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Float -> putFloat(key, value)
            else -> putString(key, serialize(value))
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> getSpValue(
    key: String,
    default: T,
    name: String = applicationContext.packageName,
): T {
    return applicationContext.sp(name = name).run {
        val result = when (default) {
            is Long -> getLong(key, default)
            is String -> getString(key, default)
            is Int -> getInt(key, default)
            is Boolean -> getBoolean(key, default)
            is Float -> getFloat(key, default)
            else -> deSerialization(getString(key, serialize(default)))
        }
        result as T
    }
}

private fun <T> serialize(obj: T): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
    objectOutputStream.writeObject(obj)
    var serialized = byteArrayOutputStream.toString("ISO-8859-1")
    serialized = URLEncoder.encode(serialized, "UTF-8")
    objectOutputStream.close()
    byteArrayOutputStream.close()
    return serialized
}

@Suppress("UNCHECKED_CAST")
private fun <T> deSerialization(value: String?): T {
    val decoded = URLDecoder.decode(value, "UTF-8")
    val byteArrayInputStream = ByteArrayInputStream(decoded.toByteArray(charset("ISO-8859-1")))
    val objectInputStream = ObjectInputStream(byteArrayInputStream)
    val obj = objectInputStream.readObject() as T
    objectInputStream.close()
    byteArrayInputStream.close()
    return obj
}
