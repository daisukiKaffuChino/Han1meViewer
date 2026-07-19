package io.github.daisukikaffuchino.utils

import java.io.File

val File?.folderSize: Long
    get() {
        var size = 0L
        val files = this?.listFiles()
        files?.forEach { file -> size += if (file.isDirectory) file.folderSize else file.length() }
        return size
    }

fun File.createFileIfNotExists(): Boolean {
    return if (!exists()) {
        parentFile?.mkdirs()
        createNewFile()
    } else {
        isFile
    }
}
