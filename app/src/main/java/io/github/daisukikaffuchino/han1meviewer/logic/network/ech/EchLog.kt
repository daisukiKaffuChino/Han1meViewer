package io.github.daisukikaffuchino.han1meviewer.logic.network.ech

import io.github.daisukikaffuchino.utils.LogUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class EchLogLevel {
    Debug,
    Info,
    Warning,
    Error,
}

data class EchLogEntry(
    val timestampMillis: Long = System.currentTimeMillis(),
    val level: EchLogLevel,
    val tag: String,
    val message: String,
)

object EchLog {

    private const val MAX_ENTRIES = 500

    private val mutableEntries = MutableStateFlow<List<EchLogEntry>>(emptyList())
    val entries = mutableEntries.asStateFlow()

    fun d(tag: String, message: String) {
        append(EchLogLevel.Debug, tag, message)
        LogUtil.d(tag, message)
    }

    fun i(tag: String, message: String) {
        append(EchLogLevel.Info, tag, message)
        LogUtil.i(tag, message)
    }

    fun w(tag: String, message: String) {
        append(EchLogLevel.Warning, tag, message)
        LogUtil.w(tag, message)
    }

    fun e(tag: String, message: String) {
        append(EchLogLevel.Error, tag, message)
        LogUtil.e(tag, message, null)
    }

    fun clear() {
        mutableEntries.value = emptyList()
    }

    private fun append(level: EchLogLevel, tag: String, message: String) {
        val entry = EchLogEntry(
            level = level,
            tag = tag,
            message = message.replace('\n', ' ').take(2000),
        )
        mutableEntries.update { current ->
            (current + entry).takeLast(MAX_ENTRIES)
        }
    }
}
