package io.github.daisukikaffuchino.han1meviewer.logic.model

import kotlinx.coroutines.flow.StateFlow

/** [Local] 设置的存储接口。 */
interface SettingsStore {
    val settings: StateFlow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}
