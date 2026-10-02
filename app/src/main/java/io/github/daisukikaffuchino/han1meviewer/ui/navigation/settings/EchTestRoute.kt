package io.github.daisukikaffuchino.han1meviewer.ui.navigation.settings

import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import io.github.daisukikaffuchino.han1meviewer.HanimeConstants.HANIME_HOSTNAME
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.ui.screen.settings.EchTestScreen

@Composable
fun EchTestRouteScreen() {
    val initialHost = SettingsRepository.baseUrl.toUri().host
        ?: HANIME_HOSTNAME.first()
    EchTestScreen(initialHost = initialHost)
}
