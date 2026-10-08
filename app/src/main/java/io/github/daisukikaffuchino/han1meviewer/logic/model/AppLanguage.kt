package io.github.daisukikaffuchino.han1meviewer.logic.model

/** [Local] 本地配置模型：语言偏好。 */
enum class AppLanguage(val code: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    CHINESE_SIMPLIFIED("zh-CN"),
    CHINESE_TRADITIONAL("zh-TW");

    val preferenceValue: String
        get() = code ?: SYSTEM_VALUE

    companion object {
        const val SYSTEM_VALUE = "system"

        fun fromPreference(value: String?): AppLanguage = when (value) {
            "en" -> ENGLISH
            "zh-CN", "zh-rCN", "zhs" -> CHINESE_SIMPLIFIED
            "zh-TW", "zh", "zht" -> CHINESE_TRADITIONAL
            else -> SYSTEM
        }
    }
}
