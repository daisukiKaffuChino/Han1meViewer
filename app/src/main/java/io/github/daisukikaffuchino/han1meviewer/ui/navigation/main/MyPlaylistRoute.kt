package io.github.daisukikaffuchino.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.SettingsRepository
import io.github.daisukikaffuchino.han1meviewer.getHanimeShareText
import io.github.daisukikaffuchino.han1meviewer.ui.screen.home.myplaylist.PlaylistScreen
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.LocalPlayListViewModel
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.MyPlayListViewModel
import io.github.daisukikaffuchino.utils.rememberCopyTextToClipboard
import io.github.daisukikaffuchino.utils.SonnerToast

@Composable
fun MyPlaylistRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val isLoggedIn by SettingsRepository.loginStateFlow.collectAsStateWithLifecycle()
    val copyTextToClipboard = rememberCopyTextToClipboard()
    if (isLoggedIn) {
        val viewModel: MyPlayListViewModel = viewModel(key = "online_playlist")
        PlaylistScreen(
            viewModel = viewModel,
            navigateBack = onBack,
            onClickItem = onNavigateToVideo,
            onLongClickItem = { videoCode, title ->
                copyTextToClipboard(getHanimeShareText(title, videoCode))
                SonnerToast.success(R.string.copy_to_clipboard)
            },
        )
    } else {
        val viewModel: LocalPlayListViewModel = viewModel(key = "local_playlist")
        PlaylistScreen(
            viewModel = viewModel,
            navigateBack = onBack,
            onClickItem = onNavigateToVideo,
            onLongClickItem = { videoCode, title ->
                copyTextToClipboard(getHanimeShareText(title, videoCode))
                SonnerToast.success(R.string.copy_to_clipboard)
            },
        )
    }
}
